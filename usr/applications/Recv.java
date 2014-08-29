package usr.applications;

import java.net.SocketException;
import java.util.Scanner;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;
import usr.net.DatagramSocket;
import java.nio.ByteBuffer;

/**
 * An application for Receiving some data
 */
public class Recv implements Application {
    int port = 0;

    int count = 0;

    boolean running = false;
    DatagramSocket socket = null;

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

            return new ApplicationResponse(true, "");

        } else {
            return new ApplicationResponse(false, "Usage: Recv port");
        }
    }

    /** Start application with argument  */
    @Override
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

        running = true;

        return new ApplicationResponse(true, "");
    }

    /** Implement graceful shut down */
    @Override
	public ApplicationResponse stop() {
        running = false;

        if (socket != null) {
            socket.close();

            Logger.getLogger("log").logln(USR.STDOUT, "Recv stop");
        }

        return new ApplicationResponse(true, "");
    }

    /** Run the ping application */
    @Override
	public void run() {
        Datagram datagram;

        try {
            while ((datagram = socket.receive()) != null) {

                Logger.getLogger("log").log(USR.STDOUT, count + ". ");
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
        } catch (SocketException se) {
            Logger.getLogger("log").log(USR.ERROR, se.getMessage());
        }


    }

}
