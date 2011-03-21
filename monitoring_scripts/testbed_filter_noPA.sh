#!/bin/sh

RIN=monitoring_scripts/testbed_filter_dummy_noPA.xml
ROUT=monitoring_scripts/testbed_routeroptions.xml
CONTROL_SCRIPT=monitoring_scripts/testbed_filter_noPA.xml
AWK=gawk

CPVAR=/home/rclegg/code/userspacerouter:/home/rclegg/code/userspacerouter/libs/monitoring-0.6.7.jar:/home/rclegg/code/userspacerouter/libs/timeindex-20101020.jar:/home/rclegg/code/userspacerouter/libs/aggregator-0.3.jar
CLEANSCRIPT=/home/rclegg/code/userspacerouter/scripts/claycleanscript.sh


SEQ="0.02 0.05 0.1"
ITER=5
OUTPUT=testbed_nofilter_noPA
FILTER=always
rm -f $OUTPUT
for i in  $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i "" >> $OUTPUT
    sed -e 's/yyy/'$FILTER'/g' $RIN |  sed -e 's/xxx/'$i'/g' > $ROUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
    $CLEANSCRIPT
  done
done
FILTER=2%
OUTPUT=testbed_2perfilter_noPA
rm -f $OUTPUT
for i in $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$FILTER'/g' $RIN |  sed -e 's/xxx/'$i'/g' > $ROUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
    $CLEANSCRIPT
  done
done
FILTER=5%
OUTPUT=testbed_5perfilter_noPA
rm -f $OUTPUT
for i in $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$FILTER'/g' $RIN |  sed -e 's/xxx/'$i'/g' > $ROUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
    $CLEANSCRIPT
  done
done
FILTER=10%
OUTPUT=testbed_10perfilter_noPA
rm -f $OUTPUT
for i in $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$FILTER'/g' $RIN |  sed -e 's/xxx/'$i'/g' > $ROUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
    $CLEANSCRIPT
  done
done


