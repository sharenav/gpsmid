/**
 * This file is part of GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  Kai Krueger
 */
package de.ueller.midlet.gps;

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.SearchResult;
import de.ueller.midlet.gps.data.KeySelectMenuItem;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.tile.SearchNames;

import de.enough.polish.util.Locale;

public class KeySelectMenu extends Canvas implements
		GpsMidDisplayable, CommandListener {

	private final static Logger logger = Logger.getInstance(
			KeySelectMenu.class, Logger.DEBUG);

	private final Command OK_CMD = new Command(Locale.get("keyselectmenu.Ok")/*Ok*/, Command.OK, 1);
	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 5);
	private final Command DEL_CMD = new Command(Locale.get("keyselectmenu.delete")/*delete*/, Command.ITEM, 2);
	private final Command CLEAR_CMD = new Command(Locale.get("keyselectmenu.clear")/*clear*/, Command.ITEM, 3);

	private Vector result = new Vector();

	/**
	 * This vector is used to buffer writes, so that we only have to synchronize
	 * threads at the end of painting
	 */
	private Vector result2 = new Vector();

	private boolean resetResult2;

	private int carret = 0;

	private int cursor = 0;

	private int scrollOffset = 0;

	private StringBuffer searchCanon = new StringBuffer();

	private int fontSize;

	/**
	 * Record the time at which a pointer press was recorded to determine a
	 * double click
	 */
	private long pressedPointerTime;
	/**
	 * Stores if there was already a click that might be the first click in a
	 * double click
	 */
	private boolean potentialDoubleClick;
	/**
	 * Indicates that there was a drag event since the last pointerPressed
	 */
	private boolean pointerDragged;
	/**
	 * Stores the position of the Y coordinate at which the pointer started
	 * dragging since the last update
	 */
	private int pointerYDragged;
	/**
	 * Stores the position of the initial pointerPress to identify dragging
	 */
	private int pointerXPressed;
	private int pointerYPressed;

	private GpsMidDisplayable parent;
	protected KeySelectMenuListener callback;

	protected KeySelectMenu(GpsMidDisplayable parent) {
		super();
		this.parent = parent;
		
		setCommandListener(this);
		addCommand(OK_CMD);
		addCommand(BACK_CMD);
	}
	
	public KeySelectMenu(GpsMidDisplayable parent,
			KeySelectMenuListener callback) throws Exception {
		this(parent);
		
		if (callback == null) {
			//#debug info
			logger.info("Callback in KeySelectMenu is null");
			throw new Exception(Locale.get("keyselectmenu.NoCallbackSpecified")/*No Callback was specified*/);
		}
		this.callback = callback;
	}

	public void show() {
		this.setFullScreenMode(Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN));
		potentialDoubleClick = false;
		pointerDragged = false;
		GpsMid.getInstance().show(this);
		repaint();
	}

	public synchronized void addResult(KeySelectMenuItem item) {
		//#debug debug
		logger.debug("Adding item " + item + " to KeySelectMenu");
		result.addElement(item);
		repaint();
	}
	
	public synchronized void addResult(Vector items) {
		//#debug info
		logger.info("Adding " + items.size() + " items to KeySelectMenu");
		for (int i = 0; i < items.size(); i++) {
			if (items.elementAt(i) instanceof KeySelectMenuItem) {
				result.addElement(items.elementAt(i));
			} else {
				logger.error(Locale.get("keyselectmenu.AddingWrongTypeToKeySelectMenu")/*Adding a wrong type to KeySelectMenu*/);
			}
		}
		repaint();
	}
	
	public synchronized void removeAll() {
		resetResult2 = true;
		cursor = 0;
	}
	
	public void commandAction(Command c, Displayable d) {
		if (c == BACK_CMD) {
			destroy();
			callback.keySelectMenuCancel();
			parent.show();
			return;
		}
		if (c == DEL_CMD) {
			if (carret > 0) {
				searchCanon.deleteCharAt(--carret);
				callback.keySelectMenuSearchString(searchCanon.toString());
			}
			return;
		}
		if (c == CLEAR_CMD) {
			synchronized (this) {
				resetResult2 = true;
				result.removeAllElements();
			}
			callback.keySelectMenuResetMenu();
			searchCanon.setLength(0);
			carret = 0;
			repaint();
			return;
		}
		if (c == OK_CMD) {
			try {
			Object o = result2.elementAt(cursor);
			if (o instanceof KeySelectMenuItem) {
				KeySelectMenuItem menuItem = (KeySelectMenuItem) o;
				callback.keySelectMenuItemSelected(menuItem);
			} else {
				logger.error(Locale.get("keyselectmenu.SelectedBogusItem")/*Selected an item that should not have been there*/);
			}
			destroy();
			parent.show();
			} catch (Exception e) {
				//logger.exception("Failed to OK ", e);
			} catch (Error ee) {
				logger.error(Locale.get("keyselectmenu.FailedWithError")/*Failed with error: */ + ee.getMessage());
			}
		}
	}

	protected void paint(Graphics gc) {
		//#debug debug
		logger.debug("Painting KeySelectMenu screen with offset: "
				+ scrollOffset);
		if (fontSize == 0)
			fontSize = gc.getFont().getHeight();
		int yc = scrollOffset;

		gc.setColor(255, 255, 255);
		gc.fillRect(0, 0, getWidth(), getHeight());
		gc.setColor(0, 0, 0);
		if (yc < 0) {
			gc.drawString("^", getWidth(), 0, Graphics.TOP | Graphics.RIGHT);
		}

		if (resetResult2) {
			synchronized (this) {
				result2.removeAllElements();
				resetResult2 = false;
			}
		}
		// insert new results from search thread
		if (result.size() > 0) {
			synchronized (this) {
				for (int i = 0; i < result.size(); i++) {
					result2.addElement(result.elementAt(i));
				}
				result.removeAllElements();
			}
		}
		//#debug debug
		logger.debug("Painting " + result2.size() + " number of elements");
		// keep cursor within bounds
		if (cursor != 0 && cursor >= result2.size()) {
			cursor = result2.size() - 1;
		}

		for (int i = 0; i < result2.size(); i++) {
			if (yc < 0) {
				yc += fontSize;
				continue;
			}
			if (yc > getHeight()) {
				gc.setColor(0, 0, 0);
				gc.drawString("v", getWidth(), getHeight() - 7, Graphics.BOTTOM
						| Graphics.RIGHT);
				return;
			}

			if (i == cursor) {
				gc.setColor(255, 0, 0);
			} else {
				gc.setColor(0, 0, 0);
			}
			Object o = result2.elementAt(i);
			if (o instanceof KeySelectMenuItem) {
				KeySelectMenuItem menuItem = (KeySelectMenuItem) o;
				Image img = menuItem.getImage();

				if (img != null)
					gc.drawImage(img, 8, yc + fontSize / 2 - 1,
							Graphics.VCENTER | Graphics.HCENTER);

				String name = menuItem.getName();
				if (name != null) {
					// avoid index out of bounds
					int imatch = searchCanon.length();
					if (name.length() < imatch) {
						imatch = name.length();
					}

					
					gc.drawString(name, 17, yc, Graphics.TOP | Graphics.LEFT);
					
					if (carret <= imatch) {
						int cx = 17 + gc.getFont().stringWidth(
								name.substring(0, carret));
						gc.setColor(0, 255, 0);
						gc.drawLine(cx - 1, yc + fontSize, cx + 1, yc
								+ fontSize);
					}
				} else {
					gc.drawString("...", 17, yc, Graphics.TOP | Graphics.LEFT);
				}
				yc += fontSize;
			} else {
				logger.error(Locale.get("keyselectmenu.PaintingInvalidItem")/*Painting an invalid item*/);
				continue;
			}

		}
	}

	protected void keyRepeated(int keyCode) {
		// Moving the cursor should work with repeated keys the same
		// as pressing the key multiple times
		int action = this.getGameAction(keyCode);
		// System.out.println("repeat key " + keyCode + " " + action);
		if ((action == UP) || (action == DOWN) || (action == LEFT)
				|| (action == RIGHT) || (keyCode == -8)) {
			keyPressed(keyCode);
			return;
		}
	}

	protected void keyPressed(int keyCode) {
		int action = getGameAction(keyCode);
		/**
		 * Ignore gameActions from unicode character keys (space char and
		 * above). By not treating those keys as game actions they get added to
		 * the search canon by default if not handled otherwise
		 */
		if (keyCode >= 32) {
			action = 0;
		}
		//#debug info
		logger.info("KeySelectMenu got key " + keyCode + " " + action);

		// Unicode character 10 is LF
		// so 10 should correspond to Enter key on QWERT keyboards
		if (keyCode == 10 || action == FIRE) {
			commandAction(OK_CMD, null);
			return;
		} else if (action == UP) {
			if (cursor > 0)
				cursor--;
			if (cursor * fontSize + scrollOffset < 0) {
				scrollOffset += 3 * fontSize;
			}
			if (scrollOffset > 0)
				scrollOffset = 0;
			repaint();
			return;
		} else if (action == DOWN) {
			if (cursor < result2.size() - 1)
				cursor++;
			if (((cursor + 1) * fontSize + scrollOffset) > getHeight()) {
				scrollOffset -= 3 * fontSize;
			}

			if (scrollOffset > 0)
				scrollOffset = 0;

			repaint();
			return;
		} else if (action == LEFT) {
			if (carret > 0)
				carret--;
			repaint();
			return;
		} else if (action == RIGHT) {
			if (carret < searchCanon.length())
				carret++;
			repaint();
			return;
		} else if (keyCode == -8 || keyCode == 8 || keyCode == 127) {
			/**
			 * Non standard Key -8: hopefully is mapped to the delete / clear
			 * key. According to
			 * www.j2meforums.com/wiki/index.php/Canvas_Keycodes most major
			 * mobiles that have this key map to -8
			 * 
			 * Unicode Character Key: 8 is backspace so this should be standard
			 * Keycode 127 is Clear-Key passed by MicroEmulator
			 **/

			if (carret > 0) {
				searchCanon.deleteCharAt(--carret);
			}
		} else {
			// filter out special keys such as shift key (-50), volume keys,
			// camera keys...
			if (keyCode > 0) {
				searchCanon.insert(carret++, (char) keyCode);
			}
		}
		setTitle(searchCanon.toString());
		callback.keySelectMenuSearchString(searchCanon.toString());
	}

	public void pointerPressed(int x, int y) {
		// #debug debug
		logger.debug("PointerPressed: " + x + "," + y);
		long currTime = System.currentTimeMillis();
		if (potentialDoubleClick) {
			if ((currTime - pressedPointerTime > 400)) {
				potentialDoubleClick = false;
				pressedPointerTime = currTime;
			}
		} else {
			pressedPointerTime = currTime;
		}
		pointerYDragged = y;
		pointerXPressed = x;
		pointerYPressed = y;
	}

	public void pointerReleased(int x, int y) {
		// #debug debug
		logger.debug("PointerReleased: " + x + "," + y);
		long currTime = System.currentTimeMillis();
		int clickIdx = (y - scrollOffset) / fontSize;
		if (pointerDragged) {
			pointerDragged = false;
			potentialDoubleClick = false;
			return;
		}
		if (potentialDoubleClick) {
			if ((currTime - pressedPointerTime < 400) && (clickIdx == cursor)) {
				// #debug debug
				logger.debug("PointerDoublePressed");
				commandAction(OK_CMD, null);
				potentialDoubleClick = false;
				return;
			}
		}
		potentialDoubleClick = true;
		cursor = clickIdx;
		repaint();
	}

	public void pointerDragged(int x, int y) {
		// #debug debug
		logger.debug("Pointer dragged: " + x + " " + y);
		if ((Math.abs(x - pointerXPressed) < 3)
				&& (Math.abs(y - pointerYPressed) < 3)) {
			/**
			 * On some devices, such as PhoneME, every pointerPressed event also
			 * causes a pointerDragged event. We therefore need to filter out
			 * those pointerDragged events that haven't actually moved the
			 * pointer. Chose threshold of 2 pixels
			 */
			// #debug debug
			logger.debug("No real dragging, as pointer hasn't moved");
			return;
		}
		pointerDragged = true;

		scrollOffset += (y - pointerYDragged);

		if (scrollOffset > 0) {
			scrollOffset = 0;
		}
		if (scrollOffset < -1 * (result.size() - 2) * fontSize) {
			scrollOffset = -1 * (result.size() - 2) * fontSize;
		}
		pointerYDragged = y;
		repaint();
	}

	public void destroy() {
	}

}
