// InfoConsumer.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.aggregator.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.net.*;
import usr.net.GIDAddress;
import plugins_usr.monitoring.distribution.USRDataPlaneConsumerWithNames;
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.plane.DataPlane;
import java.util.Scanner;
import java.util.Properties;
import java.io.File;
import java.io.Serializable;
import com.timeindexing.index.*;
import com.timeindexing.time.MillisecondTimestamp;
import com.timeindexing.data.SerializableItem;

/**
 * An InfoConsumer that can receive measurements.
 *
 * Has args:
 * input mulitcast addr
 * logpath - the path to log into
 */
public class InfoConsumer implements Reporter {
    // The address for input
    SocketAddress inputDataAddress = new SocketAddress(2299);

    /*
     * The receiver for the data domain.
     * This receives measurements.
     */
    DataConsumer dataDomain;

    // The name
    String name = "info-consumer";

    // The place to store the collected data
    String collectorPath = "/tmp/";

    /*
     * The Time Index that holds the sent data
     */
    IndexView dataIndex;


    /**
     * InfoConsumer constructor.
     */
    public InfoConsumer() {
    }

    /**
     * Start the InfoConsumer.
     */
    public void start() {
        System.err.println("InfoConsumer: start");

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

        // Set up the data listener
        // this is the handler
        dataDomain = new DataConsumer(this);

        System.err.println("InfoConsumer connect to " + inputDataAddress);

        DataPlane inputDataPlane = new USRDataPlaneConsumerWithNames(inputDataAddress);

        dataDomain.setDataPlane(inputDataPlane);

        dataDomain.connect();

    }

    /**
     * Receiver of a measurment.
     */
    public void report(Measurement m) {
        System.out.println(m);

        try {
            Serializable object = (Serializable)m;
            dataIndex.addItem(new SerializableItem(object), new MillisecondTimestamp());
        } catch (TimeIndexException tie) {
            System.err.println("Can't add data to time index log " + dataIndex.getName());
        }
    }

    /**
     * Get the  address for inpput traffic.
     */
    public SocketAddress getInputAddress() {
        return inputDataAddress;
    }

    /**
     * Set the  address for input traffic.
     */
    public SocketAddress setInputAddress(SocketAddress in) {
        SocketAddress old = inputDataAddress;
        inputDataAddress = in;
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
     * Main entry point.
     * Args are:
     * -i input  address (default: @(0)/2299)
     * -l log path, (default: /tmp/)
     * -n name (default: "info-consumer")
     */
    public static void main(String[] args) {
        // Set up Router
        try {
            int port = 18191;
            int r2r = 18192;

            RouterEnv routerEnv = new RouterEnv(port, r2r, "Router-3");
            Router router = routerEnv.getRouter();

            // check
            if (routerEnv.isActive()) {
            } else {
                throw new Exception("Router failed to start");
            }

            // set ID
            router.setAddress(new GIDAddress(3));


        } catch (Exception e) {
            System.err.println("SimpleConsumer exception: " + e);
            e.printStackTrace();
            System.exit(2);
        }

        // allocate an InfoConsumer
        InfoConsumer infoConsumer = new InfoConsumer();

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
                    infoConsumer.setInputAddress(newInputAddr);
                    break;
                }

                case 'l': {
                    // assume a file name
                    File potentialPath = new File(argValue);

                    // check if directory part exists
                    if (potentialPath.isDirectory() && potentialPath.canWrite()) {
                        infoConsumer.setCollectionPath(argValue);
                    } else {
                        System.err.println("InfoConsumer: cannot write file in directory " + argValue);
                        System.exit(1);
                    }
                    break;
                }

                case 'n': {
                    infoConsumer.setName(argValue);
                    break;
                }



                default:
                    System.err.println("InfoConsumer: unknown option " + option);
                    break;
                }

            }
        }

        // start the info consumer
        infoConsumer.start();

    }

    /**
     * Simple DataConsumer
     */
    class DataConsumer extends AbstractDataConsumer  {

        /**
         * Construct a BasicConsumer.
         */
        public DataConsumer(InfoConsumer info) {
            addReporter(info);
        }

    }

}