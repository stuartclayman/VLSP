// HostInfoReporter.java

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

/**
 * A HostInfoReporter collects measurements sent by
 * a AppListProbe embedded in each Router.
 * It shows the apps running on a router.
 */
public class HostInfoReporter implements Reporter, ReporterMeasurementType {
    GlobalController globalController;


    // A HashMap of LocalController name -> latest measurement
    HashMap<String, Measurement> measurements;

    // count of no of measurements
    int count = 0;

    /**
     * Constructor
     */
    public HostInfoReporter(GlobalController gc) {
        globalController = gc;
        measurements = new HashMap<String, Measurement>();

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
                measurements.put(localControllerName, m);
            }


            Logger.getLogger("log").logln(1<<13, showData(m));


        } else {
            // not what we were expecting
        }
    }

    /**
     * Get the last measurment for the specified LocalController
     * 
     * Each measurement has the following structure:
     * ProbeValues
     * 0: Name: STRING: name
     * 1: cpu-user: FLOAT: percent
     * 2: cpu-sys: FLOAT: percent
     * 3: cpu-idle: FLOAT: percent
     * 4: mem-used: INTEGER: Mb
     * 5: mem-free: INTEGER: Mb
     * 6: mem-total: INTEGER: Mb
     * 7: in-packets: LONG: n
     * 8: in-bytes: LONG: n
     * 9: out-packets: LONG: n
     * 10: out-bytes: LONG: n
     * 
     * HostInfo attributes: [0: STRING LocalController:10000, 1: FLOAT 7.72, 2: FLOAT 14.7, 3: FLOAT 77.57, 4: INTEGER 15964, 5: INTEGER 412, 6: INTEGER 16376, 7: LONG 50728177, 8: LONG 43021697138, 9: LONG 40879848, 10: LONG 7519963728]
     */
    public Measurement  getData(String localControllerName) {
        return measurements.get(localControllerName);
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
