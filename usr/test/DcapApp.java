package usr.test;

import java.net.SocketException;
import java.util.Scanner;
import java.nio.ByteBuffer;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.protocol.Protocol;
import usr.applications.*;
import usr.dcap.*;

/**
 * An application for Receiving some data
 */
public class DcapApp implements Application {
    int count = 0;

    boolean running = false;

    // Address of remote router for interface to listen on
    int arg;
    Address addr;

    Dcap dcap = null;

    /**
     * Constructor for Recv
     */
    public DcapApp() {
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

            DcapNetworkInterface dcapNIF = DcapNetworkInterface.getIFByAddress(addr);

            if (dcapNIF != null) {
                dcap = dcapNIF.open();
            } else {
                Logger.getLogger("log").logln(USR.ERROR, "No DcapNetworkInterface for Address: " + addr);
                return new ApplicationResponse(false, "No DcapNetworkInterface for Address: " + addr);
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

        if (dcap != null) {
            dcap.close();

            Logger.getLogger("log").logln(USR.STDOUT, "Recv stop");
        }

        return new ApplicationResponse(true, "");
    }

    /** Run the ping application */
    public void run() {
        Datagram datagram;

        try {
            while ((datagram = dcap.receive()) != null) {

                if (datagram.getProtocol() == Protocol.CONTROL) {
                    Logger.getLogger("log").log(USR.STDOUT, "CAPTURE: " + "CONTROL" + ". \n");
                } else {
                Logger.getLogger("log").log(USR.STDOUT, "CAPTURE: " + count + ". ");
                Logger.getLogger("log").log(USR.STDOUT, "HdrLen: " + datagram.getHeaderLength() +
                                            " Len: " + datagram.getTotalLength() +
                                            " Time: " + (System.currentTimeMillis() - datagram.getTimestamp()) +
                                            " From: " + datagram.getSrcAddress() +
                                            " To: " + datagram.getDstAddress() +
                                            ". ");

                byte[] payload = datagram.getPayload();

                if (payload == null) {
                    Logger.getLogger("log").log(USR.STDOUT, "No payload");
                } else {
                    Logger.getLogger("log").log(USR.STDOUT, new String(payload));
                }
                Logger.getLogger("log").log(USR.STDOUT, "\n");

                count++;
            }
            }
        } catch (CaptureException se) {
            Logger.getLogger("log").log(USR.ERROR, se.getMessage());
        }


    }

}
