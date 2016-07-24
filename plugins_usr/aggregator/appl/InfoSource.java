// InfoSource.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.aggregator.appl;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import plugins_usr.monitoring.distribution.USRDataPlaneProducerWithNames;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.GIDAddress;
import usr.net.SocketAddress;

import com.timeindexing.data.SerializableItem;
import com.timeindexing.index.IndexType;
import com.timeindexing.index.IndexView;
import com.timeindexing.index.TimeIndexException;
import com.timeindexing.index.IndexCloseException;
import com.timeindexing.index.TimeIndexFactory;
import com.timeindexing.time.MillisecondTimestamp;

import eu.reservoir.demo.RandomProbe;
import eu.reservoir.monitoring.appl.datarate.EveryNSeconds;
import eu.reservoir.monitoring.core.AbstractDataSource;
import eu.reservoir.monitoring.core.ConsumerMeasurement;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.Probe;
import eu.reservoir.monitoring.core.ProbeFilter;
import eu.reservoir.monitoring.core.ProbeValue;
import eu.reservoir.monitoring.core.plane.DataPlane;

/**
 * An InfoSource that can send CPU load, memory usage, router traffic,
 * or simulated response times.
 *
 * Has args:
 * output addr
 * probe - cpu, memory, traffic, response time
 * filter - none, 5% change
 * logpath - the path to log into
 * initial delay - seconds
 */
public class InfoSource implements Application
{
    // The address for output
    Address addr;
    SocketAddress outputDataAddress;

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

    // initial delay
    int initialDelay = 0;

    boolean inInitialDelay = false;
    Thread myThread;

    Object threadSyncObj = new Object();
    boolean closing_ = false;

    /*
     * The Time Index that holds the sent data
     */
    IndexView dataIndex;
    File dataIndexPath = null;

    /*
     * Filters
     */

    // LinuxCPU probe generates user / nice / system / idle groups

    // setup an enum for each of the Filters
    enum FilterSpecifer { Always, Percent2, Percent5, Percent10 };

    // Filter which always returns the value
    // i.e. no filtering
    ProbeFilter always = new ProbeFilter()
        {
            @Override
            public String getName(){
                return "always";
            }

            @Override
            public boolean filter(Probe p, Measurement m){
                return true;
            }
        };

    // Filter only returns value if the 0th field value is different by 2%
    ProbeFilter filter2pcTolerance = new ProbeFilter()
        {
            @Override
            public String getName(){
                return "field0-2%-filter";
            }

            @Override
            public boolean filter(Probe p, Measurement m){
                List<ProbeValue> list = m.getValues();
                Number n = (Number)list.get(0).getValue();

                Number oldValue = new Float(0);
                Measurement oldM;
                if ((oldM = p.getLastMeasurement()) != null)
                    oldValue = (Number)oldM.getValues().get(0).getValue();
                if (oldValue.floatValue() == 0.0)
                    return n.floatValue() != 0.0;
                float percent = n.floatValue() / oldValue.floatValue();

                //Logger.getLogger("log").logln(USR.STDOUT, "ProbeFilter: "
                // + n
                // + "/" + oldValue + " = " +
                //		   percent);

                // test for 2% tolerance -  0.98 -> 1.02
                if (0.98 < percent && percent < 1.02) {
                    // values too similar
                    //Logger.getLogger("log").logln(USR.STDOUT,
                    // "ProbeFilter:
                    // filtered " + n);
                    return false;
                } else {
                    // Logger.getLogger("log").logln(USR.STDOUT,
                    // "ProbeFilter:
                    // reported " + n);
                    return true;
                }
            }
        };

