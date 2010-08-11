package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See COPYING
 */

import javax.microedition.lcdui.*;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Legend;


public class GuiRoute extends Form implements CommandListener {
	private final static Logger logger = Logger.getInstance(GuiRoute.class,Logger.DEBUG);

	// commands
	private static final Command CMD_OK = new Command("Ok"/* i:Ok */, Command.OK, 2);
	private static final Command CMD_CANCEL = new Command("Cancel"/* i:Cancel */, Command.BACK, 3);
	
	private ChoiceGroup routingTravelModesGroup;
	private Gauge gaugeRoutingEsatimationFac; 
	private ChoiceGroup routingTurnRestrictionsGroup;
	private ChoiceGroup continueMapWhileRouteing;
	private ChoiceGroup routingOptsGroup;
	private ChoiceGroup routingStrategyOptsGroup;
	private ChoiceGroup routingShowOptsGroup;
	private TextField  tfMainStreetNetDistanceKm;
	private TextField  tfMinRouteLineWidth;
	private TextField tfTrafficSignalCalcDelay;

	// other
	private GpsMidDisplayable parent;
	private boolean useAsSetupDialog;
	
	public GuiRoute(GpsMidDisplayable parent, boolean useAsSetupDialog) {
		super("Route to destination"/* i:RouteToDestination */);
		// Set up this Displayable to listen to command events
		this.parent = parent;

		setCommandListener(this);
		addCommand(CMD_OK);
		addCommand(CMD_CANCEL);

		this.useAsSetupDialog = useAsSetupDialog;
		if (useAsSetupDialog) {
			setTitle("Routing Options"/* i:RoutingOptions */);
		}

		String travelModes[] = new String[Legend.getTravelModes().length];
		for (int i=0; i<travelModes.length; i++) {
			travelModes[i]=Legend.getTravelModes()[i].travelModeName;
		}
		routingTravelModesGroup = new ChoiceGroup("Travel by"/* i:TravelBy */, Choice.EXCLUSIVE, travelModes, null);
		routingTravelModesGroup.setSelectedIndex(Configuration.getTravelModeNr(), true);
		append(routingTravelModesGroup);

		String [] trStates = new String[2];
		trStates[0] = "On"/* i:On */;
		trStates[1] = "Off"/* i:Off */;
		routingTurnRestrictionsGroup = new ChoiceGroup("Turn restrictions"/* i:TurnRestrictions */, Choice.EXCLUSIVE, trStates ,null);
		routingTurnRestrictionsGroup.setSelectedIndex( (Configuration.getCfgBitSavedState(Configuration.CFGBIT_USE_TURN_RESTRICTIONS_FOR_ROUTE_CALCULATION) ? 0 : 1) ,true);
		append(routingTurnRestrictionsGroup);
		

		gaugeRoutingEsatimationFac=new Gauge("Calculation speed"/* i:CalculationSpeed */, true, 10, Configuration.getRouteEstimationFac());
		append(gaugeRoutingEsatimationFac);
		tfMainStreetNetDistanceKm = new TextField("Distance in km to main street net (used for large route distances):"/* i:DistanceToMainStreet */, Integer.toString(Configuration.getMainStreetDistanceKm()), 5, TextField.DECIMAL);
		append(tfMainStreetNetDistanceKm);
		
		String [] routingStrategyOpts = new String[3];
		boolean[] selRoutingStrategy = new boolean[3];
		routingStrategyOpts[0] = "Look for motorways"/* i:LookForMotorways */; selRoutingStrategy[0]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_TRY_FIND_MOTORWAY);
		routingStrategyOpts[1] = "Boost motorways"/* i:BoostMotorways */; selRoutingStrategy[1]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_BOOST_MOTORWAYS);
		routingStrategyOpts[2] = "Boost trunks & primarys"/* i:BoostTrunksPrimarys */; selRoutingStrategy[2]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_BOOST_TRUNKS_PRIMARYS);
		routingStrategyOptsGroup = new ChoiceGroup("Calculation strategies"/* i:CalculationStrategies */, Choice.MULTIPLE, routingStrategyOpts ,null);
		routingStrategyOptsGroup.setSelectedFlags(selRoutingStrategy);
		append(routingStrategyOptsGroup);

		if (useAsSetupDialog) {
			String [] routingShowOpts = new String[3];
			boolean[] selRoutingShow = new boolean[3];
			routingShowOpts[0] = "Estimated duration"/* i:EstimatedDuration */; selRoutingShow[0]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_ROUTE_DURATION_IN_MAP);
			routingShowOpts[1] = "ETA"/* i:ETA */; selRoutingShow[1]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_ETA_IN_MAP);
			routingShowOpts[2] = "Offset to route line"/* i:OffsetToRouteLine */; selRoutingShow[2]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_OFF_ROUTE_DISTANCE_IN_MAP);
			routingShowOptsGroup = new ChoiceGroup("Infos in map screen"/* i:Infos */, Choice.MULTIPLE, routingShowOpts ,null);
			routingShowOptsGroup.setSelectedFlags(selRoutingShow);
			append(routingShowOptsGroup);
	
			String [] routingBack = new String[3];
			routingBack[0] = "No"/* i:No */;
			routingBack[1] = "At route line creation"/* i:AtCreation */;
			routingBack[2] = "Yes"/* i:Yes */;
			continueMapWhileRouteing = new ChoiceGroup("Continue map while calculation:"/* i:ContinueMap */, Choice.EXCLUSIVE, routingBack ,null);
			continueMapWhileRouteing.setSelectedIndex(Configuration.getContinueMapWhileRouteing(),true);
			append(continueMapWhileRouteing);
	
			
			tfMinRouteLineWidth = new TextField("Minimum width of route line"/* i:MinimumWidth */, Integer.toString(Configuration.getMinRouteLineWidth()), 1, TextField.DECIMAL);
			append(tfMinRouteLineWidth);
			
			String [] routingOpts = new String[3];
			boolean[] selRouting = new boolean[3];
			routingOpts[0] = "Auto recalculation"/* i:AutoRecalculation */; selRouting[0]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_AUTO_RECALC);
			routingOpts[1] = "Route browsing with up/down keys"/* i:RouteBrowsing */; selRouting[1]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_BROWSING);
			routingOpts[2] = "Hide quiet arrows"/* i:HideQuietArrows */; selRouting[2]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_HIDE_QUIET_ARROWS);
			routingOptsGroup = new ChoiceGroup("Other"/* i:Other */, Choice.MULTIPLE, routingOpts ,null);
			routingOptsGroup.setSelectedFlags(selRouting);
			append(routingOptsGroup);
			
			tfTrafficSignalCalcDelay = new TextField("Seconds the examined route path gets delayed at traffic signals during calculation"/* i:SecondsDelayed */, Integer.toString(Configuration.getTrafficSignalCalcDelay()), 2, TextField.DECIMAL);
			append(tfTrafficSignalCalcDelay);
		}

	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD_CANCEL) {			
			parent.show();
			return;
		}

		if (c == CMD_OK) {			
			Configuration.setTravelMode(routingTravelModesGroup.getSelectedIndex());
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_USE_TURN_RESTRICTIONS_FOR_ROUTE_CALCULATION, (routingTurnRestrictionsGroup.getSelectedIndex() == 0) );			
			Configuration.setRouteEstimationFac(gaugeRoutingEsatimationFac.getValue());

			String km=tfMainStreetNetDistanceKm.getString(); 
			Configuration.setMainStreetDistanceKm(
					(int) (Float.parseFloat(km)) 
			);

			boolean[] selStrategyRouting = new boolean[3];
			routingStrategyOptsGroup.getSelectedFlags(selStrategyRouting);
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_TRY_FIND_MOTORWAY, selStrategyRouting[0]);
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_BOOST_MOTORWAYS, selStrategyRouting[1]);
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_BOOST_TRUNKS_PRIMARYS, selStrategyRouting[2]);
		
			if (useAsSetupDialog) {
				boolean[] selShowRouting = new boolean[3];
				routingShowOptsGroup.getSelectedFlags(selShowRouting);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_ROUTE_DURATION_IN_MAP, selShowRouting[0]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_ETA_IN_MAP, selShowRouting[1]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_OFF_ROUTE_DISTANCE_IN_MAP, selShowRouting[2]);
	
				Configuration.setContinueMapWhileRouteing(continueMapWhileRouteing.getSelectedIndex());
				
				String w=tfMinRouteLineWidth.getString(); 
				Configuration.setMinRouteLineWidth( 
						(int) (Float.parseFloat(w)) 
				); 
				
				boolean[] selRouting = new boolean[3];
				routingOptsGroup.getSelectedFlags(selRouting);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_AUTO_RECALC, selRouting[0]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_BROWSING, selRouting[1]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_HIDE_QUIET_ARROWS, selRouting[2]);

				String s=tfTrafficSignalCalcDelay.getString(); 
				Configuration.setTrafficSignalCalcDelay( 
						(int) (Integer.parseInt(s)) 
				); 
			
			} else {
				Trace.getInstance().performIconAction(Trace.ROUTING_START_CMD);
			}
			parent.show();
			return;
		}
	}
	
	
	public void show() {
		GpsMid.getInstance().show(this);
	}

}
