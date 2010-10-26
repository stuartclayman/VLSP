#!/bin/sh
OUTPUT=hotspot_noPA
POLICY=HotSpot
RIN=scripts/routerdummy.xml
ROUT=scripts/routeroptions.xml
PIN=scripts/probdummy.xml
POUT=scripts/probdists.xml
rm -f $OUTPUT
for i in 2.0 5.0 10.0 20.0 50.0 100.0; do
    for j in `seq 5`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java usr.globalcontroller.GlobalController scripts/simcontrol.xml  > out
    tail -50 summary.out | nawk '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{print a/n,b/n,c/n,d/n,e/n,f/n;}' >> $OUTPUT
  done
done

