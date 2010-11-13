// InfoSource.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.aggregator.appl;

import usr.applications.*;
import usr.router.Router;
import usr.net.*;
import usr.logging.*;
import usr.interactor.RouterInteractor;
import plugins_usr.monitoring.distribution.USRDataPlaneProducerWithNames;
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.distribution.*;
import eu.reservoir.monitoring.appl.datarate.*;
import eu.reservoir.monitoring.appl.host.linux.MemoryInfo;
import eu.reservoir.monitoring.appl.host.linux.CPUInfo;
import eu.reservoir.demo.RandomProbe;
import java.net.InetAddress;
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
 * output addr
 * probe - cpu, memory, response time
 * filter - none, 5% change
 * logpath - the path to log into
 * initial delay - seconds
 */
public class InfoSource implements Application {
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
    boolean closing_= false;

    /*
     * The Time Index that holds the sent data
     */
    IndexView dataIndex;
    File dataIndexPath = null;


    /*
     * Filters
     */

    // CPUInfo probe generates user / nice / system / idle groups

    // setup an enum for each of the Filters
    enum FilterSpecifer { Always, Percent2, Percent5, Percent10 };

    // Filter which always returns the value
    // i.e. no filtering
    ProbeFilter always = new ProbeFilter() {
	    public String getName() { return "always"; }

	    public boolean filter(Probe p, Measurement m) {
		return true;
	    }
	};

    // Filter only returns value if the 0th field value is different by 2%
    ProbeFilter filter2pcTolerance = new ProbeFilter() {
	    public String getName() { return "field0-2%-filter"; }

	    public boolean filter(Probe p, Measurement m) {
		List<ProbeValue> list = m.getValues();
		Number n =  (Number)list.get(0).getValue();

		Number oldValue = new Float(0);
		Measurement oldM;
		if ((oldM = p.getLastMeasurement()) != null) {
		    oldValue = (Number)oldM.getValues().get(0).getValue();
		}

		float percent = n.floatValue() / oldValue.floatValue();

		Logger.getLogger("log").logln(USR.STDOUT, "ProbeFilter: " + n + "/" + oldValue + " = " +
				   percent);

		// test for 2% tolerance -  0.98 -> 1.02
		if (0.98 < percent && percent < 1.02) {
		    // values too similar
		    Logger.getLogger("log").logln(USR.STDOUT, "ProbeFilter: filtered " + n);
		    return false;
		} else {
		    Logger.getLogger("log").logln(USR.STDOUT, "ProbeFilter: reported " + n);
		    return true;
		}
	    }
	};

    // Filter only returns value if the 0th field value is different by 5%
    ProbeFilter filter5pcTolerance = new ProbeFilter() {
	    public String getName() { return "field0-5%-filter"; }

	    public boolean filter(Probe p, Measurement m) {
		List<ProbeValue> list = m.getValues();
		Number n =  (Number)list.get(0).getValue();

		Number oldValue = new Float(0);
		Measurement oldM;
		if ((oldM = p.getLastMeasurement()) != null) {
		    oldValue = (Number)oldM.getValues().get(0).getValue();
		}

		float percent = n.floatValue() / oldValue.floatValue();

		Logger.getLogger("log").logln(USR.STDOUT, "ProbeFilter: " + n + "/" + oldValue + " = " +
				   percent);

		// test for 5% tolerance -  0.95 -> 1.05
		if (0.95 < percent && percent < 1.05) {
		    // values too similar
		    Logger.getLogger("log").logln(USR.STDOUT, "ProbeFilter: filtered " + n);
		    return false;
		} else {
		    Logger.getLogger("log").logln(USR.STDOUT, "ProbeFilter: reported " + n);
		    return true;
		}
	    }
	};

