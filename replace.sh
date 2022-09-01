#!/bin/sh


FILE=$1
cat $FILE | sed 's/2.4.0.Final/2.4.0.COMTOR/g' > /tmp/tempo.txt

cp /tmp/tempo.txt $FILE

