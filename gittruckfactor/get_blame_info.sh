#!/bin/bash

repository=$1
path=$2
current=${PWD}


cd $repository
if [ -f "linguistfiles.log" ];
then
  echo "linguistfiles.log ok"
  
  while IFS=";" read language file
  	do
  		git blame --line-porcelain $file | sed -n 's/^author /;/p' | sort | uniq -c | sort -rn > .tempfile
		
  		while IFS=";" read value dev
  						do
  							echo "$file;$dev;$value" >> blame.log
  						done < .tempfile
  	done < linguistfiles.log
  
else
  echo "linguistfiles.log not exist. Processing by using filelist.log"
  
  while IFS=";" read file
  	do
  		git blame --line-porcelain $file | sed -n 's/^author /;/p' | sort | uniq -c | sort -rn > .tempfile
		
  		while IFS=";" read value dev
  						do
  							echo "$file;$dev;$value" >> blame.log
  						done < .tempfile
  	done < filelist.log
fi




cd $current


