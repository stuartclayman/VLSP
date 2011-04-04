#!/bin/sh
echo "killing java and deleting temp files"
ssh rclegg@clayone -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@claytwo -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@claythree -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@clayfour -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@claydesk1 -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@claydesk2 -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@ethane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@methane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@butane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@propane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"
ssh rclegg@pentane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \;"

