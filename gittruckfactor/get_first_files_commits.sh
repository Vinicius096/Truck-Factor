#!/bin/bash

repository=$1
path=$2
current=${PWD}


cd $repository
if [ -f "files_comit_sha.log" ];
then
    rm files_comit_sha.log
fi    
if [ -f "linguistfiles.log" ];
then
  echo "linguistfiles.log ok"
  
  while IFS=";" read language file
  	do
        sha=$(git log --follow --pretty=format:"%H" "$file"  | tail -1)
    echo "$file;$sha" >> files_comit_sha.log  		
  done < linguistfiles.log
  
else
  echo "linguistfiles.log not exist. Processing by using filelist.log"
  
  while IFS=";" read file
  	do
        sha=$(git log --follow --pretty=format:"%H" "$file"  | tail -1)
    echo "$file;$sha"   	
  done < filelist.log >> files_comit_sha.log
fi