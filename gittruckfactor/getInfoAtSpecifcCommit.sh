#!/bin/bash

path=$1
commit=$2
currentpath=${PWD}
now=$(date)

exe() { echo "\$ $@" ; "$@" ; }

cd $path
rm filelist.log
rm linguistfiles.log

exe git reset --hard origin/$commit

#Get current file list
git ls-files > filelist.log

cd $currentpath

#Filter files by using linguist script
./linguist_script.sh $path


now=$(date)
echo -e "Log files (filelist.log, linguistfiles.log) were generated in $path folder:  \\n"
echo -e $now: END git log extraction: $path \\n 
