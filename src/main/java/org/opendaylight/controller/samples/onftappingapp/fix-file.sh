#/bin/bash

expand -t 4 < $1 > .foo
cat .foo | sed -e "s/ \{1,\}$//" > $1

