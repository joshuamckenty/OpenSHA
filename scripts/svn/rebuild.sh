#!/bin/bash

#cd /usr/local/tomcat/default/webapps/OpenSHA_dev/WEB-INF

echo "updating from svn"

svn up

cd ant

echo "compiling!"

ant="/usr/local/ant/default/bin/ant"

#../etc/compile.sh $ant 1
./runAnt.sh build.xml
./runAnt.sh build.xml resource.all
./runAnt.sh CompleteJar.xml
