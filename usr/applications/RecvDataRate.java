package usr.applications;

import usr.net.*;
import usr.logging.*;
import java.nio.ByteBuffer;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.Map;

/**
 * An application for Receiving some data
 * It prints out the data rate.
 */
public class RecvDataRate implements Application, RuntimeMonitoring {
    int port = 0;

    boolean running = false;
    DatagramSocket socket = null;

    // Timer stuff
    Timer timer;
    TimerTask timerTask;

    // total no of Datagrams in
    int count = 0;
    // last time count
    int lastTimeCount = 0;
    // no per second
    int diffs = 0;
    // elaspsed time
    long startTime = 0;
    long lastTime = 0;

    /**
     * Constructor for RecvDataRate
     */
    public RecvDataRate() {
    }

    /**
     * Initialisation for Recv.
     * Recv port
     */
    public ApplicationResponse init(String[] args) {
        if (args.length == 1) {
            // try port
            Scanner scanner = new Scanner(args[0]);

            if (scanner.hasNextInt()) {
                port = scanner.nextInt();
            } else {
                return new ApplicationResponse(false, "Bad port " + args[1]);
            }

            return new ApplicationResponse(true, "");

        } else {
            return new ApplicationResponse(false, "Usage: Recv port");
        }
    }

    /** Start application with argument  */
    public ApplicationResponse start() {
        try {
            // set up socket
            socket = new DatagramSocket();

            socket.bind(port);

            // Logger.getLogger("log").logln(USR.ERROR, "Socket has source port "+socket.getLocalPort());

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot open socket " + e.getMessage());
            return new ApplicationResponse(false, "Cannot open socket " + e.getMessage());
        }

        // set up timer to count throughput
        timerTask = new TimerTask() {
            boolean running = true;

            public void run() {
                if (running) {
                    diffs = count - lastTimeCount;
                    lastTime = System.currentTimeMillis();
                    long elaspsedSecs = (lastTime - startTime)/1000;
                    long elaspsedMS = (lastTime - startTime)%1000;

                    usr.net.Address address = usr.router.RouterDirectory.getRouter().getAddress();
                    Logger.getLogger("log").logln(USR.STDOUT,
                                                  address + ": Task count: " + count + " time:" + elaspsedSecs + "." + elaspsedMS + " diff: "  +
                                                  diffs);
                    lastTimeCount = count;
                }
            }

            public boolean cancel() {
                Logger.getLogger("log").log(USR.STDOUT, "cancel @ " + count);

                if (running) {
                    running = false;
                }


                return running;
            }

            public long scheduledExecutionTime() {
                Logger.getLogger("log").log(USR.STDOUT, "scheduledExecutionTime:");
                return 0;
            }

        };

        // if there is no timer, start one
        if (timer == null) {
            timer = new Timer();
            timer.schedule(timerTask, 1000, 1000);
        }

        startTime = System.currentTimeMillis();

        running = true;

        return new ApplicationResponse(true, "");
    }

    /** Implement graceful shut down */
    public ApplicationResponse stop() {
        Logger.getLogger("log").logln(USR.STDOUT, "Recv stop");

        running = false;

        timer.cancel();

        if (socket != null) {
            socket.close();
        }

        return new ApplicationResponse(true, "");
    }

    /** Run the ping application */
    public void run() {
        Datagram datagram;

        try {
            while ((datagram = socket.receive()) != null) {
                count++;
            }
        } catch (SocketException se) {
            Logger.getLogger("log").log(USR.ERROR, se.getMessage());
        }


    }

    /**
     * Return a map of monitoring data.
     */
    public Map<String, String> getMonitoringData() {
        Map theMap = new HashMap<String, String>();

        theMap.put("diffs", Integer.toString(diffs));
        theMap.put("count", Integer.toString(count));

        return theMap;
    }

}
