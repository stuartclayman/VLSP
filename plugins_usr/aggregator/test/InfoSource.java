// InfoSource.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.aggregator.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.net.*;
import usr.interactor.RouterInteractor;
import plugins_usr.monitoring.distribution.USRDataPlaneProducerWithNames;
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.appl.datarate.*;
import plugins_usr.aggregator.appl.LinuxMem;
//import eu.reservoir.monitoring.appl.host.linux.MemoryInfo;
import plugins_usr.aggregator.appl.LinuxCPU;
//import eu.reservoir.monitoring.appl.host.linux.CPUInfo;
import eu.reservoir.demo.RandomProbe;
import java.util.Scanner;
import java.util.List;
import java.util.Properties;
import java.io.File;
import java.io.Serializable;
import com.timeindexing.index.*;
import com.timeindexing.time.MillisecondTimestamp;
import com.timeindexing.data.SerializableItem;

/**
 * An InfoSource that can send CPU load, memory usage,
 * or simulated response times.
 *
 * Has args:
 * output mulitcast addr
 * probe - cpu, memory, response time
 * filter - none, 5% change
 * logpath - the path to log into
 */
public class InfoSource {
    // The address for output
    Address addr = new GIDAddress(2);
    SocketAddress outputDataAddress = new SocketAddress(addr, 2299);

    // The DataSource
    InfoDataSource dataSource;

    // The Probe
    Probe probe;

    // The name
    String name = "info-source";

    // The place to store the collected data
    String collectorPath = "/tmp/";

    // sleep time for Probes
    int sleepTime = 30;

    /*
     * The Time Index that holds the sent data
     */
    IndexView dataIndex;

    /*
     * Filters
     */

    // LinuxCPU probe generates user / nice / system / idle groups

    // setup an enum for each of the Filters
    enum FilterSpecifer { Always, Percent2, Percent5, Percent10 };

    // Filter which always returns the value
    // i.e. no filtering
    ProbeFilter always = new ProbeFilter() {
        public String getName() {
            return "always";
        }

        public boolean filter(Probe p, Measurement m) {
            return true;
        }

    };

    // Filter only returns value if the 0th field value is different by 2%
    ProbeFilter filter2pcTolerance = new ProbeFilter() {
        public String getName() {
            return "field0-2%-filter";
        }

        public boolean filter(Probe p, Measurement m) {
            List<ProbeValue> list = m.getValues();
            Number n = (Number)list.get(0).getValue();

            Number oldValue = new Float(0);
            Measurement oldM;

            if ((oldM = p.getLastMeasurement()) != null) {
                oldValue = (Number)oldM.getValues().get(0).getValue();
            }

            float percent = n.floatValue() / oldValue.floatValue();

            System.out.println("ProbeFilter: " + n + "/" + oldValue + " = " +
                               percent);

            // test for 2% tolerance -  0.98 -> 1.02
            if (0.98 < percent && percent < 1.02) {
                // values too similar
                System.out.println("ProbeFilter: filtered " + n);
                return false;
            } else {
                System.out.println("ProbeFilter: reported " + n);
                return true;
            }
        }

    };

    // Filter only returns value if the 0th field value is different by 5%
    ProbeFilter filter5pcTolerance = new ProbeFilter() {
        public String getName() {
            return "field0-5%-filter";
        }

        public boolean filter(Probe p, Measurement m) {
            List<ProbeValue> list = m.getValues();
            Number n = (Number)list.get(0).getValue();

            Number oldValue = new Float(0);
            Measurement oldM;

            if ((oldM = p.getLastMeasurement()) != null) {
                oldValue = (Number)oldM.getValues().get(0).getValue();
            }

            float percent = n.floatValue() / oldValue.floatValue();

            System.out.println("ProbeFilter: " + n + "/" + oldValue + " = " +
                               percent);

            // test for 5% tolerance -  0.95 -> 1.05
            if (0.95 < percent && percent < 1.05) {
                // values too similar
                System.out.println("ProbeFilter: filtered " + n);
                return false;
            } else {
                System.out.println("ProbeFilter: reported " + n);
                return true;
            }
        }

    };

    // Filter only returns value if the 0th field value is different by 10%
    ProbeFilter filter10pcTolerance = new ProbeFilter() {
        public String getName() {
            return "field0-10%-filter";
        }

        public boolean filter(Probe p, Measurement m) {
            List<ProbeValue> list = m.getValues();
            Number n = (Number)list.get(0).getValue();

            Number oldValue = new Float(0);
            Measurement oldM;

            if ((oldM = p.getLastMeasurement()) != null) {
                oldValue = (Number)oldM.getValues().get(0).getValue();
            }

            float percent = n.floatValue() / oldValue.floatValue();

            System.out.println("ProbeFilter: " + n + "/" + oldValue + " = " +
                               percent);

            // test for 10% tolerance -  0.90 -> 1.10
            if (0.90 < percent && percent < 1.10) {
                // values too similar
                System.out.println("ProbeFilter: filtered " + n);
                return false;
            } else {
                System.out.println("ProbeFilter: reported " + n);
                return true;
            }
        }

    };

