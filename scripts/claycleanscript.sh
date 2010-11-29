#!/bin/sh
echo "killing java and deleting temp files"
ssh rclegg@clayone -C "killall -9 java; rm /space/rclegg/testbed*; rm /tmp/info-*; rm /tmp/agg-*"
ssh rclegg@claytwo -C "killall -9 java; rm /space/rclegg/testbed*; rm /tmp/info-*; rm /tmp/agg-*"
ssh rclegg@claythree -C "killall -9 java; rm /space/rclegg/testbed*; rm /tmp/info-*; rm /tmp/agg-*"
