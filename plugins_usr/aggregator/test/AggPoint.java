// AggPoint.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.aggregator.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.net.*;
import usr.interactor.RouterInteractor;
import plugins_usr.monitoring.distribution.USRDataPlaneConsumerWithNames;
import plugins_usr.monitoring.distribution.USRDataPlaneProducerWithNames;
import eu.reservoir.aggregator.*;
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.plane.DataPlane;
import com.timeindexing.time.*;
import com.timeindexing.basic.*;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.Serializable;
import java.io.File;


/**
 * Main AggregationPoint.
 *
 * Has args:
 * input mulitcast addr
 * output mulitcast addr
 * function - avg, variance, sd, length, sum, min, max
 * filter - none, 5% change, 10% change
 * time - e.g. 30 seconds
 * path - log file directory
 * name - name of log files
 * This aggregates measurements by averaging data for the last 30 seconds,
 * every 30 seconds.
 * It listens on SocketAddress(2299) by default
 * and forwards to  SocketAddress(@(3), 2288) by default
 */
public class AggPoint {

    /*
     * Set up the Collector
     */

    // The default address for the collector.
    SocketAddress inputDataAddress = new SocketAddress(2299);

    // the name of this AggPoint
    String name = "agg-point";

    // The place to store the collected data
    String collectorPath = "/tmp/";


    // The extraction function, which takes an individual Measurements
    // and returns the data to be stored by the Collector
    // In this case it is the value returned by Measurement::getValues()
    // which is an ArrayList<ProbeValue>
    Extractor onlyProbeValues = new Extractor() {
        public Serializable extract(Measurement m) {
            //Serializable object = (Serializable)m.getValues();
            Serializable object = (Serializable)m;
            System.out.println("Collected: " + object);
            return object;
        }

    };


    /*
     * Set up the Selector
     */
    // The chooser function is applied by the Selector to values
    // collected from the Extractor.
    // In this case it takes an ArrayList<ProbeValue> and
    // returns value of first one, which is a Number
    Chooser firstValue = new Chooser() {
        public ChooserResult choose(Object obj) {
            Measurement m = (Measurement)obj;
            //ArrayList<ProbeValue> list = (ArrayList<ProbeValue>)obj;
            List<ProbeValue> list = m.getValues();
            return new NumberCR((Number)list.get(0).getValue());
        }

    };


    /*
     * Set up the Aggregator
     */

    // setup an enum for each of the AggregateFns
    enum AggregateFnSpecifer { Average, Variance, SD, Length, Sum, Min, Max };

    // Define some aggregation functions
    // These take the Collection<Number> that is returned by
    // the Selector and aggregates them.

