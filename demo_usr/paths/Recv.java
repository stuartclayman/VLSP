package demo_usr.paths;

import java.net.SocketException;
import java.util.Scanner;

import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.net.DatagramSocket;

/**
 * An application for Receiving some data
 */
public class Recv implements Application {
    int port = 0;

    int count = 0;

    boolean running = false;
    DatagramSocket inSocket = null;
    DatagramSocket outSocket = null;

    /**
     * Constructor for Recv
     */
    public Recv() {
    }

    /**
     * Initialisation for Recv.
     * Recv port
     */
    @Override
	public ApplicationResponse init(String[] args) {
        if (args.length == 1) {
            // try port
            Scanner scanner = new Scanner(args[0]);
            if (scanner.hasNextInt()) {
                port = scanner.nextInt();
                scanner.close();
            } else {
            	scanner.close();
                return new ApplicationResponse(false, "Bad port " + args[1]);
            }
            scanner.close();
            return new ApplicationResponse(true, "");

        } else {
            return new ApplicationResponse(false, "Usage: Recv port");
        }
    }

    /** Start application with argument  */
    @Override
	public ApplicationResponse start() {
        try {
            // set up inbound socket
            inSocket = new DatagramSocket();

            inSocket.bind(port);

            // set up outbound socket
            // don't bind() or connect() so we can set src and dst addr and port
            outSocket = new DatagramSocket();


        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot open socket " + e.getMessage());
            return new ApplicationResponse(false,  "Cannot open socket " + e.getMessage());
        }

        running = true;

        return new ApplicationResponse(true, "");
    }

    /** Implement graceful shut down */
    @Override
	public ApplicationResponse stop() {
        running = false;

        if (inSocket != null) {
            inSocket.close();
        }

        if (outSocket != null) {
            outSocket.close();
        }

        Logger.getLogger("log").logln(USR.STDOUT, "Recv stop");

        return new ApplicationResponse(true, "");
    }

    /** Run the ping application */
    @Override
	public void run()  {
        Datagram inDatagram;

        try {
            while ((inDatagram = inSocket.receive()) != null) {

                Logger.getLogger("log").log(USR.STDOUT, count + ". ");

                /*
                Logger.getLogger("log").log(USR.STDOUT, "HL: " + datagram.getHeaderLength() +
                                            " TL: " + datagram.getTotalLength() +
                                            " From: " + datagram.getSrcAddress() +
                                            " To: " + datagram.getDstAddress() +
                                            ". ");
                */


                PathLabelledDatagram pldg = PathLabelledDatagram.fromDatagram(inDatagram);

                // lets check the fields
                Address srcAddr = pldg.getSrcAddress();
                int srcPort = pldg.getSrcPort();
                Address dstAddr = pldg.getDstAddress();
                int dstPort = pldg.getDstPort();

                byte[] data = pldg.getPayload();

                // now create a new Datagram
                Datagram outDatagram = DatagramFactory.newDatagram(data);

                // and reconstitute the original src and dst addresses
                outDatagram.setSrcAddress(srcAddr);
                outDatagram.setSrcPort(srcPort);
                outDatagram.setDstAddress(dstAddr);
                outDatagram.setDstPort(dstPort);

                try {
                    outSocket.send(outDatagram);
                } catch (Exception e) {
                    if (outSocket.isClosed()) {
                        break;
                    } else {
                        Logger.getLogger("log").logln(USR.STDOUT, "Cant send: " + outDatagram);
                    }
                }

                // Logger.getLogger("log").log(USR.STDOUT, "\n");

                count++;
            }
        } catch (SocketException se) {
            Logger.getLogger("log").log(USR.ERROR, se.getMessage());
        }

        Logger.getLogger("log").logln(USR.STDOUT, "Recv: end of run()");


    }

}