    // Filter only returns value if the 0th field value is different by 5%
    ProbeFilter filter5pcTolerance = new ProbeFilter()
        {
            @Override
            public String getName(){
                return "field0-5%-filter";
            }

            @Override
            public boolean filter(Probe p, Measurement m){
                List<ProbeValue> list = m.getValues();
                Number n = (Number)list.get(0).getValue();

                Number oldValue = new Float(0);
                Measurement oldM;
                if ((oldM = p.getLastMeasurement()) != null)
                    oldValue = (Number)oldM.getValues().get(0).getValue();
                if (oldValue.floatValue() == 0.0)
                    return n.floatValue() != 0.0;
                float percent = n.floatValue() / oldValue.floatValue();

                Logger.getLogger("log").logln(
                                              USR.STDOUT, "ProbeFilter: " + n + "/" + oldValue +
                                              " = " +
                                              percent);

                // test for 5% tolerance -  0.95 -> 1.05
                if (0.95 < percent && percent < 1.05) {
                    // values too similar
                    Logger.getLogger("log").logln(USR.STDOUT,
                                                  "ProbeFilter: filtered " + n);
                    return false;
                } else {
                    Logger.getLogger("log").logln(USR.STDOUT,
                                                  "ProbeFilter: reported " + n);
                    return true;
                }
            }
        };

    // Filter only returns value if the 0th field value is different by 10%
    ProbeFilter filter10pcTolerance = new ProbeFilter()
        {
            @Override
            public String getName(){
                return "field0-10%-filter";
            }

            @Override
            public boolean filter(Probe p, Measurement m){
                List<ProbeValue> list = m.getValues();
                Number n = (Number)list.get(0).getValue();

                Number oldValue = new Float(0);
                Measurement oldM;
                if ((oldM = p.getLastMeasurement()) != null)
                    oldValue = (Number)oldM.getValues().get(0).getValue();
                if (oldValue.floatValue() == 0.0)
                    return n.floatValue() != 0.0;

                float percent = n.floatValue() / oldValue.floatValue();

                //	Logger.getLogger("log").logln(USR.STDOUT, "ProbeFilter:
                // " + n + "/" + oldValue + " = " +
                //			   percent);

                // test for 10% tolerance -  0.90 -> 1.10
                if (0.90 < percent && percent < 1.10) {
                    // values too similar
                    //    Logger.getLogger("log").logln(USR.STDOUT,
                    // "ProbeFilter: filtered " + n);
                    return false;
                } else {
                    //    Logger.getLogger("log").logln(USR.STDOUT,
                    // "ProbeFilter: reported " + n);
                    return true;
                }
            }
        };

    // no default filter
    ProbeFilter actualFilter = null;

    /**
     * InfoSource constructor.
     */
    public InfoSource(){
    }

    /**
     * init
     * Args are:
     * -o output address
     * -p probe, [cpu, memory, traffic, rt]  (NO default)
     * -f filter, [always, 2%, 5%, 10%]  (default: always)
     * -l log path, (default: /tmp/)
     * -t sleep timeout (default: 30)
     * -d initial delay (default: 0)
     * -n name (default: "info-source")
     */
    @Override
    public ApplicationResponse init(String[] args){
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
                    SocketAddress newOutputAddr = new SocketAddress(
                                                                    gidAddr,
                                                                    port);
                    setOutputAddress(newOutputAddr);
                    break;
                }

                case 'p': {
                    if (argValue.equals("cpu")) {
                        setProbe(ProbeSpecifier.CPU);
                    } else if (argValue.equals("memory")) {
                        setProbe(ProbeSpecifier.Memory);
                    } else if (argValue.equals("traffic")) {
                        setProbe(ProbeSpecifier.Traffic);
                    } else if (argValue.equals("rt")) {
                        setProbe(ProbeSpecifier.ResponseTime);
                    } else {
                        Logger.getLogger("log").logln(
                                                      USR.ERROR,
                                                      "InfoSource: unknown probe " +
                                                      argValue);
                    }
                    break;
                }

                case 'f': {
                    if (argValue.equals("always")) {
                        setFilter(FilterSpecifer.Always);
                    } else if (argValue.equals("2%")) {
                        setFilter(FilterSpecifer.Percent2);
                    } else if (argValue.equals("5%")) {
                        setFilter(FilterSpecifer.Percent5);
                    } else if (argValue.equals("10%")) {
                        setFilter(FilterSpecifer.Percent10);
                    } else {
                        Logger.getLogger("log").logln(
                                                      USR.ERROR,
                                                      "InfoSource: unknown filter " +
                                                      argValue);
                    }
                    break;
                }

