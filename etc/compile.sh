#!/bin/bash

set -o errexit

ant=${1-"ant"}
resource=${2-0}

$ant -f compile.xml -lib ../lib:../dev/scratch/ISTI/isti.util.jar
if [[ $resource -eq 1 ]];then
	$ant -f compile.xml resource.all
fi
exit $?