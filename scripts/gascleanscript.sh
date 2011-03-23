#!/bin/sh
echo "killing java and deleting temp files"
ssh rclegg@ethane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@methane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@butane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@propane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@pentane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
