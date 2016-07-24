// ThreadListProbe.java

package usr.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.List;

import usr.applications.ApplicationHandle;
import usr.applications.RuntimeMonitoring;
import usr.common.TimedThreadGroup;
import usr.common.ThreadTools;
import eu.reservoir.monitoring.appl.datarate.EveryNSeconds;
import eu.reservoir.monitoring.core.DefaultProbeAttribute;
import eu.reservoir.monitoring.core.DefaultProbeValue;
import eu.reservoir.monitoring.core.Probe;
import eu.reservoir.monitoring.core.ProbeAttributeType;
import eu.reservoir.monitoring.core.ProbeMeasurement;
import eu.reservoir.monitoring.core.ProbeValue;
import eu.reservoir.monitoring.core.ProducerMeasurement;
import eu.reservoir.monitoring.core.list.DefaultMList;
import eu.reservoir.monitoring.core.list.MList;
import eu.reservoir.monitoring.core.table.DefaultTable;
import eu.reservoir.monitoring.core.table.DefaultTableHeader;
import eu.reservoir.monitoring.core.table.DefaultTableRow;
import eu.reservoir.monitoring.core.table.DefaultTableValue;
import eu.reservoir.monitoring.core.table.Table;
import eu.reservoir.monitoring.core.table.TableHeader;
import eu.reservoir.monitoring.core.table.TableProbeAttribute;
import eu.reservoir.monitoring.core.table.TableRow;
import eu.reservoir.monitoring.core.table.TableValue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;

import com.timeindexing.time.ElapsedMillisecondTimestamp;
import com.timeindexing.time.MillisecondTimestamp;
import com.timeindexing.time.MicrosecondTimestamp;
import com.timeindexing.time.NanosecondTimestamp;

/**
 * A probe that talks to a Router can collects the stats
 * for each executing ThreadGroup.
 */
public class ThreadGroupListProbe extends RouterProbe implements Probe {
    // The TableHeader for the table of stats
    TableHeader statsHeader;

    // Save table, so we only send different ones
    DefaultTable savedT = null;

    // ThreadMXBean
    ThreadMXBean mxBean;

    /**
     * Construct a ThreadGroupListProbe
     */
    public ThreadGroupListProbe(RouterController cont) {
        setController(cont);

        // set probe name
        setName(cont.getName()+".probe.ThreadGroupList");
        // set data rate
        setDataRate(new EveryNSeconds(10));

        // Define the header. Has:
        // Name
        // ThreadName
        // ClassName
        // State
        statsHeader = new DefaultTableHeader()
            .add("Name", ProbeAttributeType.STRING)
            .add("StartTime", ProbeAttributeType.LONG)
            .add("ElapsedTime", ProbeAttributeType.LONG)
            .add("RunTime", ProbeAttributeType.LONG)
            .add("UserTime", ProbeAttributeType.LONG)
            .add("SysTime", ProbeAttributeType.LONG)
            .add("Mem", ProbeAttributeType.LONG)
        ;


        // setup the probe attributes
        // The router name
        // The table of stats
        addProbeAttribute(new DefaultProbeAttribute(0, "RouterName", ProbeAttributeType.STRING, "name"));
        addProbeAttribute(new TableProbeAttribute(1, "Data", statsHeader));

        mxBean = ManagementFactory.getThreadMXBean();

    }

    /**
     * Collect a measurement.
     */
    @Override
    public ProbeMeasurement collect() {
        //System.out.println("ThreadGroupListProbe: collect()");
        try {
            long now = System.currentTimeMillis();



            // try ThreadGroup info
            ThreadGroup threadGroup1 = getController().getThreadGroup();

            // now get all the threads for the Controller
            ThreadGroup[] subGroups = ThreadTools.getGroupThreadGroupsRecursive(threadGroup1);

            // add current ThreadGroup into list
            List<ThreadGroup> threadGroups = new ArrayList<ThreadGroup>(Arrays.asList(subGroups));
            threadGroups.add(0, threadGroup1);

            if (threadGroups == null || threadGroups.size() == 0) {
                // no threads to report
                return null;

            } else {

                long cpu = 0;
                long user = 0;
                long sys = 0;
                long mem = 0;
                long elapsed = 0;

                long time = System.currentTimeMillis();

                // collate measurement values
                ArrayList<ProbeValue> list = new ArrayList<ProbeValue>();

                // add router name
                list.add(new DefaultProbeValue(0, getController().getName()));

                // now allocate a table
                DefaultTable statsTable = new DefaultTable();
                statsTable.defineTable(statsHeader);


                // List every thread in the group
                for (int i=0; i<threadGroups.size(); i++) {
                    ThreadGroup threadGroup = threadGroups.get(i);

                    if (! (threadGroup instanceof TimedThreadGroup)) {
                        //System.out.println(threadGroup.getName() + " NOT instanceof TimedThreadGroup");
                        continue;
                    }

                    if (threadGroup.activeCount() == 0) {
                        //System.out.println(threadGroup.getName() + " SIZE 0");
                        continue;
                    }


                    TimedThreadGroup tg = (TimedThreadGroup) threadGroup;

                    long[] result = tg.getUsage();

                    cpu = result[0];
                    user = result[1];
                    sys = result[2];
                    mem = result[3];
                    elapsed = tg.getElapsedTime();


                    // create a row for ApplicationHandle data
                    TableRow appHRow = new DefaultTableRow();

                    // Name
                    appHRow.add(new DefaultTableValue(tg.getName()));

                    // StartTime
                    appHRow.add(new DefaultTableValue(tg.getStartTime()));

                    // ElapsedTime
                    appHRow.add(new DefaultTableValue(elapsed));

                    // RunTime
                    appHRow.add(new DefaultTableValue(cpu));

                    // UserTime
                    appHRow.add(new DefaultTableValue(user));

                    // SysTime
                    appHRow.add(new DefaultTableValue(sys));

                    // Mem
                    appHRow.add(new DefaultTableValue(mem));



                    // add this row to the table
                    statsTable.addRow(appHRow);


                    //System.out.println("ThreadGroupListProbe: " + new MillisecondTimestamp(time) +  " elapsed: " + new MillisecondTimestamp(elapsed) +  " " + threadGroup.getName()  + " - " + tg.getName() + " cpu: " + new MicrosecondTimestamp(cpu/1000) + " user: " + new MicrosecondTimestamp(user/1000) + " system: " + new MicrosecondTimestamp(sys/1000)  + " mem: " + mem );

                }

                list.add(new DefaultProbeValue(1, statsTable));

                // set the type to be: ThreadList
                ProducerMeasurement lastestM = new ProducerMeasurement(this, list, "ThreadGroupList");
                return lastestM;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Are tables equal
     */
    private boolean tablesEqual(Table t1, Table t2) {
        // check all the rows

        if (t1 == null || t2 == null) {
            return false;
        } else {
            // get sizes
            int t1Rows = t1.getRowCount();
            int t2Rows = t2.getRowCount();

            if (t1Rows != t2Rows) {
                // different size - must be different
                return false;
            } else {
                // same size - check rows
                for (int r = 0; r < t1Rows; r++) {
                    // see if the rows are equal
                    TableRow t1Row = t1.getRow(r);
                    TableRow t2Row = t2.getRow(r);

                    int size = t1Row.size();

                    for (int e = 0; e < size; e++) {
                        TableValue t1V = t1Row.get(e);
                        TableValue t2V = t2Row.get(e);

                        if (!t1V.getValue().equals(t2V.getValue())) {
                            // a value is different - therefore table must be different
                            return false;
                        }
                    }
                }

                // all the rows are the same
                return true;

            }
        }
    }



}
