// AggPoint.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.aggregator.appl;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import plugins_usr.monitoring.distribution.USRDataPlaneConsumerWithNames;
import plugins_usr.monitoring.distribution.USRDataPlaneProducerWithNames;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.GIDAddress;
import usr.net.SocketAddress;

import com.timeindexing.basic.AbsolutePosition;
import com.timeindexing.basic.EndPointInterval;
import com.timeindexing.basic.Interval;
import com.timeindexing.basic.Position;
import com.timeindexing.time.Second;
import com.timeindexing.time.TimeDirection;

import eu.reservoir.aggregator.Aggregate;
import eu.reservoir.aggregator.AggregateFn;
import eu.reservoir.aggregator.AggregationPoint;
import eu.reservoir.aggregator.AggregatorMeasurement;
import eu.reservoir.aggregator.Chooser;
import eu.reservoir.aggregator.ChooserResult;
import eu.reservoir.aggregator.Extractor;
import eu.reservoir.aggregator.Filter;
import eu.reservoir.aggregator.Forwarder;
import eu.reservoir.aggregator.SleepSeconds;
import eu.reservoir.aggregator.Timing;
import eu.reservoir.monitoring.core.DefaultProbeAttribute;
import eu.reservoir.monitoring.core.DefaultProbeValue;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.ProbeAttribute;
import eu.reservoir.monitoring.core.ProbeAttributeType;
import eu.reservoir.monitoring.core.ProbeValue;
import eu.reservoir.monitoring.core.plane.DataPlane;
//import eu.reservoir.monitoring.im.*;

/**
 * Main AggregationPoint.
 *
 * Has args:
 * input addr
 * output addr
 * function - avg, variance, sd, length, sum, min, max
 * filter - none, 5% change, 10% change
 * time - e.g. 30 seconds
 * path - log file directory
 * name - name of log files
 * This aggregates measurements by averaging data for the last 30
 ***************************seconds,
 * every 30 seconds.
 */
public class AggPoint implements Application {
    /*
     * Set up the Collector
     */

    // The default address for the collector.
    SocketAddress inputDataAddress;

    // the name of this AggPoint
    String name = "agg-point";

    // The place to store the collected data
    String collectorPath = "/tmp/";

