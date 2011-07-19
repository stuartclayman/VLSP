// LinuxCPU.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Mar 2011

package plugins_usr.aggregator.appl;

import eu.reservoir.monitoring.core.*;
import java.util.*;
import eu.reservoir.monitoring.appl.host.linux.CPUDev;

/**
 * A probe to get cpu info on a Linux system.
 * It uses /proc/stat to read the underyling data.
 */
public class LinuxCPU extends AbstractProbe implements Probe  {
    // A CPUDev object that reads info about the CPU.
    CPUDev cpuDev;


    /*
     * Construct a LinuxCPU probe
     */
    public LinuxCPU(String name) {
        setName(name);
        setDataRate(new Rational(360, 1));

        // allocate cpuDev
        cpuDev = new CPUDev();

        // read data, but calculate nothing
        cpuDev.read(false);


        // add a probe attribute
        addProbeAttribute(new DefaultProbeAttribute(0, "usage", ProbeAttributeType.FLOAT, "percent"));
    }


    /**
     * Collect a measurement.
     */
    public ProbeMeasurement collect() {
        // create a list for the result
        ArrayList<ProbeValue> list = new ArrayList<ProbeValue>(1);

        // read the data
        if (cpuDev.read(true)) {
            try {
                int cpuCount = cpuDev.getCPUCount();
                float idleTotal = 0.0f;

                // get amount for idleness
                for (int c=0; c<cpuCount; c++) {
                    float f = (float)cpuDev.getDeltaValue("cpu" + c  + "-idle");
                    System.out.print(c + "=" + f + ", ");
                    idleTotal += f;
                }
                System.out.println();

                // we now have total idleness, over N cpus.
                // But, usage = 100% - idleness

                float usage = 100 - (idleTotal / cpuCount);

                if (usage < 0) {
                    usage = 0;
                }


                list.add(new DefaultProbeValue(0, usage));


                ProbeMeasurement m = new ProducerMeasurement(this, list, "LinuxCPU");

                //System.out.println("LinuxCPU => " + m);

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

