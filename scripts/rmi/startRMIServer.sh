#!/bin/bash

mainDir=`pwd`
if [[ `basename $mainDir` != "WEB-INF" ]];then
	echo "must be run from WEB-INF directory!"
	exit 2
fi
rmiPolicyFile=$mainDir/conf/SimpleRMI.policy
java="/usr/java/default/bin/java"
#java="/usr/java/jdk1.6.0_10/jre/bin/java"

$java -Xmx500M -classpath ${mainDir}/dist/OpenSHA_complete.jar -Djava.security.policy=file:$rmiPolicyFile -Djava.rmi.server.hostname=opensha.usc.edu org.opensha.sha.earthquake.rupForecastImpl.remote.RegisterRemoteERF_Factory &
