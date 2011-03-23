#!/bin/sh
echo "killing java and deleting temp files"
ssh rclegg@clayone -C "killall -9 java; rm -f /space/rclegg/testbed*; find /tmp -name \"info-*\" -exec rm -f {} \; ; find /tmp -name \"agg-*\" -exec rm -f {} \;"
ssh rclegg@claytwo -C "killall -9 java; rm -f /space/rclegg/testbed*; find /tmp -name \"info-*\" -exec rm -f {} \; ; find /tmp -name \"agg-*\" -exec rm -f {} \;"
ssh rclegg@claythree -C "killall -9 java; rm -f /space/rclegg/testbed*; find /tmp -name \"info-*\" -exec rm -f {} \; ; find /tmp -name \"agg-*\" -exec rm -f {} \;"
