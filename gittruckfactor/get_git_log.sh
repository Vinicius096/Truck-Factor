#!/bin/bash

path=$1
currentpath=${PWD}
now=$(date)
echo -e $now: BEGIN git log extraction: $path \\n 

cd $path

git config diff.renameLimit 999999 

#Extract commit information
git log --pretty=format:"%H;%an;%ae;%at;%cn;%ce;%ct;%f"  > commitinfo.log

#Extract and format commit files information
git log --name-status --pretty=format:"commit	%H" --find-renames > temp.log
awk -F$'\t' -f $currentpath/log.awk temp.log > commitfileinfo.log

#Extract and format commit files information
git log  --pretty=format:"commit %H" --find-renames --numstat | awk -f $currentpath/log_size.awk > commitfileinfo_size.log

#Get current file list
git ls-files > filelist.log

cd $currentpath

#Filter files by using linguist script
./linguist_script.sh $path

#Get blame info
#./get_blame_info.sh $path

#Get blame info
#./cloc_script.sh $path 


#git config --unset diff.renameLimit

rm temp.log

now=$(date)
echo -e "Log files (commitinfo.log, commitfileinfo.log, filelist.log, linguistfiles.log) were generated in $path folder:  \\n"
echo -e $now: END git log extraction: $path \\n 