                case 'l': {
                    // assume a file name
                    File potentialPath = new File(argValue);
                    // check if directory part exists
                    if (potentialPath.isDirectory() &&
                        potentialPath.canWrite()) {
                        setCollectionPath(argValue);
                    } else {
                        Logger.getLogger("log").logln(
                                                      USR.ERROR,
                                                      "InfoSource: cannot write file in directory "
                                                      + argValue);
                        System.exit(1);
                    }
                    break;
                }

                case 't': {
                    Scanner sc = new Scanner(argValue);
                    int t = sc.nextInt();
                    sc.close();
                    setSleepTime(t);
                    break;
                }

                case 'd': {
                    Scanner scd = new Scanner(argValue);
                    int t = scd.nextInt();
                    scd.close();
                    setInitialDelay(t);
                    break;
                }

                case 'n': {
                    setName(argValue);
                    break;
                }

                default:
                    Logger.getLogger("log").logln(
                                                  USR.ERROR, "InfoSource: unknown option " +
                                                  option);
                    break;
                }
            }
        }

        // check actual probe
        if (probe == null)
            return new ApplicationResponse(false,
                                           "No Probe has been set");

        // check outputDataAddress
        if (outputDataAddress == null)
            return new ApplicationResponse(false,
                                           "No Output Address has been set");

        return new ApplicationResponse(true, "");
    }

    /**
     * Start
     */
    @Override
    public ApplicationResponse start(){
        //Logger.getLogger("log").logln(USR.STDOUT, "InfoSource: top of
        // start");
        try {
            // create a TimeIndexFactory
            TimeIndexFactory factory = new TimeIndexFactory();
            Properties indexProperties = new Properties();

            String realName;

            // create forwardIndex
            realName = name + "-log";
            dataIndexPath = new File(collectorPath, realName);
            indexProperties.setProperty("indexpath",
                                        dataIndexPath.getPath());

            // try and append to an existing index
            dataIndex = factory.append(indexProperties);

            // if it does not exist
            if (dataIndex == null) {
                Logger.getLogger("log").logln(
                                              USR.STDOUT,
                                              "InfoSource: about to create index: " +
                                              realName);

                // create it
                indexProperties.setProperty("name", realName);

                dataIndex = factory.create(IndexType.EXTERNAL,
                                           indexProperties);

                Logger.getLogger("log").logln(
                                              USR.STDOUT, "InfoSource: created index: " +
                                              realName);
            } else {
                dataIndex.activate();
                Logger.getLogger("log").logln(
                                              USR.STDOUT, "InfoSource: appending to: " +
                                              realName);
            }

            dataIndex.setAutoCommit(true);
        } catch (TimeIndexException tie) {
            Logger.getLogger("log").logln(
                                          USR.STDOUT,
                                          "InfoSource: TimeIndex setup failed in start");
            tie.printStackTrace();

            // got an Exception so try ans close the index.
            try {
                dataIndex.close();
            } catch (IndexCloseException ice) {
                ice.printStackTrace();
            }

            return new ApplicationResponse(
                                           false, "Cannot create TimeIndex " + dataIndexPath);
        }

        //Logger.getLogger("log").logln(USR.STDOUT, "InfoSource:
        // TimeIndex
        // setup in start");

        try {
            Logger.getLogger("log").logln(
                                          USR.STDOUT, "InfoSource connect to " +
                                          outputDataAddress);

            //Logger.getLogger("log").logln(USR.STDOUT, "InfoSource:
            // about
            // to setup data source: " +  name);
            DataPlane outputDataPlane =
                new USRDataPlaneProducerWithNames(
                                                  outputDataAddress);

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
            dataSource.addProbe(probe);             // this does
            // registerProbe and
            // activateProbe
            Logger.getLogger("log").logln(
                                          USR.STDOUT,
                                          "InfoSource: setup data source: " + name);

            return new ApplicationResponse(true, "");
        } catch (Exception e) {
            Logger.getLogger("log").logln(
                                          USR.STDOUT,
                                          "InfoSource: data source setup failed in start");
            //e.printStackTrace();
            return new ApplicationResponse(false, e.getMessage());
        }
    }

    /**
     * Stop
     */
    @Override
    public ApplicationResponse stop(){
        synchronized (threadSyncObj) {
            try {
                if (closing_) {
                    Logger.getLogger("log").logln(
                                                  USR.STDOUT, "InfoSource: stop already called");
                    return new ApplicationResponse(
                                                   false, "Stop already called for InfoSource");
                }

                closing_ = true;

                // we might stop while the Application is in the
                // inInitialDelay
                // stage
                if (inInitialDelay) {
                    // so interrupt the sleep
                    Logger.getLogger("log").logln(
                                                  USR.STDOUT,
                                                  "InfoSource: about to interrupt initial delay");
                    myThread.interrupt();
                }

                if (dataSource.isProbeOn(probe))
                    dataSource.turnOffProbe(probe);

                dataSource.removeProbe(probe);

                dataSource.disconnect();

                try {
                    dataIndex.close();

                    Logger.getLogger("log").logln(
                                                  USR.STDOUT,
                                                  "InfoSource: TimeIndex closed in stop");
                    return new ApplicationResponse(true, "");
                } catch (TimeIndexException tie) {
                    Logger.getLogger("log").logln(
                                                  USR.STDOUT, "Cannot close index " + dataIndex +
                                                  " because " + tie.getMessage());
                    //tie.printStackTrace();

                    return new ApplicationResponse(
                                                   false,
                                                   "Cannot close TimeIndex " + dataIndexPath);
                }
                
            } finally {
                //synchronized (this) {
                threadSyncObj.notifyAll();
                //}

            }
        }
    }

    /**
     * Run
     */
    @Override
    public void run(){
        // we might stop while the Application is in the
        // initial delay stage, so we have to label this situation
        myThread = Thread.currentThread();
        inInitialDelay = true;

        if (closing_) {
            // stop was called before we got here
            Logger.getLogger("log").logln(
                                          USR.STDOUT,
                                          "InfoSource: stop called at top of run()");
            return;
        }

        try {
            //Logger.getLogger("log").logln(USR.STDOUT, "SLEEP  " +
            // getInitialDelay() + " seconds");
            Thread.sleep(getInitialDelay() * 1000);
        } catch (InterruptedException ie) {
            Logger.getLogger("log").logln(
                                          USR.STDOUT, "InfoSource: initial delay interrupted");
            Logger.getLogger("log").logln(USR.STDOUT,
                                          "InfoSource: End of run()");
            return;
        }

        inInitialDelay = false;

        if (closing_) {
            // stop was called before we got here
            Logger.getLogger("log").logln(
                                          USR.STDOUT,
                                          "InfoSource: stop called after sleeping in run()");
            return;
        }

        Logger.getLogger("log").logln(
                                      USR.STDOUT,
                                      "InfoSource: turn on Probe: " + probe.getName());

        dataSource.turnOnProbe(probe);

        // A DataSource already runs in itws own thread
        // so this one can wait and do nothing.
        if (!closing_) {
            try {
                synchronized (threadSyncObj) {
                    threadSyncObj.wait();
                }
            } catch (InterruptedException ie) {
            }
        }

        Logger.getLogger("log").logln(USR.STDOUT,
                                      "InfoSource: End of run()");
    }

    /**
     * Get the address for output traffic.
     */
    public SocketAddress getOutputAddress(){
        return outputDataAddress;
    }

    /**
     * Set the multicast address for output traffic.
     */
    public SocketAddress setOutputAddress(SocketAddress out){
        SocketAddress old = outputDataAddress;

        outputDataAddress = out;
        return old;
    }

    /**
     * Get the path where raw data is collected into.
     */
    public String getCollectionPath(){
        return collectorPath;
    }

    /**
     * Set the path where raw data is collected into.
     */
    public String setCollectionPath(String path){
        String old = collectorPath;

        collectorPath = path;
        return old;
    }

    /**
     * Get the sleep time between reads.
     */
    public int getSleepTime(){
        return sleepTime;
    }

    /**
     * Set the sleep time between reads.
     */
    public int setSleepTime(int slTime){
        int oldSleepTime = sleepTime;

        sleepTime = slTime;

        return oldSleepTime;
    }

    /**
     * Get the initial delay
     */
    public int getInitialDelay(){
        return initialDelay;
    }

    /**
     * Set the initialDelay
     */
    public int setInitialDelay(int delay){
        int oldInitialDelay = initialDelay;

        initialDelay = delay;

        return oldInitialDelay;
    }

    /**
     * Get the name of the AggPoint.
     */
    public String getName(){
        return name;
    }

    /**
     * Set the name of the AggPoint.
     */
    public String setName(String str){
        String old = name;

        name = str;
        return old;
    }

    /*
     * Setup the probe to use.
     */

    // an enum for the probe
    enum ProbeSpecifier { CPU, Memory, Traffic, ResponseTime };

    /**
     * Set the probe.
     */
    public void setProbe(ProbeSpecifier spec){
        switch (spec) {
        case CPU:
            probe = new LinuxCPU(name + "cpu-info");
            break;

        case Memory:
            probe = new LinuxMem(name + "memory-info");
            break;

        case Traffic:
            probe = new RouterTrafficProbe(name + "traffic-info");
            break;

        case ResponseTime:
            // probe name is elapsedTime
            // field is called time
            // response times are around 30 secs +/- a bit
            probe = new RandomProbe(name + "elapsedTime", "time", 30);
            break;
        }
    }

    /**
     * Get the current Filter
     */
    public ProbeFilter getFilter(){
        return actualFilter;
    }

    /**
     * Set the Filter
     */
    public ProbeFilter setFilter(FilterSpecifer spec){
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
     * Simple Data Source
     */
    class InfoDataSource extends AbstractDataSource
    {
        IndexView dataIndex;

        public InfoDataSource(IndexView dataIndex){
            this.dataIndex = dataIndex;
        }

        /**
         * Recieve a measurment from the Probe
         * and pass it onto the data source delegate.
         * @return null if something goes wrong
         */
        @Override
        public int notifyMeasurement(Measurement m){
            // do usual notifyMeasurement
            int result = super.notifyMeasurement(m);

            // now log the measurement in a time index
            try {
                // We receive the Measurement as a ProducerMeasurement
                // but it is better to store a ConsumerMeasurement
                // as it deserializes better
                ConsumerMeasurement cm = new ConsumerMeasurement(
                                                                 m.getSequenceNo(),
                                                                 m.getProbeID(),
                                                                 m.getType(),
                                                                 m.getTimestamp(
                ).value(),
                                                                 m.getDeltaTime(
                ).value(),
                                                                 m.getServiceID(),
                                                                 m.getGroupID(),
                                                                 m.getValues());

                Serializable object = cm;

                if (!dataIndex.isClosed() && dataIndex.isActivated()) {
                    dataIndex.addItem(new SerializableItem(
                                                           object), new MillisecondTimestamp());
                } else {
                    Logger.getLogger("log").logln(
                                                  USR.ERROR,
                                                  "Can't add data to time index log " +
                                                  dataIndex.getName() +
                                                  " because it is closed");
                }

                return result;
            } catch (TimeIndexException tie) {
                Logger.getLogger("log").logln(
                                              USR.ERROR,
                                              "Can't add data to time index log " +
                                              dataIndex.getName() +
                                              " because of exception " +
                                              tie.getMessage());

                return result;
            }
        }
    }
}
