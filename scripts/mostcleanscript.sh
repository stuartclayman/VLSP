#!/bin/sh
echo "killing java and deleting temp files"
ssh rclegg@clayone -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\" ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\""
ssh rclegg@claytwo -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\" ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\""
ssh rclegg@claythree -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\" ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\"" 
ssh rclegg@clayfour -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\" ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\""
ssh rclegg@ethane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\" ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\""
ssh rclegg@methane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\" ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\""
ssh rclegg@butane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\" ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\""
ssh rclegg@propane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\" ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\""
ssh rclegg@pentane -C "killall -9 java; rm /space/rclegg/testbed*; find /tmp -user rclegg -name \"info-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\" ; find /tmp -user rclegg -name \"agg-*\" -exec rm {} \; 2>&1 |  grep -v \"Permission denied\""