    // The extraction function, which takes an individual Measurements
    // and returns the data to be stored by the Collector
    // In this case it is the value returned by Measurement::getValues()
    // which is an ArrayList<ProbeValue>
    Extractor onlyProbeValues = new Extractor() {
            @Override
            public Serializable extract(Measurement m){
                //Serializable object = (Serializable)m.getValues();
                Serializable object = (Serializable)m;

                Logger.getLogger("log").logln(USR.APP_EXTRA, "Collected: " + object);
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
            @Override
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
            @Override
            public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
                Collection<Number> list = convert(coll);
                Aggregate aggregate = new Aggregate();
                Number number = aggregate.average(list);
                Logger.getLogger("log").logln(USR.APP_EXTRA, "Aggregation: average = " + number);

                // now create a Measurement as the result

                try {
                    // field 0 is the avg
                    DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                    // create a list of ProbeValue
                    ArrayList <ProbeValue> pvList = new ArrayList<ProbeValue>();
                    pvList.add(pv0);

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
            @Override
            public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
                Collection<Number> list = convert(coll);
                Aggregate aggregate = new Aggregate();
                Number number = aggregate.variance(list);
                Logger.getLogger("log").logln(USR.APP_EXTRA, "Aggregation: variance = " + number);
                // now create a Measurement as the result

                try {
                    // field 0 is the avg
                    DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                    // create a list of ProbeValue
                    ArrayList <ProbeValue> pvList = new ArrayList<ProbeValue>();
                    pvList.add(pv0);

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
            @Override
            public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
                Collection<Number> list = convert(coll);
                Aggregate aggregate = new Aggregate();
                Number number = aggregate.sd(list);
                Logger.getLogger("log").logln(USR.APP_EXTRA, "Aggregation: sd = " + number);

                // now create a Measurement as the result

                try {
                    // field 0 is the avg
                    DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                    // create a list of ProbeValue
                    ArrayList <ProbeValue>pvList = new ArrayList <ProbeValue>();
                    pvList.add(pv0);

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
            @Override
            public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
                Collection<Number> list = convert(coll);
                Aggregate aggregate = new Aggregate();
                Number number = aggregate.length(list);
                Logger.getLogger("log").logln(USR.APP_EXTRA, "Aggregation: length = " + number);

                // now create a Measurement as the result

                try {
                    // field 0 is the avg
                    DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                    // create a list of ProbeValue
                    ArrayList <ProbeValue>pvList = new ArrayList<ProbeValue>();
                    pvList.add(pv0);

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
            @Override
            public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
                Collection<Number> list = convert(coll);
                Aggregate aggregate = new Aggregate();
                Number number = aggregate.sum(list);
                Logger.getLogger("log").logln(USR.APP_EXTRA, "Aggregation: sum = " + number);

                // now create a Measurement as the result

                try {
                    // field 0 is the avg
                    DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                    // create a list of ProbeValue
                    ArrayList <ProbeValue> pvList = new ArrayList<ProbeValue>();
                    pvList.add(pv0);

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
            @Override
            public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
                Collection<Number> list = convert(coll);
                Aggregate aggregate = new Aggregate();
                Number number = aggregate.min(list);
                Logger.getLogger("log").logln(USR.APP_EXTRA, "Aggregation: min = " + number);

                // now create a Measurement as the result

                try {
                    // field 0 is the avg
                    DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                    // create a list of ProbeValue
                    ArrayList <ProbeValue> pvList = new ArrayList<ProbeValue> ();
                    pvList.add(pv0);

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
            @Override
            public AggregatorMeasurement aggregate(Collection<ChooserResult> coll) {
                Collection<Number> list = convert(coll);
                Aggregate aggregate = new Aggregate();
                Number number = aggregate.max(list);
                Logger.getLogger("log").logln(USR.APP_EXTRA, "Aggregation: max = " + number);

                // now create a Measurement as the result

                try {
                    // field 0 is the avg
                    DefaultProbeValue pv0 = new DefaultProbeValue(0, number);

                    // create a list of ProbeValue
                    ArrayList <ProbeValue> pvList = new ArrayList<ProbeValue> ();
                    pvList.add(pv0);

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
    SocketAddress outputDataAddress;

    // The ProbeAttributes
    List<ProbeAttribute> probeAttributes = null;

    // setup an enum for each of the Filters
    enum FilterSpecifer { Always, Percent2, Percent5, Percent10 };

    // Filter which always returns the value
    // i.e. no filtering
    Filter always = new Filter() {
            @Override
            public boolean filter(Forwarder forwarder, AggregatorMeasurement m) {
                return true;
            }
        };

    // Filter only returns value if it is different by 5%
    Filter filter5pcTolerance = new Filter() {
            @Override
            public boolean filter(Forwarder forwarder, AggregatorMeasurement m) {
                AggregatorMeasurement oldValue = forwarder.getOldValue();

                if (oldValue == null)
                    return true;

                // get the first field of m
                Double mVal = (Double)m.getValues().get(0).getValue();
                // get the first field of oldValue
                Double oVal = (Double)oldValue.getValues().get(0).getValue();

                if (oVal == 0.0)
                    return mVal != 0.0;
                double percent = mVal / oVal;

                //	Logger.getLogger("log").logln(USR.APP_EXTRA, "Filter: "
                // + mVal + "/" + oVal + " = " +
                //			   percent);

                // test for 5% tolerance -  0.95 -> 1.05
                if (0.95 < percent && percent < 1.05)
                    // values too similar
                    return false;
                else
                    return true;
            }
        };

    // Filter only returns value if it is different by 2%
    Filter filter2pcTolerance = new Filter() {
            @Override
            public boolean filter(Forwarder forwarder, AggregatorMeasurement m) {
                AggregatorMeasurement oldValue = forwarder.getOldValue();

                if (oldValue == null)
                    return true;

                // get the first field of m
                Double mVal = (Double)m.getValues().get(0).getValue();
                // get the first field of oldValue
                Double oVal = (Double)oldValue.getValues().get(0).getValue();
                if (oVal == 0.0)
                    return mVal != 0.0;
                double percent = mVal / oVal;

                //System.out.println("Filter: " + mVal + "/" + oVal + " = "
                // +
                //		   percent);

        

                //System.out.println("Filter: " + mVal + "/" + oVal + " = "
                // +
                //		   ((1 - percent) * 100) + " lower = " + lower +
                //		   " upper = " + upper);

                // test for 2% tolerance -  0.98 -> 1.02
                if (0.98 < percent && percent < 1.02)
                    // values too similar
                    return false;
                else
                    return true;
            }
        };

    // Filter only returns value if it is different by 10%
    Filter filter10pcTolerance = new Filter() {
            @Override
            public boolean filter(Forwarder forwarder, AggregatorMeasurement m) {
                AggregatorMeasurement oldValue = forwarder.getOldValue();

                if (oldValue == null)
                    return true;

                // get the first field of m
                Double mVal = (Double)m.getValues().get(0).getValue();
                // get the first field of oldValue
                Double oVal = (Double)oldValue.getValues().get(0).getValue();
                if (oVal == 0.0)
                    return mVal != 0.0;
                double percent = mVal / oVal;

                //System.out.println("Filter: " + mVal + "/" + oVal + " = "
                // +
                //		   percent);

                // test for 10% tolerance -  0.90 -> 1.10
                if (0.90 < percent && percent < 1.10)
                    // values too similar
                    return false;
                else
                    return true;
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
    public AggPoint(){
    }

    /**
     * init
     * Args are:
     * -i input address
     * -o output address
     * -a aggregatefn,  [average, variance, sd, length, sum, min, max] (NO
     ***************************default)
     * -f filter, [always, 2%, 5%, 10%]  (default: always)
     * -l log path, (default: /tmp/)
     * -t sleep timeout (default: 30)
     * -n name (default: "agg-point")
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
                    setInputAddress(newInputAddr);
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
                    setOutputAddress(newOutputAddr);
                    break;
                }

                case 'a': {
                    if (argValue.equals("average")) {
                        setAggregateFn(AggregateFnSpecifer.Average);
                    } else if (argValue.equals("variance")) {
                        setAggregateFn(AggregateFnSpecifer.Variance);
                    } else if (argValue.equals("sum")) {
                        setAggregateFn(AggregateFnSpecifer.Sum);
                    } else if (argValue.equals("min")) {
                        setAggregateFn(AggregateFnSpecifer.Min);
                    } else if (argValue.equals("max")) {
                        setAggregateFn(AggregateFnSpecifer.Max);
                    } else {
                        Logger.getLogger("log").logln( USR.ERROR, "AggPoint: unknown aggregation function " + argValue);
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
                        Logger.getLogger("log").logln( USR.ERROR, "AggPoint: unknown filter " + argValue);
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
                        Logger.getLogger("log").logln( USR.ERROR, "AggPoint: cannot write file in directory " + argValue);
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

                case 'n': {
                    setName(argValue);
                    break;
                }

                default:
                    Logger.getLogger("log").logln( USR.ERROR, "AggPoint: unknown option " + option);
                    break;
                }
            }
        }

        // set up ProbeAttributes where there is one value from a
        // Measurement
        List<ProbeAttribute> pList = new ArrayList<ProbeAttribute>();
        
        // field 0 is the value
        DefaultProbeAttribute pa0 = new DefaultProbeAttribute(0, "value", ProbeAttributeType.DOUBLE, "n");

        pList.add(pa0);

        setProbeAttributes(pList);

        // check actual AggregateFn
        if (actualAggregateFn == null)
            return new ApplicationResponse(false, "No Aggregation Fn has been set");

        // check inputDataAddress
        if (inputDataAddress == null)
            return new ApplicationResponse(false, "No Input Address has been set");

        // check outputDataAddress
        // don;t check - allow for no forwarding
        //if (outputDataAddress == null) {
        // return new ApplicationResponse(false, "No Output Address has
        // been
        // set");
        //}

        return new ApplicationResponse(true, "");
    }

    /**
     * Start the aggpoint
     */
    @Override
    public ApplicationResponse start(){
        Logger.getLogger("log").logln(USR.STDOUT, "AggPoint: input = " + inputDataAddress);

        // set up data plane
        DataPlane inputDataPlane = new USRDataPlaneConsumerWithNames(inputDataAddress);

        Logger.getLogger("log").logln(USR.STDOUT, "AggPoint: output = " + outputDataAddress);

        // set up data plane
        // might be null if there is no forwarding required
        DataPlane outputDataPlane = null;
        if (outputDataAddress != null)
            outputDataPlane = new USRDataPlaneProducerWithNames(outputDataAddress);

        // Allocate the AggregationPoint
        aggPoint = new AggregationPoint(inputDataPlane, outputDataPlane, name,
                                        collectorPath, onlyProbeValues, firstValue,
                                        actualFilter, sleepTime, interval,
                                        actualAggregateFn, probeAttributes);

        // and activate it.
        aggPoint.activateControl();


        if (aggPoint.isRunning()) {
            Logger.getLogger("log").logln(USR.STDOUT, "AggPoint: running");
        } else {
            Logger.getLogger("log").logln(USR.ERROR, "AggPoint: NOT running");
        }

        return new ApplicationResponse(true, "");
    }

    /**
     * Stop
     */
    @Override
    public ApplicationResponse stop(){
        aggPoint.deactivateControl();

        synchronized (this) {
            notifyAll();
        }

        return new ApplicationResponse(true, "");
    }

    /**
     * Run
     */
    @Override
    public void run(){
        // A AggPoint already runs in itws own thread
        // so this one can wait and do nothing.
        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException ie) {
        }
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

    /**
     * Get the sleep time between aggregations.
     */
    public int getSleepTime(){
        return sleepTime.getSeconds();
    }

    /**
     * Set the sleep time between aggregations.
     */
    public int setSleepTime(int slTime){
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
    public SocketAddress getInputAddress(){
        return inputDataAddress;
    }

    /**
     * Set the address for input traffic to the aggregation point.
     */
    public SocketAddress setInputAddress(SocketAddress in){
        SocketAddress old = inputDataAddress;

        inputDataAddress = in;
        return old;
    }

    /**
     * Get the address for output traffic to the aggregation point.
     */
    public SocketAddress getOutputAddress(){
        return outputDataAddress;
    }

    /**
     * Set the address for output traffic to the aggregation point.
     */
    public SocketAddress setOutputAddress(SocketAddress out){
        SocketAddress old = outputDataAddress;

        outputDataAddress = out;
        return old;
    }

    /**
     * Get the current AggregateFn.
     */
    public AggregateFn getAggregateFn(){
        return actualAggregateFn;
    }

    /**
     * Set the AggregateFn.
     */
    public AggregateFn setAggregateFn(AggregateFnSpecifer spec){
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
    public Filter getFilter(){
        return actualFilter;
    }

    /**
     * Set the Filter
     */
    public Filter setFilter(FilterSpecifer spec){
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
    public List<ProbeAttribute> getProbeAttributes(){
        return probeAttributes;
    }

    /**
     * Set the ProbeAttributes
     */
    public void setProbeAttributes(List<ProbeAttribute> list){
        probeAttributes = list;
    }

    /*
     * Support functions for AggregateFn
     */

    /**
     * Convert a Collection<ChooserResult> to  Collection<Number>
     */
    private Collection<Number> convert(Collection<ChooserResult> coll){
        ArrayList <Number>list = new ArrayList<Number>();
        for (ChooserResult cr : coll)
            list.add(((NumberCR)cr).number);

        return list;
    }
}

// A ChooserResult class.
// This one is a wrapper for a Number.
class NumberCR implements ChooserResult
{
    public Number number;

    public NumberCR(Number n){
        number = n;
    }
}
