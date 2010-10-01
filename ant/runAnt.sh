#!/bin/bash

set -o errexit

buildFile=${1-"build.xml"}
target=${2}

ant -f $buildFile -lib ../lib:../dev/scratch/ISTI/isti.util.jar $target
exit $?
