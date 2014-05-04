#/bin/bash

for filename in *.java
do
  cat ../license.txt $filename > new/$filename
done