    // Filter only returns value if the 0th field value is different by 10%
    ProbeFilter filter10pcTolerance = new ProbeFilter() {
	    public String getName() { return "field0-10%-filter"; }

	    public boolean filter(Probe p, Measurement m) {
		List<ProbeValue> list = m.getValues();
		Number n =  (Number)list.get(0).getValue();

		Number oldValue = new Float(0);
		Measurement oldM;
		if ((oldM = p.getLastMeasurement()) != null) {
		    oldValue = (Number)oldM.getValues().get(0).getValue();
		}

		float percent = n.floatValue() / oldValue.floatValue();

		Logger.getLogger("log").logln(USR.STDOUT, "ProbeFilter: " + n + "/" + oldValue + " = " +
				   percent);

		// test for 10% tolerance -  0.90 -> 1.10
		if (0.90 < percent && percent < 1.10) {
		    // values too similar
		    Logger.getLogger("log").logln(USR.STDOUT, "ProbeFilter: filtered " + n);
		    return false;
		} else {
		    Logger.getLogger("log").logln(USR.STDOUT, "ProbeFilter: reported " + n);
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


    /**
     * init
     * Args are:
     * -o output address
     * -p probe, [cpu, memory, rt]  (NO default)
     * -f filter, [always, 2%, 5%, 10%]  (default: always)
     * -l log path, (default: /tmp/)
     * -t sleep timeout (default: 30)
     * -d initial delay (default: 0)
     * -n name (default: "info-source")
     */
    public ApplicationResponse init(String[] args) {
	// process args
	int argc = args.length;

	for (int arg=0; arg < argc; arg++) {
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
                    sc = new Scanner(parts[1]);
		    int port = sc.nextInt();
                    Address gidAddr = new GIDAddress(addr);
		    SocketAddress newOutputAddr = new SocketAddress(gidAddr, port);
		    setOutputAddress(newOutputAddr);
		    break;
		}

		case 'p': {
		    if (argValue.equals("cpu")) {
			setProbe(ProbeSpecifier.CPU);
		    } else if (argValue.equals("memory")) {
			setProbe(ProbeSpecifier.Memory);
		    } else if (argValue.equals("rt")) {
			setProbe(ProbeSpecifier.ResponseTime);
		    } else {
			Logger.getLogger("log").logln(USR.ERROR, "InfoSource: unknown probe " + argValue);
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
			Logger.getLogger("log").logln(USR.ERROR, "InfoSource: unknown filter " + argValue);
		    }
		    break;
		}

		case 'l': {
		    // assume a file name
		    File potentialPath = new File(argValue);
		    // check if directory part exists
		    if (potentialPath.isDirectory() && potentialPath.canWrite()) {
			setCollectionPath(argValue);
		    } else {
			Logger.getLogger("log").logln(USR.ERROR, "InfoSource: cannot write file in directory " + argValue);
			System.exit(1);
		    }
		    break;
		}

		case 't': {
		    Scanner sc = new Scanner(argValue);
		    int t = sc.nextInt();
		    setSleepTime(t);
		    break;
		}


		case 'd': {
		    Scanner scd = new Scanner(argValue);
		    int t = scd.nextInt();
		    setInitialDelay(t);
		    break;
		}


		case 'n': {
		    setName(argValue);
		    break;
		}


		default:
		    Logger.getLogger("log").logln(USR.ERROR, "InfoSource: unknown option " + option);
		    break;
		}
		
	    }
	}

        // check actual probe
        if (probe == null) {
            return new ApplicationResponse(false, "No Probe has been set");
        }

        // check outputDataAddress
        if (outputDataAddress == null) {
	    return new ApplicationResponse(false, "No Output Address has been set");
        }


        return new ApplicationResponse(true, "");
    }

    /**
     * Start
     */
    public ApplicationResponse start() {
	try {
	    // create a TimeIndexFactory
	    TimeIndexFactory factory = new TimeIndexFactory();
	    Properties indexProperties = new Properties();

	    String realName;

	    // create forwardIndex
	    realName = name+"-log";
            dataIndexPath = new File(collectorPath, realName);
	    indexProperties.setProperty("indexpath",  dataIndexPath.getPath());
	    indexProperties.setProperty("name", realName);

	    dataIndex = factory.create(IndexType.EXTERNAL, indexProperties);
            dataIndex.setAutoCommit(true);

	} catch (TimeIndexException tie) {
	    tie.printStackTrace();
	    return new ApplicationResponse(false, "Cannot create TimeIndex " + dataIndexPath) ;
	}

        try {
            Logger.getLogger("log").logln(USR.STDOUT, "InfoSource connect to " + outputDataAddress);

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
            dataSource.addProbe(probe);  // this does registerProbe and activateProbe
            return new ApplicationResponse(true, "");
        } catch (Exception e) {
            return new ApplicationResponse(false, e.getMessage());
        }
    }


    /**
     * Stop
     */
    public ApplicationResponse stop() {
        closing_= true;
	dataSource.removeProbe(probe);

        dataSource.disconnect();

        try {
            dataIndex.close();
	} catch (TimeIndexException tie) {
	    tie.printStackTrace();
	    return new ApplicationResponse(false, "Cannot close TimeIndex " + dataIndexPath) ;
	}


        synchronized (this) {
            notifyAll();
        }

        return new ApplicationResponse(true, "");

    }

    /**
     * Run
     */
    public void run() {
        try {
            //Logger.getLogger("log").logln(USR.STDOUT, "SLEEP  " + getInitialDelay() + " seconds");
            Thread.sleep(getInitialDelay() * 1000);
        } catch (InterruptedException ie) {
        }
        
        //Logger.getLogger("log").logln(USR.STDOUT, "TURN ON Probe: " + probe.getName());
        dataSource.turnOnProbe(probe);

        // A DataSource already runs in itws own thread
        // so this one can wait and do nothing.
        if (!closing_) {
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException ie) {
            }
        }
        // Logger.getLogger("log").logln(USR.STDOUT, "TURN OFF Probe: " + probe.getName());
	  dataSource.turnOffProbe(probe);
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
     * Get the initial delay
     */
    public int getInitialDelay() {
	return initialDelay;
    }

    /**
     * Set the initialDelay
     */
    public int setInitialDelay(int delay) {
	int oldInitialDelay = initialDelay;
	initialDelay = delay;

	return oldInitialDelay;
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
	    probe = new CPUInfo(name + "cpu-info");
	    break;

	case Memory:
	    probe = new MemoryInfo(name + "memory-info");
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
		Logger.getLogger("log").logln(USR.ERROR, "Can't add data to time index log " + dataIndex.getName());
                return result;
	    }
	}

    }
}
