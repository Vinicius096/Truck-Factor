#!/bin/bash

path=$1
repository=$2
currentpath=${PWD}
log_path=$currentpath"/log/"$repository"/"

now=$(date)
echo -e $now: BEGIN copy logs: $repository \\n 
cd $path



if [ ! -d $currentpath"/log/" ]; then
	mkdir $currentpath"/log/"
fi
if [ ! -d $log_path ]; then
	mkdir $log_path
  cd $path
  $(mv *.log $log_path)
else 
  echo "Log already saved! Not empty folder: $log_path"
fi



now=$(date)
echo -e $now: END logs saved in: $log_path \\n 