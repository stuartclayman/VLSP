// ThreadListProbe.java

package usr.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import usr.applications.ApplicationHandle;
import usr.applications.RuntimeMonitoring;
import usr.common.TimedThread;
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
 * for each executing App.
 */
public class ThreadListProbe extends RouterProbe implements Probe {
    // The TableHeader for the table of stats
    TableHeader statsHeader;

    // Save table, so we only send different ones
    DefaultTable savedT = null;

    // ThreadMXBean
    ThreadMXBean mxBean;

    /**
     * Construct a ThreadListProbe
     */
    public ThreadListProbe(RouterController cont) {
        setController(cont);

        // set probe name
        setName(cont.getName()+".appList");
        // set data rate
        setDataRate(new EveryNSeconds(10));

        // Define the header. Has:
        // Name
        // ThreadName
        // ClassName
        // State
        statsHeader = new DefaultTableHeader()
            .add("AID", ProbeAttributeType.INTEGER)
            .add("StartTime", ProbeAttributeType.LONG)
            .add("RunTime", ProbeAttributeType.INTEGER)
            .add("State", ProbeAttributeType.STRING)
            .add("ClassName", ProbeAttributeType.STRING)
            .add("Args", ProbeAttributeType.STRING)
            .add("Name", ProbeAttributeType.STRING)
            .add("RuntimeKeys", ProbeAttributeType.LIST)
            .add("RuntimeValues", ProbeAttributeType.LIST)
        ;

        //add("ThreadName", ProbeAttributeType.STRING);


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
        //System.out.println("ThreadListProbe: collect()");

        long now = System.currentTimeMillis();



        // try ThreadGroup info
        ThreadGroup threadGroup1 = getController().getThreadGroup();

        // now get all the threads for the Controller
        Thread[] threads = ThreadTools.getGroupThreadsRecursive(threadGroup1);

        if (threads == null || threads.length == 0) {
            // no threads to report
            return null;

        } else {

            try {

                long cpu = 0;
                long user = 0;
                long sys = 0;
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
                for (int i=0; i<threads.length; i++) {
                    Thread t = threads[i];

                    long id = t.getId();

                    ThreadGroup threadGroup = t.getThreadGroup();


                    if (t instanceof TimedThread) {
                        TimedThread tt = (TimedThread) t;

                        long[] result = tt.getCpuUsage();

                        cpu = result[0];
                        user = result[1];
                        sys = result[2];
                        elapsed = tt.getElapsedTime();

                    } else {

                        ThreadInfo info = mxBean.getThreadInfo(id);

                        long threadCPU = mxBean.getThreadCpuTime(id);
                        long threadUser = mxBean.getThreadUserTime(id);

                        cpu = threadCPU;
                        user = threadUser;
                        sys = (threadCPU - threadUser);
                        elapsed = 0;
                    }


                    //System.out.println(" " + time + " Thread " + t.getThreadGroup().getName() + "." + t.getName() + " id: " + t.getId() + " cpu: " + (threadCPU/1000000) + " user: " + (threadUser/1000000) + " system: " + ((threadCPU - threadUser)/1000000));

                    System.out.println(" " + new MillisecondTimestamp(time) +  " elapsed: " + new MillisecondTimestamp(elapsed) +  " " + threadGroup.getName()  + " - " + t.getName() + " cpu: " + new MicrosecondTimestamp(cpu/1000) + " user: " + new MicrosecondTimestamp(user/1000) + " system: " + new MicrosecondTimestamp(sys/1000) );



                    /*

                    // create a row for ApplicationHandle data
                    TableRow appHRow = new DefaultTableRow();

                    // AID
                    appHRow.add(new DefaultTableValue(ah.getID()));

                    // StartTime
                    appHRow.add(new DefaultTableValue(ah.getStartTime()));

                    // RunTime
                    appHRow.add(new DefaultTableValue((int)(now - ah.getStartTime())));

                    // State
                    appHRow.add(new DefaultTableValue(ah.getState().toString()));

                    // ClassName
                    appHRow.add(new DefaultTableValue(ah.getApplication().getClass().getName()));

                    // Args
                    appHRow.add(new DefaultTableValue(Arrays.asList(ah.getArgs()).toString()));

                    // Name
                    appHRow.add(new DefaultTableValue(ah.getName()));

                    // check if we should get run time monitoring data
                    if (ah.getApplication() instanceof RuntimeMonitoring) {
                        // yes
                        MList keys = new DefaultMList(ProbeAttributeType.STRING);
                        MList values = new DefaultMList(ProbeAttributeType.STRING);

                        // get the data
                        Map<String, String> theMap = ((RuntimeMonitoring)ah.getApplication()).getMonitoringData();

                        // add the keys and values
                        for (Map.Entry<String, String> entry : theMap.entrySet()) {
                            keys.add(entry.getKey());
                            values.add(entry.getValue());
                        }

                        appHRow.add(new DefaultTableValue(keys));
                        appHRow.add(new DefaultTableValue(values));

                    } else {
                        // no
                        appHRow.add(new DefaultTableValue(new DefaultMList(ProbeAttributeType.STRING)));
                        appHRow.add(new DefaultTableValue(new DefaultMList(ProbeAttributeType.STRING)));

                    }

                    // add this row to the table
                    statsTable.addRow(appHRow);

                    */

                }

                list.add(new DefaultProbeValue(1, statsTable));

                // TODO: do the following as a ProbeFilter.
                // if the tables are the same, don not send a new measurement
                if (tablesEqual(savedT, statsTable)) {
                    // nothing to send
                    return null;
                } else {
                    // set the type to be: ThreadList
                    ProducerMeasurement lastestM = new ProducerMeasurement(this, list, "ThreadList");
                    savedT = statsTable;
                    return lastestM;
                }


            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
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