    // The average function.
    AggregateFn average = new AggregateFn() {
        public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
            Collection<Number> list = convert(coll);
            Aggregate aggregate = new Aggregate();
            Number number = aggregate.average(list);
            System.out.println("Aggregation: average = " + number);

            // now create a Measurement as the result

            try {
                // field 0 is the avg
                DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                // create a list of ProbeValue
                ArrayList<ProbeValue> pvList = new ArrayList<ProbeValue>();
                pvList.add((ProbeValue)pv0);

                // Construct the Measurement
                AggregatorMeasurement m = new AggregatorMeasurement("aggregated-average", pvList);

                // return it
                return m;
            } catch (Exception e) {
                return null;
            }


        }

    };


    // The variance function.
    AggregateFn variance = new AggregateFn() {
        public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
            Collection<Number> list = convert(coll);
            Aggregate aggregate = new Aggregate();
            Number number = aggregate.variance(list);
            System.out.println("Aggregation: variance = " + number);
            // now create a Measurement as the result

            try {
                // field 0 is the avg
                DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                // create a list of ProbeValue
                ArrayList<ProbeValue> pvList = new ArrayList<ProbeValue>();
                pvList.add((ProbeValue)pv0);

                // Construct the Measurement
                AggregatorMeasurement m = new AggregatorMeasurement("aggregated-variance", pvList);

                // return it
                return m;
            } catch (Exception e) {
                return null;
            }

        }

    };


    // The standard deviation function.
    AggregateFn sd = new AggregateFn() {
        public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
            Collection<Number> list = convert(coll);
            Aggregate aggregate = new Aggregate();
            Number number = aggregate.sd(list);
            System.out.println("Aggregation: sd = " + number);

            // now create a Measurement as the result

            try {
                // field 0 is the avg
                DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                // create a list of ProbeValue
                ArrayList<ProbeValue> pvList = new ArrayList<ProbeValue>();
                pvList.add((ProbeValue)pv0);

                // Construct the Measurement
                AggregatorMeasurement m = new AggregatorMeasurement("aggregated-sd", pvList);

                // return it
                return m;
            } catch (Exception e) {
                return null;
            }
        }

    };


    // The length function.
    AggregateFn length = new AggregateFn() {
        public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
            Collection<Number> list = convert(coll);
            Aggregate aggregate = new Aggregate();
            Number number = aggregate.length(list);
            System.out.println("Aggregation: length = " + number);

            // now create a Measurement as the result

            try {
                // field 0 is the avg
                DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                // create a list of ProbeValue
                ArrayList<ProbeValue> pvList = new ArrayList<ProbeValue>();
                pvList.add((ProbeValue)pv0);

                // Construct the Measurement
                AggregatorMeasurement m = new AggregatorMeasurement("aggregated-length", pvList);

                // return it
                return m;
            } catch (Exception e) {
                return null;
            }
        }

    };

    // The sum function.
    AggregateFn sum = new AggregateFn() {
        public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
            Collection<Number> list = convert(coll);
            Aggregate aggregate = new Aggregate();
            Number number = aggregate.sum(list);
            System.out.println("Aggregation: sum = " + number);

            // now create a Measurement as the result

            try {
                // field 0 is the avg
                DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                // create a list of ProbeValue
                ArrayList<ProbeValue> pvList = new ArrayList<ProbeValue>();
                pvList.add((ProbeValue)pv0);

                // Construct the Measurement
                AggregatorMeasurement m = new AggregatorMeasurement("aggregated-sum", pvList);

                // return it
                return m;
            } catch (Exception e) {
                return null;
            }

        }

    };


    // The min function.
    AggregateFn min = new AggregateFn() {
        public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
            Collection<Number> list = convert(coll);
            Aggregate aggregate = new Aggregate();
            Number number = aggregate.min(list);
            System.out.println("Aggregation: min = " + number);

            // now create a Measurement as the result

            try {
                // field 0 is the avg
                DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                // create a list of ProbeValue
                ArrayList<ProbeValue> pvList = new ArrayList<ProbeValue>();
                pvList.add((ProbeValue)pv0);

                // Construct the Measurement
                AggregatorMeasurement m = new AggregatorMeasurement("aggregated-min", pvList);

                // return it
                return m;
            } catch (Exception e) {
                return null;
            }

        }

    };


    // The max function.
    AggregateFn max = new AggregateFn() {
        public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
            Collection<Number> list = convert(coll);
            Aggregate aggregate = new Aggregate();
            Number number = aggregate.max(list);
            System.out.println("Aggregation: max = " + number);

            // now create a Measurement as the result

            try {
                // field 0 is the avg
                DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                // create a list of ProbeValue
                ArrayList<ProbeValue> pvList = new ArrayList<ProbeValue>();
                pvList.add((ProbeValue)pv0);

                // Construct the Measurement
                AggregatorMeasurement m = new AggregatorMeasurement("aggregated-max", pvList);

                // return it
                return m;
            } catch (Exception e) {
                return null;
            }

        }

    };


    AggregateFn actualAggregateFn = null;

    /*
     * Set up the Forwarder
     */

    // The address for the forwarder
    GIDAddress addr = new GIDAddress(3);
    SocketAddress outputDataAddress = new SocketAddress(addr, 2288);

    // The ProbeAttributes
    List<ProbeAttribute> probeAttributes = null;

    // setup an enum for each of the Filters
    enum FilterSpecifer { Always, Percent2, Percent5, Percent10 };

    // Filter which always returns the value
    // i.e. no filtering
    Filter always = new Filter() {
        public boolean filter(Forwarder forwarder, AggregatorMeasurement m) {
            return true;
        }

    };

    // Filter only returns value if it is different by 5%
    Filter filter5pcTolerance = new Filter() {
        public boolean filter(Forwarder forwarder, AggregatorMeasurement m) {
            AggregatorMeasurement oldValue = forwarder.getOldValue();

            if (oldValue == null) {
                return true;
            }

            // get the first field of m
            Double mVal = (Double)m.getValues().get(0).getValue();
            // get the first field of oldValue
            Double oVal = (Double)oldValue.getValues().get(0).getValue();

            double percent = mVal / oVal;

            System.out.println("Filter: " + mVal + "/" + oVal + " = " +
                               percent);

            // test for 5% tolerance -  0.95 -> 1.05
            if (0.95 < percent && percent < 1.05) {
                // values too similar
                return false;
            } else {
                return true;
            }
        }

    };

    // Filter only returns value if it is different by 2%
    Filter filter2pcTolerance = new Filter() {
        public boolean filter(Forwarder forwarder, AggregatorMeasurement m) {
            AggregatorMeasurement oldValue = forwarder.getOldValue();

            if (oldValue == null) {
                return true;
            }

            // get the first field of m
            Double mVal = (Double)m.getValues().get(0).getValue();
            // get the first field of oldValue
            Double oVal = (Double)oldValue.getValues().get(0).getValue();

            double percent = mVal / oVal;

            System.out.println("Filter: " + mVal + "/" + oVal + " = " +
                               percent);


            float lower = oVal.floatValue() * 0.98f;
            float upper = oVal.floatValue() * 1.02f;

            System.out.println("Filter: " + mVal + "/" + oVal + " = " +
                               ((1 - percent) * 100) + " lower = " + lower +
                               " upper = " + upper);

            // test for 2% tolerance -  0.98 -> 1.02
            if (0.98 < percent && percent < 1.02) {
                // values too similar
                return false;
            } else {
                return true;
            }
        }

    };

    // Filter only returns value if it is different by 10%
    Filter filter10pcTolerance = new Filter() {
        public boolean filter(Forwarder forwarder, AggregatorMeasurement m) {
            AggregatorMeasurement oldValue = forwarder.getOldValue();

            if (oldValue == null) {
                return true;
            }

            // get the first field of m
            Double mVal = (Double)m.getValues().get(0).getValue();
            // get the first field of oldValue
            Double oVal = (Double)oldValue.getValues().get(0).getValue();

            double percent = mVal / oVal;

            System.out.println("Filter: " + mVal + "/" + oVal + " = " +
                               percent);

            // test for 10% tolerance -  0.90 -> 1.10
            if (0.90 < percent && percent < 1.10) {
                // values too similar
                return false;
            } else {
                return true;
            }
        }

    };

    // set default filter to pass every value
    Filter actualFilter = always;

    /*
     * Set up aggregation
     */

    // Here the AggregationPoint will wake up every 30 seconds
    // and select all the data from the last measurement,
    // back 30 seconds.

    // Set sleep time to be 30 seconds
    Timing.Regular sleepTime = new SleepSeconds(30);

    // The selection of data to get,
    // Get last 30 seconds of collected data, as per sleepTime
    Second backN = new Second(sleepTime.getSeconds(), TimeDirection.BACKWARD);
    // From Position.END_OF_INDEX, back 30 seconds
    Interval interval = new EndPointInterval((AbsolutePosition)Position.END_OF_INDEX, backN);

    //  The AggregationPoint
    AggregationPoint aggPoint;

    /**
     * Constructor
     */
    public AggPoint() {
    }

    /**
     * Start the aggpoint
     */
    public void start() {
        // check actual AggregateFn
        if (actualAggregateFn == null) {
            throw new Error("No Aggregation Fn has been set");
        }

        System.err.println("AggPoint: input = " + inputDataAddress);

        // set up data plane
        DataPlane inputDataPlane = new USRDataPlaneConsumerWithNames(inputDataAddress);

        System.err.println("AggPoint: output = " + outputDataAddress);

        // set up data plane
        DataPlane outputDataPlane = new USRDataPlaneProducerWithNames(outputDataAddress);

        // Allocate the AggregationPoint
        aggPoint = new AggregationPoint(inputDataPlane,
                                        outputDataPlane,
                                        name,
                                        collectorPath,
                                        onlyProbeValues,
                                        firstValue,
                                        actualFilter,
                                        sleepTime,
                                        interval,
                                        actualAggregateFn,
                                        probeAttributes);

        // and activate it.
        aggPoint.activateControl();
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

    /**
     * Get the sleep time between aggregations.
     */
    public int getSleepTime() {
        return sleepTime.getSeconds();
    }

    /**
     * Set the sleep time between aggregations.
     */
    public int setSleepTime(int slTime) {
        int oldSleepTime = sleepTime.getSeconds();

        // Set sleep time to be slTimeseconds
        sleepTime = new SleepSeconds(slTime);

        // The selection of data to get,
        // Get last N seconds of collected data, as per sleepTime
        Second backN = new Second(sleepTime.getSeconds(), TimeDirection.BACKWARD);
        // From Position.END_OF_INDEX, back 30 seconds
        interval = new EndPointInterval((AbsolutePosition)Position.END_OF_INDEX, backN);


        return oldSleepTime;
    }

    /**
     * Get the address for input traffic to the aggregation point.
     */
    public SocketAddress getInputAddress() {
        return inputDataAddress;
    }

    /**
     * Set the address for input traffic to the aggregation point.
     */
    public SocketAddress setInputAddress(SocketAddress in) {
        SocketAddress old = inputDataAddress;
        inputDataAddress = in;
        return old;
    }

    /**
     * Get the address for output traffic to the aggregation point.
     */
    public SocketAddress getOutputAddress() {
        return outputDataAddress;
    }

    /**
     * Set the address for output traffic to the aggregation point.
     */
    public SocketAddress setOutputAddress(SocketAddress out) {
        SocketAddress old = outputDataAddress;
        outputDataAddress = out;
        return old;
    }

    /**
     * Get the current AggregateFn.
     */
    public AggregateFn getAggregateFn() {
        return actualAggregateFn;
    }

    /**
     * Set the AggregateFn.
     */
    public AggregateFn setAggregateFn(AggregateFnSpecifer spec) {
        AggregateFn old = actualAggregateFn;

        switch (spec) {
        case Average:
            actualAggregateFn = average;
            break;

        case Variance:
            actualAggregateFn = variance;
            break;

        case SD:
            actualAggregateFn = sd;
            break;

        case Length:
            actualAggregateFn = length;
            break;

        case Sum:
            actualAggregateFn = sum;
            break;

        case Min:
            actualAggregateFn = min;
            break;

        case Max:
            actualAggregateFn = max;
            break;
        }

        return old;
    }

    /**
     * Get the current Filter
     */
    public Filter getFilter() {
        return actualFilter;
    }

    /**
     * Set the Filter
     */
    public Filter setFilter(FilterSpecifer spec) {
        Filter old = actualFilter;

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
     * Get the ProbeAttributes
     */
    public List<ProbeAttribute> getProbeAttributes() {
        return probeAttributes;
    }

    /**
     * Set the ProbeAttributes
     */
    public void setProbeAttributes(List<ProbeAttribute> list) {
        probeAttributes = list;
    }

    /*
     * Support functions for AggregateFn
     */

    /**
     * Convert a Collection<ChooserResult> to  Collection<Number>
     */
    private Collection<Number> convert(Collection<ChooserResult> coll) {
        ArrayList<Number> list = new ArrayList<Number>();

        for (ChooserResult cr : coll) {
            list.add(((NumberCR)cr).number);
        }

        return list;
    }

    /**
     * Main entry point.
     * Args are:
     * -i input address  (default: @(0)/2299)
     * -o output address (default: @(3)/2288)
     * -a aggregatefn,  [average, variance, sd, length, sum, min, max] (NO default)
     * -f filter, [always, 2%, 5%, 10%]  (default: always)
     * -l log path, (default: /tmp/)
     * -t sleep timeout (default: 30)
     * -n name (default: "agg-point")
     */
    public static void main(String[] args) {
        // the host that has the Router at addr
        String remHost = "localhost";
        int remPort = 19191;

        // Set up Router
        try {
            int port = 19191;
            int r2r = 19192;

            RouterEnv routerEnv = new RouterEnv(port, r2r, "Router-2");
            Router router = routerEnv.getRouter();

            // check
            if (routerEnv.isActive()) {
            } else {
                throw new Exception("Router failed to start");
            }

            // set ID
            router.setAddress(new GIDAddress(2));

            // connnect to the other router
            // first we tal kto my own ManagementConsole
            RouterInteractor selfInteractor = new RouterInteractor("localhost", 18191);

            // then set up Router-to-Router data connection
            selfInteractor.createConnection(remHost + ":" + remPort, 20);

            // and stop talking to the ManagementConsole
            selfInteractor.quit();



        } catch (Exception e) {
            System.err.println("SimpleConsumer exception: " + e);
            e.printStackTrace();
            System.exit(2);
        }

        // allocate an AggPoint
        AggPoint aggPoint = new AggPoint();

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

                case 'i': {
                    String[] parts = argValue.split("/");
                    Scanner sc = new Scanner(parts[0]);
                    int addr = sc.nextInt();
                    sc.close();
                    sc = new Scanner(parts[1]);
                    int port = sc.nextInt();
                    sc.close();

                    Address gidAddr = new GIDAddress(addr);

                    SocketAddress newInputAddr = new SocketAddress(gidAddr, port);
                    aggPoint.setInputAddress(newInputAddr);
                    break;
                }

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
                    aggPoint.setOutputAddress(newOutputAddr);
                    break;
                }

                case 'a': {
                    if (argValue.equals("average")) {
                        aggPoint.setAggregateFn(AggregateFnSpecifer.Average);
                    } else if (argValue.equals("variance")) {
                        aggPoint.setAggregateFn(AggregateFnSpecifer.Variance);
                    } else if (argValue.equals("sum")) {
                        aggPoint.setAggregateFn(AggregateFnSpecifer.Sum);
                    } else if (argValue.equals("min")) {
                        aggPoint.setAggregateFn(AggregateFnSpecifer.Min);
                    } else if (argValue.equals("max")) {
                        aggPoint.setAggregateFn(AggregateFnSpecifer.Max);
                    } else {
                        System.err.println("AggPoint: unknown aggregation function " + argValue);
                    }
                    break;
                }
                case 'f': {
                    if (argValue.equals("always")) {
                        aggPoint.setFilter(FilterSpecifer.Always);
                    } else if (argValue.equals("2%")) {
                        aggPoint.setFilter(FilterSpecifer.Percent2);
                    } else if (argValue.equals("5%")) {
                        aggPoint.setFilter(FilterSpecifer.Percent5);
                    } else if (argValue.equals("10%")) {
                        aggPoint.setFilter(FilterSpecifer.Percent10);
                    } else {
                        System.err.println("AggPoint: unknown filter " + argValue);
                    }
                    break;
                }

                case 'l': {
                    // assume a file name
                    File potentialPath = new File(argValue);

                    // check if directory part exists
                    if (potentialPath.isDirectory() && potentialPath.canWrite()) {
                        aggPoint.setCollectionPath(argValue);
                    } else {
                        System.err.println("AggPoint: cannot write file in directory " + argValue);
                        System.exit(1);
                    }
                    break;
                }

                case 't': {
                    Scanner sc = new Scanner(argValue);
                    int t = sc.nextInt();
                    sc.close();
                    aggPoint.setSleepTime(t);
                    break;
                }

                case 'n': {
                    aggPoint.setName(argValue);
                    break;
                }

                default:
                    System.err.println("AggPoint: unknown option " + option);
                    break;
                }

            }
        }


        // set up ProbeAttributes where there is one value from a Measurement
        List<ProbeAttribute> pList = new ArrayList<ProbeAttribute>();
        // field 0 is the value
        DefaultProbeAttribute pa0 = new DefaultProbeAttribute(0, "value", ProbeAttributeType.DOUBLE, "n");

        pList.add(pa0);

        aggPoint.setProbeAttributes(pList);

        // start the agg point
        aggPoint.start();
    }

}

// A ChooserResult class.
// This one is a wrapper for a Number.
class NumberCR implements ChooserResult {
    public Number number;

    public NumberCR(Number n) {
        number = n;
    }

}