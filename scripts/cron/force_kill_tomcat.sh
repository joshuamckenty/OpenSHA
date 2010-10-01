#!/bin/bash

procs=`ps gx | grep tomcat | grep "org\.apache\.catalina\.startup\.Bootstrap" | awk '{ print $1 }'`
echo "leftover procs: $procs"
if [[ $procs ]];then
        echo "calling manual kill"
        kill $procs
        sleep 2
        procs=`ps gx | grep tomcat | grep "org\.apache\.catalina\.startup\.Bootstrap" | awk '{ print $1 }'`
        if [[ $procs ]];then
                echo "calling kill -9"
                kill -9 $procs
        fi
fi
