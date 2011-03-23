#!/bin/sh
echo "killing java and deleting temp files"
ssh rclegg@clayone -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -name \"info-*\" -exec rm {} \; ; find /tmp -name \"agg-*\" -exec rm {} \;"
ssh rclegg@claytwo -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -name \"info-*\" -exec rm {} \; ; find /tmp -name \"agg-*\" -exec rm {} \;"
ssh rclegg@claythree -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -name \"info-*\" -exec rm {} \; ; find /tmp -name \"agg-*\" -exec rm {} \;"
