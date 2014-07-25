// ThreadListReporter.java

package usr.globalcontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;


import usr.common.BasicRouterInfo;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.ProbeValue;
import eu.reservoir.monitoring.core.ProbeValueWithName;
import eu.reservoir.monitoring.core.Reporter;
import eu.reservoir.monitoring.core.ReporterMeasurementType;
import eu.reservoir.monitoring.core.list.MList;
import eu.reservoir.monitoring.core.table.*;

import com.timeindexing.time.MillisecondTimestamp;
import com.timeindexing.time.MicrosecondTimestamp;
import com.timeindexing.time.NanosecondTimestamp;

/**
 * A ThreadListReporter collects measurements sent by
 * a ThreadListProbe embedded in each Router.
 * It shows the apps running on a router.
 */
public class ThreadListReporter implements Reporter, ReporterMeasurementType {
    GlobalController globalController;


    // A HashMap of router name -> latest measurement
    HashMap<String, Measurement> measurements;

    // count of no of measurements
    int count = 0;

    /**
     * Constructor
     */
    public ThreadListReporter(GlobalController gc) {
        globalController = gc;
        measurements = new HashMap<String, Measurement>();

        // get logger
        try {
            Logger.getLogger("log").addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel15.out")), new BitMask(1<<15));
        } catch (FileNotFoundException fnfe) {
            Logger.getLogger("log").logln(USR.ERROR, fnfe.toString());
        }
    }

    /**
     * Return the measurement types this Reporter accepts.
     */
    public List<String> getMeasurementTypes() {
        List<String> list = new ArrayList<String>();

        list.add("ThreadList");

        return list;
    }

    /**
     * This collects each measurement and processes it.
     * In this case it stores the last measurement for each LocalController.
     * The measurement can be retrieved using the getData() method.
     */
    @Override
    public void report(Measurement m) {
        if (m.getType().equals("ThreadList")) {
            count++;

            List<ProbeValue> values = m.getValues();

            // ProbeValue 0 is the Router name
            ProbeValue pv0 = values.get(0);
            String routerName = (String)pv0.getValue();

            synchronized (measurements) {
                measurements.put(routerName, m);
            }


            Logger.getLogger("log").logln(1<<15, showData(m));


        } else {
            // not what we were expecting
        }
    }

    /**
     * Get the last measurement for the specified LocalController
     * 
     * Each measurement has the following structure:
     * ProbeValues
     * 0: RouterName: STRING: name
     * 1: Data: TABLE
     *
     * Table elements are:
     * 0: Name: STRING - thread  name
     * 1: StartTime: LONG - start time since epoch - in milliseconds
     * 2: ElapsedTime: LONG - time since start time - in milliseconds
     * 3: RunTime: LONG - cpu time - in nanoseconds
     * 4: UserTime: LONG - user part of cpu time - in nanoseconds
     * 5: SysTime: LONG - sys part of cpu time - in nanoseconds
     * 6: Mem: LONG - total bytes this thread has asked the run-time to allocate
     * 1: ThreadGroup: STRING - thread group  name
     */
    public Measurement getData(String routerName) {
        return measurements.get(routerName);
    }



    protected String showData(Measurement m) {
        StringBuilder builder = new StringBuilder();

        /*
        List<ProbeValue> values = m.getValues();

        for (ProbeValue value : values) {
            if (value instanceof ProbeValueWithName) {
                builder.append(((ProbeValueWithName)value).getName());
                builder.append(": ");
            }

            builder.append(value.getValue());
            builder.append(" ");
        }

        */

        // totals
        long startTimeR = 0;
        long elapsedTimeR = 0;
        long cpuT = 0;  // in milliseconds
        long userT = 0;  // in milliseconds
        long sysT = 0;  // in milliseconds
        long memT = 0;  // in Mb
        

        List<ProbeValue> values = m.getValues();

        // ProbeValue 0 is the name
        ProbeValue pv0 = values.get(0);
        String routerName = (String)pv0.getValue();

        // ProbeValue 1 is the Table
        ProbeValue pv1 = values.get(1);
        Table table = (Table)pv1.getValue();

        // visit all rows
        List<TableRow> rows = table.toList();

        for (TableRow row : rows) {
            String name = (String)row.get(0).getValue();
            Long time = (Long)row.get(1).getValue();
            Long elapsed = (Long)row.get(2).getValue();
            Long cpu = (Long)row.get(3).getValue();
            Long user = (Long)row.get(4).getValue();
            Long sys = (Long)row.get(5).getValue();
            Long mem = (Long)row.get(6).getValue();
            String threadGroupName = (String)row.get(7).getValue();


            // add up
            cpuT += (cpu / 1000);
            userT += (user / 1000);
            sysT += (sys / 1000);
            memT += (mem / 1000000);


            builder.append(routerName + " -- "  + name + " - " + threadGroupName + " -- " +  " starttime: " + new MillisecondTimestamp(time) +  " elapsed: " + new MillisecondTimestamp(elapsed) +  " cpu: " + new MicrosecondTimestamp(cpu/1000) + " user: " + new MicrosecondTimestamp(user/1000) + " system: " + new MicrosecondTimestamp(sys/1000)  + " mem: " + mem);
            builder.append("\n");

        }

        builder.append("\n");

        return builder.toString();
    }

}
