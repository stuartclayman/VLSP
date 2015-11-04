// ThreadGroupListReporter.java

package usr.globalcontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;


import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONArray;
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
 * A ThreadGroupListReporter collects measurements sent by
 * a ThreadGroupListProbe embedded in each Router.
 * It shows the apps running on a router.
 */
public class ThreadGroupListReporter implements Reporter, ReporterMeasurementType {
    GlobalController globalController;


    // A HashMap of router name -> latest measurement
    HashMap<String, Measurement> measurements;

    // count of no of measurements
    int count = 0;

    // keep previous probe values
    HashMap<String, Measurement> previousProbeValues;

    /**
     * Constructor
     */
    public ThreadGroupListReporter(GlobalController gc) {
        globalController = gc;
        measurements = new HashMap<String, Measurement>();
        previousProbeValues = new HashMap<String, Measurement>();

        // get logger
        try {
            Logger.getLogger("log").addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel14.out")), new BitMask(1<<14));
        } catch (FileNotFoundException fnfe) {
            Logger.getLogger("log").logln(USR.ERROR, fnfe.toString());
        }
    }

    /**
     * Return the measurement types this Reporter accepts.
     */
    public List<String> getMeasurementTypes() {
        List<String> list = new ArrayList<String>();

        list.add("ThreadGroupList");

        return list;
    }

    /**
     * This collects each measurement and processes it.
     * In this case it stores the last measurement for each LocalController.
     * The measurement can be retrieved using the getData() method.
     */
    @Override
    public void report(Measurement m) {
        if (m.getType().equals("ThreadGroupList")) {
            count++;

            List<ProbeValue> values = m.getValues();

            // ProbeValue 0 is the Router name
            ProbeValue pv0 = values.get(0);
            String routerName = (String)pv0.getValue();

            previousProbeValues.put(routerName, measurements.get(routerName));

            synchronized (measurements) {
                measurements.put(routerName, m);
            }


            Logger.getLogger("log").logln(1<<14, showData(m));


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
     * 0: Name: STRING - thread gorpu name
     * 1: StartTime: LONG - start time since epoch - in milliseconds
     * 2: ElapsedTime: LONG - time since start time - in milliseconds
     * 3: RunTime: LONG - cpu time - in nanoseconds
     * 4: UserTime: LONG - user part of cpu time - in nanoseconds
     * 5: SysTime: LONG - sys part of cpu time - in nanoseconds
     * 6: Mem: LONG - total bytes this thread has asked the run-time to allocate
     */
    public Measurement getData(String routerName) {
        return measurements.get(routerName);
    }

    public Measurement getPreviousData(String localControllerName) {
        return previousProbeValues.get(localControllerName);
    }

    
    // this method returns a JSONObject 
    public JSONObject getProcessedData (String routerName) {
        Measurement m = measurements.get(routerName);

        if (m == null) {
            return null;
        } else {
                        
            // totals
            long startTimeR = 0;
            long elapsedTimeR = 0;
            long cpuT = 0;  // in milliseconds
            long userT = 0;  // in milliseconds
            long sysT = 0;  // in milliseconds
            long memT = 0;  // in Kb
        

            List<ProbeValue> values = m.getValues();

            // ProbeValue 0 is the router name
            //ProbeValue pv0 = values.get(0);
            //String routerName = (String)pv0.getValue();

            // ProbeValue 1 is the Table
            ProbeValue pv1 = values.get(1);
            Table table = (Table)pv1.getValue();

            // visit all rows
            List<TableRow> rows = table.toList();

            // result object
            JSONObject jsobj = new JSONObject();

            JSONArray array = new JSONArray();

            // create result
            try {

                for (TableRow row : rows) {
                    String name = (String)row.get(0).getValue();
                    Long time = (Long)row.get(1).getValue();
                    Long elapsed = (Long)row.get(2).getValue();
                    Long cpu = (Long)row.get(3).getValue();
                    Long user = (Long)row.get(4).getValue();
                    Long sys = (Long)row.get(5).getValue();
                    Long mem = (Long)row.get(6).getValue();


                    // add up
                    cpuT += (cpu / 1000);
                    userT += (user / 1000);
                    sysT += (sys / 1000);
                    memT += (mem / 1000);


                    // the first time through we set the startTime and the elapsedTime
                    // the first row has the first ThreadGroup of a router
                    // this data is what we need
                    if (startTimeR == 0 && elapsedTimeR == 0) {
                        startTimeR = time;
                        elapsedTimeR = elapsed;
                    }

                    JSONObject jsRow = new JSONObject();

                    jsRow.put("name", name);
                    jsRow.put("starttime", new MillisecondTimestamp(time));
                    jsRow.put("elapsed", elapsed);
                    jsRow.put("cpu",  cpu/1000);
                    jsRow.put("user",  user/1000);
                    jsRow.put("system", sys/1000);
                    jsRow.put("mem", mem/1000);
                    
                    array.put(jsRow);
                }


                // totals
                JSONObject jsRow = new JSONObject();

                jsRow.put("name", "TOTAL");
                jsRow.put("starttime", new MillisecondTimestamp(startTimeR));
                jsRow.put("elapsed", elapsedTimeR);
                jsRow.put("cpu",  cpuT);
                jsRow.put("user",  userT);
                jsRow.put("system", sysT);
                jsRow.put("mem", memT);
                    
                array.put(jsRow);


                jsobj.put("threadgroup", array);


            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return jsobj;
        }

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
        long memT = 0;  // in Kb
        

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


            // add up
            cpuT += (cpu / 1000);
            userT += (user / 1000);
            sysT += (sys / 1000);
            memT += (mem / 1000);


            // the first time through we set the startTime and the elapsedTime
            // the first row has the first ThreadGroup of a router
            // this data is what we need
            if (startTimeR == 0 && elapsedTimeR == 0) {
                startTimeR = time;
                elapsedTimeR = elapsed;
            }

            builder.append(routerName + " -- "  + name + " -- " +  " starttime: " + new MillisecondTimestamp(time) +  " elapsed: " + new MillisecondTimestamp(elapsed) +  " cpu: " + new MicrosecondTimestamp(cpu/1000) + " user: " + new MicrosecondTimestamp(user/1000) + " system: " + new MicrosecondTimestamp(sys/1000)  + " mem: " + mem);
            builder.append("\n");

        }

        builder.append(routerName + " -- " + "TOTALS" + " -- "  + " starttime: " + new MillisecondTimestamp(startTimeR) +  " elapsed: " + new MillisecondTimestamp(elapsedTimeR) + " cpu: " + new MicrosecondTimestamp(cpuT) + " user: " + new MicrosecondTimestamp(userT) + " system: " + new MicrosecondTimestamp(sysT)  + " mem: " + memT );
        builder.append("\n");

        return builder.toString();
    }

}
