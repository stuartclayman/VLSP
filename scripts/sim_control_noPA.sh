#!/bin/sh
OUTPUT=random_sim_noPA
POLICY=Random
RIN=scripts/routerdummy.xml
ROUT=scripts/routeroptions.xml
PIN=scripts/probdummy_noPA.xml
POUT=scripts/probdists.xml
rm -f $OUTPUT
for i in  0.1 0.2 0.5 1.0 2.0 5.0 10.0 20.0 50.0 100.0; do
    for j in `seq 5`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java usr.globalcontroller.GlobalController scripts/simcontrol.xml  > out
    tail -50 summary.out | nawk '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{print a/n,b/n,c/n,d/n,e/n,f/n;}' >> $OUTPUT
  done
done
POLICY=Pressure
OUTPUT=pressure_sim_noPA
rm -f $OUTPUT
for i in  0.1 0.2 0.5 1.0 2.0 5.0 10.0 20.0 50.0 100.0; do
    for j in `seq 5`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java usr.globalcontroller.GlobalController scripts/simcontrol.xml  > out
    tail -50 summary.out | nawk '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{print a/n,b/n,c/n,d/n,e/n,f/n;}' >> $OUTPUT
  done
done
POLICY=HotSpot
OUTPUT=hotspot_sim_noPA
rm -f $OUTPUT
for i in 0.1 0.2 0.5 1.0 2.0 5.0 10.0 20.0 50.0 100.0; do
    for j in `seq 5`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java usr.globalcontroller.GlobalController scripts/simcontrol.xml  > out
    tail -50 summary.out | nawk '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{print a/n,b/n,c/n,d/n,e/n,f/n;}' >> $OUTPUT
  done
done

