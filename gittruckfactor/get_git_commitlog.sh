#!/bin/bash

path=$1
currentpath=${PWD}
now=$(date)
echo -e $now: BEGIN git log extraction: $path \\n 

cd $path

git config diff.renameLimit 999999 

#Extract commit information
git log --pretty=format:"%H-;-%an-;-%ae-;-%at-;-%cn-;-%ce-;-%ct-;-%f"  > commitinfo.log

#Extract and format commit files information
git log --name-status --pretty=format:"commit	%H" --find-renames > temp.log
awk -F$'\t' -f $currentpath/log.awk temp.log > commitfileinfo.log


cd $currentpath


rm temp.log

now=$(date)
echo -e "Log files (commitinfo.log, commitfileinfo.log) were generated in $path folder:  \\n"
echo -e $now: END git log extraction: $path \\n 
