// LinuxMem.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Mar 2011

package plugins_usr.aggregator.appl;

import eu.reservoir.monitoring.core.*;
import java.util.*;
import eu.reservoir.monitoring.appl.host.linux.MemoryDev;

/**
 * A probe to get cpu info on a Linux system.
 * It uses /proc/memory to read the underyling data.
 */
public class LinuxMem extends AbstractProbe implements Probe  {
    // A MemDev object that reads info about the Mem.
    MemoryDev memDev;


    /*
     * Construct a LinuxMem probe
     */
    public LinuxMem(String name) {
        setName(name);
        setDataRate(new Rational(360, 1));

        // allocate a MemoryDev
        memDev = new MemoryDev();


        // add a probe attribute
        addProbeAttribute(new DefaultProbeAttribute(0, "reallyused", ProbeAttributeType.FLOAT, "percent"));
    }


    /**
     * Collect a measurement.
     */
    public ProbeMeasurement collect() {
        // create a list for the result
        ArrayList<ProbeValue> list = new ArrayList<ProbeValue>(1);

        // read the data
        if (memDev.read()) {
            // the relevant data will be in the values map
            try {
                int memTotal = memDev.getCurrentValue("MemTotal");
                int memFree = memDev.getCurrentValue("MemFree");
                int cached = memDev.getCurrentValue("Cached");
                int buffers = memDev.getCurrentValue("Buffers");

                int used = memTotal - memFree;
                int reallyUsed = used - (cached + buffers);

                // now collect up the results
                list.add(new DefaultProbeValue(0, (float)used));

                ProbeMeasurement m = new ProducerMeasurement(this, list, "LinuxMem");

                //System.out.println("LinuxMem => " + m);

                return m;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            System.err.println("Failed to read from /proc/stat");
            return null;
        }


    }

}

