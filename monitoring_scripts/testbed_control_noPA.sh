#!/bin/sh

RIN=monitoring_scripts/testbed_router_dummy_noPA.xml
ROUT=monitoring_scripts/testbed_routeroptions.xml
CONTROL_SCRIPT=monitoring_scripts/testbed_control_noPA.xml
AWK=gawk

SEQ="0.01 0.02 0.05 0.1 0.2"
ITER=5
OUTPUT=random_testbed_noPA
POLICY=Random
rm -f $OUTPUT
for i in  $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i "" >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN |  sed -e 's/xxx/'$i'/g' > $ROUT
    java usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
done
POLICY=Pressure
OUTPUT=pressure_testbed_noPA
rm -f $OUTPUT
for i in $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN |  sed -e 's/xxx/'$i'/g' > $ROUT
    java usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
done
POLICY=HotSpot
OUTPUT=hotspot_testbed_noPA
rm -f $OUTPUT
for i in $SEQ; do
    for j in `seq $ITER`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN |  sed -e 's/xxx/'$i'/g' > $ROUT
    java usr.globalcontroller.GlobalController $CONTROL_SCRIPT  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
done