    // no default filter
    ProbeFilter actualFilter = null;



    /**
     * InfoSource constructor.
     */
    public InfoSource() {
    }

    public void start() {
        try {
            // create a TimeIndexFactory
            TimeIndexFactory factory = new TimeIndexFactory();
            Properties indexProperties = new Properties();

            String realName;

            // create forwardIndex
            realName = name+"-log";
            File dataIndexPath = new File(collectorPath, realName);
            indexProperties.setProperty("indexpath", dataIndexPath.getPath());
            indexProperties.setProperty("name", realName);

            dataIndex = factory.create(IndexType.EXTERNAL, indexProperties);
        } catch (TimeIndexException tie) {
            tie.printStackTrace();
            throw new RuntimeException("Cannot create TimeIndex ");
        }

        // check actual probe
        if (probe == null) {
            throw new Error("No Probe has been set");
        }

        System.err.println("InfoSource connect to " + outputDataAddress);

        DataPlane outputDataPlane = new USRDataPlaneProducerWithNames(outputDataAddress);

        dataSource = new InfoDataSource(dataIndex);
        dataSource.setName(name);

        // set up DataPlane
        dataSource.setDataPlane(outputDataPlane);

        // and connect
        dataSource.connect();

        // set up probe
        probe.setDataRate(new EveryNSeconds(sleepTime));

        if (actualFilter != null) {
            // turn on filter
            probe.setProbeFilter(actualFilter);
            probe.turnOnFiltering();
        }


        // turn on probe
        dataSource.addProbe(probe);
        dataSource.turnOnProbe(probe);
    }

    /**
     * Get the address for output traffic.
     */
    public SocketAddress getOutputAddress() {
        return outputDataAddress;
    }

    /**
     * Set the multicast address for output traffic.
     */
    public SocketAddress setOutputAddress(SocketAddress out) {
        SocketAddress old = outputDataAddress;
        outputDataAddress = out;
        return old;
    }

    /**
     * Get the path where raw data is collected into.
     */
    public String getCollectionPath() {
        return collectorPath;
    }

    /**
     * Set the path where raw data is collected into.
     */
    public String setCollectionPath(String path) {
        String old = collectorPath;
        collectorPath = path;
        return old;
    }

    /**
     * Get the sleep time between reads.
     */
    public int getSleepTime() {
        return sleepTime;
    }

    /**
     * Set the sleep time between reads.
     */
    public int setSleepTime(int slTime) {
        int oldSleepTime = sleepTime;
        sleepTime = slTime;

        return oldSleepTime;
    }

    /**
     * Get the name of the AggPoint.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the AggPoint.
     */
    public String setName(String str) {
        String old = name;
        name = str;
        return old;
    }

    /*
     * Setup the probe to use.
     */

    // an enum for the probe
    enum ProbeSpecifier { CPU, Memory, ResponseTime };

    /**
     * Set the probe.
     */
    public void setProbe(ProbeSpecifier spec) {
        switch (spec) {
        case CPU:
            probe = new LinuxCPU(name + ".cpu-info");
            break;

        case Memory:
            probe = new LinuxMem(name + ".memory-info");
            break;

        case ResponseTime:
            // probe name is elapsedTime
            // field is called time
            // response times are around 30 secs +/- a bit
            probe = new RandomProbe(name + ".elapsedTime", "time", 30);
            break;
        }
    }

    /**
     * Get the current Filter
     */
    public ProbeFilter getFilter() {
        return actualFilter;
    }

    /**
     * Set the Filter
     */
    public ProbeFilter setFilter(FilterSpecifer spec) {
        ProbeFilter old = actualFilter;

        switch (spec) {
        case Always:
            actualFilter = always;
            break;

        case Percent2:
            actualFilter = filter2pcTolerance;
            break;

        case Percent5:
            actualFilter = filter5pcTolerance;
            break;

        case Percent10:
            actualFilter = filter10pcTolerance;
            break;

        }

        return old;
    }

