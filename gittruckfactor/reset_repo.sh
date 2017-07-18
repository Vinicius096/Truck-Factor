#!/bin/bash

path=$1
branch=$2
currentpath=${PWD}
now=$(date)

exe() { echo "\$ $@" ; "$@" ; }

cd $path
rm commitinfo.log
rm commitfileinfo.log
rm filelist.log
rm commitfileinfo_size.log
rm linguistfiles.log
rm temp.log


exe git reset --hard $branch
