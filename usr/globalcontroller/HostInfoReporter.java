// HostInfoReporter.java

package usr.globalcontroller;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONArray;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.ProbeValue;
import eu.reservoir.monitoring.core.ProbeValueWithName;
import eu.reservoir.monitoring.core.Reporter;
import eu.reservoir.monitoring.core.ReporterMeasurementType;
import eu.reservoir.monitoring.core.table.Table;
import eu.reservoir.monitoring.core.table.TableAttribute;
import eu.reservoir.monitoring.core.table.TableRow;
import eu.reservoir.monitoring.core.table.TableValue;
import eu.reservoir.monitoring.core.table.TableHeader;

/**
 * A HostInfoReporter collects measurements sent by
 * a AppListProbe embedded in each Router.
 * It shows the apps running on a router.
 */
public class HostInfoReporter implements Reporter, ReporterMeasurementType {
    GlobalController globalController;


    // A HashMap of LocalController name -> latest measurement
    HashMap<String, Measurement> measurements;

    long measurementTime = 0L;
    
    // count of no of measurements
    int count = 0;

    // keep previous probe values
    HashMap<String, Measurement> previousProbeValues;

    long previousMeasurementTime = 0L;
    
    /**
     * Constructor
     */
    public HostInfoReporter(GlobalController gc) {
        globalController = gc;
        measurements = new HashMap<String, Measurement>();
        previousProbeValues = new HashMap<String, Measurement>();

        // get logger
        try {
            Logger.getLogger("log").addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel13.out")), new BitMask(1<<13));
        } catch (FileNotFoundException fnfe) {
            Logger.getLogger("log").logln(USR.ERROR, fnfe.toString());
        }
    }

    /**
     * Return the measurement types this Reporter accepts.
     */
    public List<String> getMeasurementTypes() {
        List<String> list = new ArrayList<String>();

        list.add("HostInfo");

        return list;
    }

    /**
     * This collects each measurement and processes it.
     * In this case it stores the last measurement for each LocalController.
     * The measurement can be retrieved using the getData() method.
     */
    @Override
    public void report(Measurement m) {
        if (m.getType().equals("HostInfo")) {
            count++;

            List<ProbeValue> values = m.getValues();

            // ProbeValue 0 is the LocalController name
            ProbeValue pv0 = values.get(0);
            String localControllerName = (String)pv0.getValue();

            synchronized (measurements) {
                // keep the previous probe value, if exists
                previousProbeValues.put(localControllerName, measurements.get(localControllerName));
                previousMeasurementTime = measurementTime;


                measurements.put(localControllerName, m);
                measurementTime = System.currentTimeMillis();
            }

            Logger.getLogger("log").logln(1<<13, showData(m));


        } else {
            // not what we were expecting
        }
    }

    /**
     * Get the last measurement for the specified LocalController
     * 
     * Each measurement has the following structure:
     * ProbeValues
     * 0: Name: STRING: name
     * 1: cpu-user: FLOAT: percent
     * 2: cpu-sys: FLOAT: percent
     * 3: cpu-idle: FLOAT: percent
     * 4: load-average: FLOAT percent
     * 5: mem-used: INTEGER: Mb
     * 6: mem-free: INTEGER: Mb
     * 7: mem-total: INTEGER: Mb
     * 8: net-stats: TABLE
     * col 0: if-name: STRING
     * col 1: in-packets: LONG
     * col 2: in-bytes: LONG
     * col 3: out-packets: LONG
     * col 4: out-bytes: LONG
     * 
     * HostInfo attributes: [0: STRING LocalController:10000, 1: FLOAT 7.72, 2: FLOAT 14.7, 3: FLOAT 77.57, 4: INTEGER 15964, 5: INTEGER 412, 6: INTEGER 16376, 7: LONG 50728177, 8: [<0: if-name: STRING>, <1: in-packets: LONG>, <2: in-bytes: LONG>, <3: out-packets: LONG>, <4: out-bytes: LONG>]
[(STRING en0), (LONG 67991652), (LONG 51397761527), (LONG 46334061), (LONG 8412540222)]
[(STRING awdl0), (LONG 17), (LONG 2750), (LONG 8938), (LONG 3792906)]
     */
    public Measurement getData(String localControllerName) {
        return measurements.get(localControllerName);
    }

    public Measurement getPreviousData(String localControllerName) {
        return previousProbeValues.get(localControllerName);
    }

