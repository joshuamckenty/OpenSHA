#!/bin/bash

procs=`ps gx | grep java | grep cruisecontrol-launcher.jar | awk '{ print $1 }'`
dashNine=""
count=0
while [[ $procs ]]
do
	echo -n "killing: ";
	echo $procs | xargs echo
	if [[ $count -gt 9 ]];then
		dashNine="-9"
	fi
	echo $procs | xargs kill $dashNine
	let count=count+1
	sleep 1
	procs=`ps gx | grep java | grep cruisecontrol-launcher.jar | awk '{ print $1 }'`
done