    /**
     * Main entry point.
     * Args are:
     * -o output address (default: @(2)/2299)
     * -p probe, [cpu, memory, responsetime]  (NO default)
     * -f filter, [always, 2%, 5%, 10%]  (default: always)
     * -l log path, (default: /tmp/)
     * -t sleep timeout (default: 30)
     * -n name (default: "info-source")
     */
    public static void main(String[] args) {
        // the host that has the Router at addr
        String remHost = "localhost";
        int remPort = 19191;

        // Set up Router
        // And connect to Router @(2)
        try {
            int port = 18181;
            int r2r = 18182;

            RouterEnv routerEnv = new RouterEnv(port, r2r, "Router-1");
            Router router = routerEnv.getRouter();

            // check
            if (routerEnv.isActive()) {
            } else {
                throw new Exception("Router failed to start");
            }

            // set up id
            router.setAddress(new GIDAddress(1));

            // connnect to the other router
            // first we tal kto my own ManagementConsole
            RouterInteractor selfInteractor = new RouterInteractor("localhost", port);

            // then set up Router-to-Router data connection
            selfInteractor.createConnection(remHost + ":" + remPort, 20);

            // and stop talking to the ManagementConsole
            selfInteractor.quit();


        } catch (Exception e) {
            System.err.println("Cannot interact with router at " + remHost + ":" + remPort);
            System.exit(1);
        }

        // allocate an InfoSource
        InfoSource infoSource = new InfoSource();

        // process args
        int argc = args.length;

        for (int arg = 0; arg < argc; arg++) {
            String thisArg = args[arg];

            // check if its a flag
            if (thisArg.charAt(0) == '-') {
                // get option
                char option = thisArg.charAt(1);

                // gwet next arg
                String argValue = args[++arg];

                switch (option) {

                case 'o': {
                    String[] parts = argValue.split("/");
                    Scanner sc = new Scanner(parts[0]);
                    int addr = sc.nextInt();
                    sc.close();
                    sc = new Scanner(parts[1]);
                    int port = sc.nextInt();
                    sc.close();
                    Address gidAddr = new GIDAddress(addr);
                    SocketAddress newOutputAddr = new SocketAddress(gidAddr, port);
                    infoSource.setOutputAddress(newOutputAddr);
                    break;
                }

                case 'p': {
                    if (argValue.equals("cpu")) {
                        infoSource.setProbe(ProbeSpecifier.CPU);
                    } else if (argValue.equals("memory")) {
                        infoSource.setProbe(ProbeSpecifier.Memory);
                    } else if (argValue.equals("rt")) {
                        infoSource.setProbe(ProbeSpecifier.ResponseTime);
                    } else {
                        System.err.println("InfoSource: unknown probe " + argValue);
                    }
                    break;
                }


                case 'f': {
                    if (argValue.equals("always")) {
                        infoSource.setFilter(FilterSpecifer.Always);
                    } else if (argValue.equals("2%")) {
                        infoSource.setFilter(FilterSpecifer.Percent2);
                    } else if (argValue.equals("5%")) {
                        infoSource.setFilter(FilterSpecifer.Percent5);
                    } else if (argValue.equals("10%")) {
                        infoSource.setFilter(FilterSpecifer.Percent10);
                    } else {
                        System.err.println("InfoSource: unknown filter " + argValue);
                    }
                    break;
                }

                case 'l': {
                    // assume a file name
                    File potentialPath = new File(argValue);

                    // check if directory part exists
                    if (potentialPath.isDirectory() && potentialPath.canWrite()) {
                        infoSource.setCollectionPath(argValue);
                    } else {
                        System.err.println("InfoSource: cannot write file in directory " + argValue);
                        System.exit(1);
                    }
                    break;
                }

                case 't': {
                    Scanner sc = new Scanner(argValue);
                    int t = sc.nextInt();
                    sc.close();
                    infoSource.setSleepTime(t);
                    break;
                }


                case 'n': {
                    infoSource.setName(argValue);
                    break;
                }


                default:
                    System.err.println("InfoSource: unknown option " + option);
                    break;
                }

            }
        }

        // start the info source
        infoSource.start();

    }

    /**
     * Simple Data Source
     */
    class InfoDataSource extends AbstractDataSource {
        IndexView dataIndex;

        public InfoDataSource(IndexView dataIndex) {
            this.dataIndex = dataIndex;
        }

        /**
         * Recieve a measurment from the Probe
         * and pass it onto the data source delegate.
         * @return null if something goes wrong
         */
        public int notifyMeasurement(Measurement m) {
            // do usual notifyMeasurement
            int result = super.notifyMeasurement(m);

            // now log the measurement in a time index
            try {
                // We receive the Measurement as a ProducerMeasurement
                // but it is better to store a ConsumerMeasurement
                // as it deserializes better
                ConsumerMeasurement cm = new ConsumerMeasurement(m.getSequenceNo(),
                                                                 m.getProbeID(),
                                                                 m.getType(),
                                                                 m.getTimestamp().value(),
                                                                 m.getDeltaTime().value(),
                                                                 m.getServiceID(),
                                                                 m.getGroupID(),
                                                                 m.getValues());

                Serializable object = (Serializable)cm;
                dataIndex.addItem(new SerializableItem(object), new MillisecondTimestamp());
                return result;
            } catch (TimeIndexException tie) {
                System.err.println("Can't add data to time index log " + dataIndex.getName());
                return result;
            }
        }

    }
}