    // this method returns a JSONObject with the difference in inbound/outbound traffic between the latest two probes
    public JSONObject getProcessedData (String localControllerName) {
        Measurement m = measurements.get(localControllerName);

        if (m == null) {
            return null;
        } else {
                        
            List<ProbeValue> currentProbeValue = m.getValues();

            Measurement prevM = previousProbeValues.get(localControllerName);
            List<ProbeValue> previousProbeValue = null;

            if (prevM != null) previousProbeValue = prevM.getValues();

            JSONObject jsobj = new JSONObject();

            try {
                jsobj.put("name", localControllerName);

                jsobj.put("cpuIdle", ((Float) currentProbeValue.get(3).getValue()) / 100F); // percentage
                jsobj.put("cpuLoad", ((Float) currentProbeValue.get(1).getValue() + (Float) currentProbeValue.get(2).getValue())/100F); // percentage
                jsobj.put("freeMemory", (Float) ((Integer)currentProbeValue.get(6).getValue() / 1024f)); // in GBs
                jsobj.put("usedMemory", (Float) ((Integer)currentProbeValue.get(5).getValue() / 1024f)); // in GBs

                /*
                  Float load=(Float) currentProbeValue.get(11).getValue();

                  load = load / 100f;
	
                  jsobj.put("loadAverage", load); // percentage
                */
                        
                jsobj.put("loadAverage", ((Float) currentProbeValue.get(4).getValue()));
                        

                /* net-stats table */
                
                // ProbeValue 8 is the table
                ProbeValue pv8 = currentProbeValue.get(8);
                Table table = (Table)pv8.getValue();


                JSONArray netstats = convertRowDataToJSON(table);

                jsobj.put("netstats", netstats);
                
                if (previousProbeValue==null) {
                    // starts with zero bytes
                    jsobj.put("networkIncomingPackets", 0);
                    jsobj.put("networkIncomingBytes", 0);
                    jsobj.put("networkOutboundPackets", 0);
                    jsobj.put("networkOutboundBytes", 0);
                } else {
                    // subtract from previous probe
                    Long[] currentTotals = calculateTotals(table);
                    Long[] previousTotals = calculateTotals((Table)previousProbeValue.get(8).getValue());

                    jsobj.put("networkIncomingPackets", currentTotals[0] - previousTotals[0]);
                    jsobj.put("networkIncomingBytes", currentTotals[1] - previousTotals[1]);
                    jsobj.put("networkOutboundPackets", currentTotals[2] - previousTotals[2]);
                    jsobj.put("networkOutboundBytes", currentTotals[3] - previousTotals[3]);
                }

                /* timestamps */
                jsobj.put("timestamp-begin", previousMeasurementTime);
                jsobj.put("timestamp-end", measurementTime);

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return jsobj;
        }
    }


    /**
     * Convert data for the row in a table
     */
    private JSONArray convertRowDataToJSON(Table table) {
        try {
            int rows = table.getRowCount();

            JSONArray retval = new JSONArray();


            TableHeader header = table.getColumnDefinitions();

            // skip through rows
            for (int r = 0; r < rows; r++) {
                TableRow row = table.getRow(r);

                // convert row to JSON
                int rowSize = row.size();

                JSONObject data = new JSONObject();

                for (int d=0; d<rowSize; d++) {
                    data.put(header.get(d).getName(), row.get(d).getValue());
                }

                // add to retval
                retval.put(data);
            }

            return retval;
            
        } catch (JSONException jse) {
            return null;
        }
    }

    /**
     * Skip through table of netstats data and calculate totals
     */
    private Long[] calculateTotals(Table table) {
        int rows = table.getRowCount();

        Long[] retval = {0L, 0L, 0L, 0L };


        TableHeader header = table.getColumnDefinitions();

        // skip through rows
        for (int r = 0; r < rows; r++) {
            TableRow row = table.getRow(r);

        // add up amounts
            // skip name
            for (int d=1; d<=4 ; d++) {
                retval[d-1] += (Long)row.get(d).getValue();
            }
        }

        return retval;
    }

    protected String showData(Measurement m) {
        StringBuilder builder = new StringBuilder();

        List<ProbeValue> values = m.getValues();

        for (ProbeValue value : values) {
            if (value instanceof ProbeValueWithName) {
                builder.append(((ProbeValueWithName)value).getName());
                builder.append(": ");
            }

            builder.append(value.getValue());
            builder.append(" ");
        }

        return builder.toString();
    }

}
