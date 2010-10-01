#!/bin/csh
echo "THIS SCRIPT NEEDS TO BE UPDATED!"
exit 1
/usr/java/default/bin/java -Xmx500M -classpath /opt/install/apache-tomcat-5.5.20/webapps/OpenSHA/WEB-INF/lib/ERF.jar -Djava.rmi.server.codebase=file:/opt/install/apache-tomcat-5.5.20/webapps/OpenSHA/WEB-INF/lib/ERF.jar -Djava.security.policy=file:/opt/install/apache-tomcat-5.5.20/webapps/OpenSHA/HazardMapDatasets/SimpleRMI.policy -Djava.rmi.server.hostname=gravity.usc.edu org.opensha.sha.calc.remoteCalc.RegisterRemoteResponseSpectrumFactory &
