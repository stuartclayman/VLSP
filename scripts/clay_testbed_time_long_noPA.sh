#!/bin/sh
MASTERSCRIPT=scripts/testbed_time_noPA.xml

RIN=scripts/routertestbeddummy_time.xml
ROUT=scripts/routertestbed_time.xml
CPVAR=/home/rclegg/code/userspacerouter:/home/rclegg/code/userspacerouter/libs/monitoring-0.6.7.jar:/home/rclegg/code/userspacerouter/libs/timeindex-20101020.jar:/home/rclegg/code/userspacerouter/libs/aggregator-0.3.jar
CLEANSCRIPT=/home/rclegg/code/userspacerouter/scripts/claycleanscript.sh


AWK=gawk

SEQ="0.0 0.1 1.0 10.0 100.0"
REPS="10"

OUTPUT=random_testbed_time_long_noPA
POLICY=Random
rm -f $OUTPUT
count=0
for i in  $SEQ; do
    for j in `seq $REPS`; do
    echo -n $count $i "" >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN | sed -e 's/xxx/'$i'/g' > $ROUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $MASTERSCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
    $CLEANSCRIPT
    done
  count=`expr $count + 1` 
done
POLICY=Pressure
OUTPUT=pressure_testbed_time_long_noPA
rm -f $OUTPUT
count=0
for i in $SEQ; do
    for j in `seq $REPS`; do
    echo -n $count $i "" >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN | sed -e 's/xxx/'$i'/g' > $ROUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $MASTERSCRIPT  > out
    $CLEANSCRIPT
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
  count=`expr $count + 1` 
done
POLICY=HotSpot
OUTPUT=hotspot_testbed_time_long_noPA
rm -f $OUTPUT
count=0
for i in $SEQ; do
    for j in `seq $REPS`; do
    echo -n $count $i "" >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN | sed -e 's/xxx/'$i'/g' > $ROUT
    java -cp $CPVAR usr.globalcontroller.GlobalController $MASTERSCRIPT  > out
    $CLEANSCRIPT
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    tail -50 traffic.agg | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
  count=`expr $count + 1` 
done

