#!/bin/bash

dir=`dirname $0`
${dir}/kill_cruise.sh
${dir}/test_start_cc.sh
