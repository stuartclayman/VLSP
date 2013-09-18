package usr.applications;

import java.net.SocketException;
import java.util.Scanner;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;
import usr.net.DatagramSocket;

/**
 * An application for Receiving a specific amount of data
 */
public class Receive implements Application {
    int port_ = 0;

    int bytes_ = 0;
    int count_ = 0;

    boolean running = false;
    DatagramSocket socket = null;

    /**
     * Constructor for Recv
     */
    public Receive() {
    }

    /**
     * Initialisation for Receive.
     * Receive port
     */
    @Override
	public ApplicationResponse init(String[] args) {
        if (args.length == 2) {
            // try port
            Scanner scanner = new Scanner(args[0]);

            if (scanner.hasNextInt()) {
                port_ = scanner.nextInt();
                scanner.close();
            } else {
            	scanner.close();
                return new ApplicationResponse(false, "Bad port " + args[0]);
            }
            bytes_ = Integer.parseInt(args[1]);

            if (bytes_  <= 0) {
                return new ApplicationResponse(false, "Cannot parse "+args[1]+" as no of bytes");
            }
            return new ApplicationResponse(true, "");

        } else {
            return new ApplicationResponse(false, "Usage: Receive port bytes");
        }
    }

    /** Start application with argument  */
    @Override
	public ApplicationResponse start() {
        try {
            // set up socket
            socket = new DatagramSocket();

            socket.bind(port_);

            // Logger.getLogger("log").logln(USR.ERROR, "Socket has source port "+socket.getLocalPort());

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot open socket " + e.getMessage());
            return new ApplicationResponse(false, "Cannot open socket " + e.getMessage());
        }

        running = true;

        return new ApplicationResponse(true, "");
    }

    /** Do necessary things to close application */
    private void closeDown() {
        if (socket != null) {
            socket.close();
        }
    }

    /** Implement graceful shut down */
    @Override
	public ApplicationResponse stop() {
        running = false;
        closeDown();
        return new ApplicationResponse(true, "");
    }

    /** Run the ping application */
    @Override
	public void run() {
        Datagram datagram;

        try {
            while ((datagram = socket.receive()) != null) {

                count_ += (datagram.getTotalLength()-datagram.getHeaderLength());

                if (count_ >= bytes_) {
                    Logger.getLogger("log").log(USR.STDOUT, "Received all bytes on connection");
                    running = false;
                    break;
                }
            }
            //closeDown();
        } catch (SocketException se) {
        }

    }

}