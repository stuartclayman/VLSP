#!/bin/sh

RIN=monitoring_scripts/sim_scale_dummy_noPA.xml
ROUT=monitoring_scripts/sim_router_scale_options.xml
PIN=monitoring_scripts/sim_scale_probdummy.xml
POUT=monitoring_scripts/sim_scale_prob.xml
CONTROL_SCRIPT=monitoring_scripts/sim_scale_noPA.xml
AWK=gawk

CPVAR=/home/rclegg/code/userspacerouter:/home/rclegg/code/userspacerouter/libs/monitoring-0.6.7.jar:/home/rclegg/code/userspacerouter/libs/timeindex-20101020.jar:/home/rclegg/code/userspacerouter/libs/aggregator-0.3.jar


SEQ="0.5 1.0 2.0 5.0 10.0 20.0 50.0 100.0"
ITER=5
OUTPUT=sim_pressure_scale_noPA
POLICY=Pressure
rm -f $OUTPUT
for i in  $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i "" >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
done
OUTPUT=sim_random_scale_noPA
POLICY=Random
rm -f $OUTPUT
for i in  $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i "" >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
done
OUTPUT=sim_hotspot_scale_noPA
POLICY=HotSpot
rm -f $OUTPUT
for i in  $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i "" >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
done
