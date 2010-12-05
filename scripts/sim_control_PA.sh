#!/bin/sh

RIN=scripts/routerdummy.xml
ROUT=scripts/routeroptions.xml
PIN=scripts/probdummy_PA.xml
POUT=scripts/probdists.xml
AWK=gawk

SEQ="0.1 0.25 0.5 1.0 2.5 5.0 10.0 25.0 50.0"

OUTPUT=random_sim_PA
POLICY=Random
rm -f $OUTPUT
for i in  $SEQ; do
    for j in `seq 5`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java usr.globalcontroller.GlobalController scripts/simcontrol.xml  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
done
POLICY=Pressure
OUTPUT=pressure_sim_PA
rm -f $OUTPUT
for i in $SEQ; do
    for j in `seq 5`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java usr.globalcontroller.GlobalController scripts/simcontrol.xml  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
done
POLICY=HotSpot
OUTPUT=hotspot_sim_PA
rm -f $OUTPUT
for i in $SEQ; do
    for j in `seq 5`; do
    echo -n $i " " >> $OUTPUT
    sed -e 's/yyy/'$POLICY'/g' $RIN > $ROUT
    sed -e 's/xxx/'$i'/g' $PIN > $POUT
    java usr.globalcontroller.GlobalController scripts/simcontrol.xml  > out
    tail -50 summary.out | $AWK '{for (i=1; i <= NF; i++) {a[i]+= $i} n++;}END{for (i=1; i <= NF; i++) printf("%g ",a[i]/n)}' >> $OUTPUT
    echo >> $OUTPUT
  done
done

