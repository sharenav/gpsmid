#!/bin/sh
#
# update the Osm2GpsMid snapshots
#

numver=0.7.5
ver=$numver-map66

ant clean
#
# workaround for the first i18n-messages failure
#
ant -Ddevice=Generic/blackberry

# normal build 

ant
cd Osm2GpsMid
ant clean
ant
cd ..
cp -p Osm2GpsMid/dist/Osm2GpsMid-$ver.jar .

# debug version build
ant clean
ant debug -Ddevice=Generic/blackberry
ant debug j2mepolish

cd Osm2GpsMid
ant clean
ant
cd ..
cp -p Osm2GpsMid/dist/Osm2GpsMid-$ver.jar Osm2GpsMid-$ver-debug.jar

# 


mkdir "Release $numver"
cp dist/*-$ver.jar dist/*-$ver.jad README.mkd WHATSNEW.txt Osm2GpsMid-$ver.jar Osm2GpsMid-$ver-debug.jar "Release $numver"


chmod -R g+w "Release $numver"
