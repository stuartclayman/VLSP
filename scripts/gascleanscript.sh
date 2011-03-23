#!/bin/sh
echo "killing java and deleting temp files"
ssh rclegg@ethane -C "killall -9 java; rm -f /space/rclegg/testbed*; find /tmp -name \"info-*\" -exec rm -f {} \; ; find /tmp -name \"agg-*\" -exec rm -f {} \;"
ssh rclegg@methane -C "killall -9 java; rm -f /space/rclegg/testbed*; find /tmp -name \"info-*\" -exec rm -f {} \; ; find /tmp -name \"agg-*\" -exec rm -f {} \;"
ssh rclegg@butane -C "killall -9 java; rm -f /space/rclegg/testbed*; find /tmp -name \"info-*\" -exec rm -f {} \; ; find /tmp -name \"agg-*\" -exec rm -f {} \;"
ssh rclegg@propane -C "killall -9 java; rm -f /space/rclegg/testbed*; find /tmp -name \"info-*\" -exec rm -f {} \; ; find /tmp -name \"agg-*\" -exec rm -f {} \;"
ssh rclegg@pentane -C "killall -9 java; rm -f /space/rclegg/testbed*; find /tmp -name \"info-*\" -exec rm -f {} \; ; find /tmp -name \"agg-*\" -exec rm -f {} \;"
