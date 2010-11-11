#!/bin/sh
OUTPUT=random_testbed_noPA
POLICY=Random
RIN=scripts/routertestbeddummy.xml
ROUT=scripts/routeroptions.xml
PIN=scripts/probdummy_noPA.xml
POUT=scripts/probdists.xml
CONTROL=scripts/testbed_control_noPA.xml
AWK=gawk
rm -f $OUTPUT
for i in  5.0 10.0 20.0 50.0 100.0; do
    for j in `seq 3`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java usr.globalcontroller.GlobalController $CONTROL  > out
    tail -50 summary.out | $AWK '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{printf("%g %g %g %g %g %g %g ",a/n,b/n,c/n,d/n,e/n,f/n);}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{printf("%g %g %g %g %g %g %g\n",a/n,b/n,c/n,d/n,e/n,f/n);}' >> $OUTPUT
  done
done
POLICY=Pressure
OUTPUT=pressure_testbed_noPA
for i in  5.0 10.0 20.0 50.0 100.0; do
    for j in `seq 3`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java usr.globalcontroller.GlobalController $CONTROL  > out
    tail -50 summary.out | $AWK '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{printf("%g %g %g %g %g %g %g ",a/n,b/n,c/n,d/n,e/n,f/n);}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{printf("%g %g %g %g %g %g %g\n",a/n,b/n,c/n,d/n,e/n,f/n);}' >> $OUTPUT
  done
done
POLICY=HotSpot
OUTPUT=hotspot_testbed_noPA
rm -f $OUTPUT
for i in  5.0 10.0 20.0 50.0 100.0; do
    for j in `seq 3`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java usr.globalcontroller.GlobalController $CONTROL  > out
    tail -50 summary.out | $AWK '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{printf("%g %g %g %g %g %g %g ",a/n,b/n,c/n,d/n,e/n,f/n);}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{printf("%g %g %g %g %g %g %g\n",a/n,b/n,c/n,d/n,e/n,f/n);}' >> $OUTPUT
  done
done
