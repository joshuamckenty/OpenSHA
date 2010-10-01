#!/bin/bash

branch=$1

if [[ ! $branch ]];then
	echo "NO BRANCH SPECIFIED!"
	exit 2
fi

url="https://source.usc.edu/svn/opensha/branches/$branch"

echo "switching to branch $branch at $url"

svn switch $url
