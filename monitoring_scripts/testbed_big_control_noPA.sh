#!/bin/sh

RIN=monitoring_scripts/testbed_router_dummy_noPA.xml
ROUT=monitoring_scripts/testbed_routeroptions.xml
CONTROL_SCRIPT=monitoring_scripts/testbed_big_control_noPA.xml
AWK=gawk

CPVAR=/home/rclegg/code/userspacerouter:/home/rclegg/code/userspacerouter/libs/monitoring-0.6.7.jar:/home/rclegg/code/userspacerouter/libs/timeindex-20101020.jar:/home/rclegg/code/userspacerouter/libs/aggregator-0.3.jar
CLEANSCRIPT=/home/rclegg/code/userspacerouter/scripts/mostcleanscript.sh


SEQ="0.02 0.05 0.1"
ITER=5
OUTPUT=random_testbed_big_noPA
POLICY=Random
rm -f $OUTPUT
for i in  $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i "" >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN |  sed -e 's/xxx/'$i'/g' > $ROUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
    $CLEANSCRIPT
  done
done
POLICY=Pressure
OUTPUT=pressure_testbed_big_noPA
rm -f $OUTPUT
for i in $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN |  sed -e 's/xxx/'$i'/g' > $ROUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
    $CLEANSCRIPT
  done
done
POLICY=HotSpot
OUTPUT=hotspot_testbed_big_noPA
rm -f $OUTPUT
for i in $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN |  sed -e 's/xxx/'$i'/g' > $ROUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
    $CLEANSCRIPT
  done
done

