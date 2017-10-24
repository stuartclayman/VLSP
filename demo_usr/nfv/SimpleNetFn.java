package demo_usr.nfv;

import java.net.SocketException;
import java.net.NoRouteToHostException;
import java.util.Scanner;
import java.nio.ByteBuffer;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.NetworkException;
import usr.applications.*;
import usr.router.Intercept;
import usr.dcap.DcapNetworkInterface;
import usr.protocol.Protocol;

import demo_usr.paths.Reconfigure;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

/**
 * An application for receiving some data and acting as a Network Function.
 * <p>
 * SimpleNetFn [optionals]
 * Optional args:
 * -a intercept interface where address of remote router (e.g. 1)
 * -i intercept interface where it is the ith port of the router fabric (e.g 0)
 * -n intercept interface where the name of the interface is specified
 * -v verbose
 */
public class SimpleNetFn implements Application, DatagramProcessor, Reconfigure {
    int count = 0;
    boolean running = false;

    // Address of remote router for interface to listen on
    int arg;
    Address addr;

    Intercept intercept = null;


    boolean receiveError = false;

    /**
     * Constructor for SimpleNetFn
     */
    public SimpleNetFn() {
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
                arg = scanner.nextInt();
                scanner.close();
            } else {
            	scanner.close();
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
            // set up capture
            addr = AddressFactory.newAddress(arg);

            DcapNetworkInterface interceptNIF = DcapNetworkInterface.getIFByAddress(addr);

            if (interceptNIF != null) {
                intercept = interceptNIF.intercept();
            } else {
                Logger.getLogger("log").logln(USR.ERROR, "No InterceptNetworkInterface for Address: " + addr);
                return new ApplicationResponse(false, "No InterceptNetworkInterface for Address: " + addr);
            }


            // Logger.getLogger("log").logln(USR.ERROR, "Socket has source port "+socket.getLocalPort());

        } catch (Exception e) {
            e.printStackTrace();

            Logger.getLogger("log").logln(USR.ERROR, "Cannot open DatagramCapture " + e.getMessage());
            return new ApplicationResponse(false, "Cannot open DatagramCapture " + e.getMessage());
        }

        running = true;

        return new ApplicationResponse(true, "");
    }

    /** Implement graceful shut down */
    public ApplicationResponse stop() {
        running = false;

        if (intercept != null) {
            intercept.close();

            Logger.getLogger("log").logln(USR.STDOUT, "Recv stop");
        }

        return new ApplicationResponse(true, "");
    }

    /** Run the ping application */
    public void run() {
        Datagram datagram;

        while ((datagram = receiveDatagram()) != null) {

            boolean processed = processDatagram(datagram);
                
            if (processed) {
                // send onwards
                boolean result = forwardDatagram(datagram);
            }
        }
    }

    /**
     * Receive a Datagram
     */
    public Datagram receiveDatagram() {
        try {
            return intercept.receive();
        } catch (NetworkException se) {
            Logger.getLogger("log").log(USR.ERROR, se.getMessage());

            receiveError = true;

            return null;
        }

    }

    /**
     * Process the recevied Datagram.
     * By default nothing is done to the Datagram.  Subclasses can do more.
     */
    public boolean processDatagram(Datagram datagram) {
        return true;
    }

    /**
     * Forward the recevied Datagram, possibly.
     * If processDatagram() returns false this will not be called.
     */
    public boolean forwardDatagram(Datagram datagram) {
        try {
            intercept.send(datagram);

            return true;
        } catch (NoRouteToHostException ne) {
            Logger.getLogger("log").log(USR.ERROR, ne.getMessage());

            return false;
        }
    }


    /**
     * Process a reconfiguration
     */
    public Object process(JSONObject jsobj) {
        return new ApplicationResponse(true, "process");
    }


}
