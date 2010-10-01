#!/bin/bash

dir=`dirname $0`

jar="${dir}/dist/OpenSHA_complete.jar"

java -classpath $jar org.opensha.refFaultParamDb.dao.db.PrefFaultSectionDataDB_DAO opensha_cron $1
