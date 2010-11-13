#!/bin/sh
OUTPUT=random_testbed_noPA
POLICY=Random
RIN=scripts/routertestbeddummy.xml
ROUT=scripts/routeroptions.xml
PIN=scripts/probdummy_noPA.xml
POUT=scripts/probdists.xml
CONTROL=scripts/testbed_control_noPA.xml
CPVAR=/home/rclegg/code/userspacerouter/libs/monitoring-0.6.7.jar:/home/rclegg/code/userspacerouter/libs/timeindex-20101020.jar:/home/rclegg/code/userspacerouter/libs/aggregator-0.3.jar:/home/rclegg/code/userspacerouter


AWK=gawk
rm -f $OUTPUT
for i in  50.0 25.0 10.0 5.0 2.5; do
    for j in `seq 3`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL  > out
    tail -50 summary.out | $AWK '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{printf("%g %g %g %g %g %g ",a/n,b/n,c/n,d/n,e/n,f/n);}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{printf("%g %g %g %g %g %g\n",a/n,b/n,c/n,d/n,e/n,f/n);}' >> $OUTPUT
  done
done
POLICY=Pressure
OUTPUT=pressure_testbed_noPA
for i in  50.0 25.0 10.0 5.0 2.5 ; do
    for j in `seq 3`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL  > out
    tail -50 summary.out | $AWK '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{printf("%g %g %g %g %g %g ",a/n,b/n,c/n,d/n,e/n,f/n);}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{printf("%g %g %g %g %g %g\n",a/n,b/n,c/n,d/n,e/n,f/n);}' >> $OUTPUT
  done
done
POLICY=HotSpot
OUTPUT=hotspot_testbed_noPA
rm -f $OUTPUT
for i in 50.0 25.0 10.0 5.0 2.5 ; do
    for j in `seq 3`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL  > out
    tail -50 summary.out | $AWK '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{printf("%g %g %g %g %g %g ",a/n,b/n,c/n,d/n,e/n,f/n);}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{a+=$1; b+=$2; c+=$3;d+=$4;e+=$5;f+=$6;n++;}END{printf("%g %g %g %g %g %g\n",a/n,b/n,c/n,d/n,e/n,f/n);}' >> $OUTPUT
  done
done
