$1 == "commit" {commit = $2}
#{printf "%s;%s;%s;%s;%s\n", $1, $2, $3, $4, $5 }
$1 == "-" {printf "%s;%s;0;0\n", commit, $3, $1, $2  }
($1 != "" && $1 != "commit" && $1 != "-")  {printf "%s;%s;%s;%s;%s\n", commit, $3, $5, $1, $2  }
#$1 == "-" {printf "%s;MODIFIED; ;%s\n", commit, $2 } 
#match($1,"R[0-9][0-9][0-9]") {printf "%s;RENAMED;%s;%s\n", commit, $2, $3 }
