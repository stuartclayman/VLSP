package ikms.processor;

import ikms.core.Response;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;

import cc.clayman.logging.Logger;
import cc.clayman.logging.MASK;
import eu.reservoir.monitoring.appl.BasicConsumer;
import eu.reservoir.monitoring.core.Reporter;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.distribution.udp.UDPDataPlaneConsumerWithNames;

public class RouterMeasurementProcessor implements Processor {
    // A monitoring address
    InetSocketAddress monitoringAddress;
    int monitoringPort = 22998;
    int monitoringTimeout = 1;


    // A BasicConsumer for the stats of a Router
    BasicConsumer dataConsumer;

    // and the Reporters that handle the incoming measurements
    // Label -> Reporter
    HashMap<String, Reporter> reporterMap;


    /**
     * Construct a RouterMeasurementProcessor
     */
    public RouterMeasurementProcessor() {
        // reporterList
        reporterMap = new HashMap<String, Reporter>();

    }


    /**
     * Initialize with some args
     */
    public Response init(String[] args) {
        Logger.getLogger("log").logln(MASK.STDOUT, leadin()+ " init with args " + Arrays.toString(args));

        // setup DataConsumer
        dataConsumer = new BasicConsumer();

        String myAddress = "localhost";

        try {
            myAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException uhe) {
            return new Response(false, uhe.getMessage());
        }

        monitoringAddress = new InetSocketAddress(myAddress, monitoringPort);

        // set up Reporter
        reporterMap.put("NetIFStats", new NetIFStatsReporter());

        return new Response(true, monitoringAddress.toString());
    }

    /**
     * Start an application.
     */
    public Response start() {
        Logger.getLogger("log").logln(MASK.STDOUT, leadin()+ "Starting");

        // start monitoring
        // listening on the GlobalController address
        startMonitoringConsumer(monitoringAddress);

        return new Response(true, monitoringAddress.toString());
    }

    /**
     * Start listening for router stats using monitoring framework.
     */
    public synchronized void startMonitoringConsumer(InetSocketAddress addr) {

        // check to see if the monitoring is already connected and running
        if (dataConsumer.isConnected()) {
            // if it is, stop it first
            stopMonitoringConsumer();
        }

        // set up DataPlane
        DataPlane inputDataPlane = new UDPDataPlaneConsumerWithNames(addr);
        dataConsumer.setDataPlane(inputDataPlane);

        // set the reporter
        dataConsumer.clearReporters();

        // add probes
        for (Reporter reporter : reporterMap.values()) {
            dataConsumer.addReporter(reporter);  
        }

        // and connect
        boolean connected = dataConsumer.connect();

        if (!connected) {
            System.err.println("Cannot startMonitoringConsumer on " + addr + ". Address probably in use. Exiting.");
            System.exit(1);
        }

    }

    /**
     * Stop an application.
     */
    public Response stop() {
        Logger.getLogger("log").logln(MASK.STDOUT, leadin()+ "Stopping");
        stopMonitoringConsumer();

       synchronized (this) {
            notifyAll();
        }

        return new Response(true, monitoringAddress.toString());
    }


    /**
     * Stop monitoring.
     */
    public synchronized void stopMonitoringConsumer() {
        if (dataConsumer.isConnected()) {
            dataConsumer.clearReporters();  // was setReporter(null);

            dataConsumer.disconnect();
        }

    }


    /**
     * run just waits for the monitoring to stop.
     */
    public void run() {
        Logger.getLogger("log").logln(MASK.STDOUT, leadin()+ "Top of run()");
        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException ie) {
        }
        Logger.getLogger("log").logln(MASK.STDOUT, leadin()+ "End of run()");
    }

    /**
     * Close down a Processor
     */
    public Response close() {
        Logger.getLogger("log").logln(MASK.STDOUT, leadin()+ "Closing");

        return new Response(true, "closed");
    }


    private String leadin() {
        return "RouterMeasurementProcessor: ";
    }
}
