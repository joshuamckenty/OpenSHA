#!/bin/bash

if [ -f ~/.bashrc ]; then
	. ~/.bashrc
fi

pids=`ps gx | grep java | grep cruisecontrol-launcher.jar | awk '{ print $1 }'`
if [[ $pids ]];then
	echo "cruise control is already running! proc(s): $pids"
	exit
fi
cd /usr/local/cruise/main/
/usr/local/cruise/main/bin/cruisecontrol.sh -configfile /usr/local/cruise/main/config.xml
