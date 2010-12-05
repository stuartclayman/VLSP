#!/bin/sh
OUTPUT=random_testbed_noPA
POLICY=Random
RIN=scripts/routertestbeddummy.xml
ROUT=scripts/routeroptions.xml
PIN=scripts/probdummy_noPA.xml
POUT=scripts/probdists.xml
CONTROL=scripts/testbed_control_noPA.xml
CPVAR=/home/rclegg/code/userspacerouter/libs/monitoring-0.6.7.jar:/home/rclegg/code/userspacerouter/libs/timeindex-20101020.jar:/home/rclegg/code/userspacerouter/libs/aggregator-0.3.jar:/home/rclegg/code/userspacerouter
CLEANSCRIPT=/home/rclegg/code/userspacerouter/scripts/claycleanscript.sh


AWK=gawk

SCHEDULE="12.0 6.0 4.0 3.0"

$CLEANSCRIPT

POLICY=Random
OUTPUT=random_testbed_noPA
rm -f $OUTPUT
for i in $SCHEDULE; do
    for j in `seq 3`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL  > out
    $CLEANSCRIPT
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
done

POLICY=Pressure
OUTPUT=pressure_testbed_noPA
rm -f $OUTPUT
for i in $SCHEDULE; do
    for j in `seq 3`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL  > out
    $CLEANSCRIPT
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
 done
done
POLICY=HotSpot
OUTPUT=hotspot_testbed_noPA
rm -f $OUTPUT
for i in $SCHEDULE; do
    for j in `seq 3`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL  > out
    $CLEANSCRIPT
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
done


