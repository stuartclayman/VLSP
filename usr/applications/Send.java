package usr.applications;

import usr.net.*;
import usr.logging.*;
import java.nio.ByteBuffer;
import java.util.Scanner;

/**
 * An application for Sending some data
 */
public class Send implements Application {
    Address addr = null;
    int port = 0;
    int count = 0;

    boolean running = false;
    DatagramSocket socket = null;

    /**
     * Constructor for Send
     */
    public Send() {
    }

    /**
     * Initialisation for Send.
     * Send address port count
     */
    public ApplicationResponse init(String[] args) {
        if (args.length == 3) {
            // try address
            try {
                addr = AddressFactory.newAddress(args[0]);

            } catch (Exception e) {
                return new ApplicationResponse(false, "UnknownHost " + args[0]);
            }

            // try port
            Scanner scanner = new Scanner(args[1]);
            if (scanner.hasNextInt()) {
                port = scanner.nextInt();
            } else {
                return new ApplicationResponse(false, "Bad port " + args[1]);
            }

            // try count
            scanner = new Scanner(args[2]);
            if (scanner.hasNextInt()) {
                count = scanner.nextInt();
            } else {
                return new ApplicationResponse(false, "Bad count " + args[2]);
            }

            System.err.print("Send addr: " + addr + " port: " + port + " count: " + count);


            return new ApplicationResponse(true, "");

        } else {
            return new ApplicationResponse(false, "Usage: Send address port count");
        }
    }

    /** Start application with argument  */
    public ApplicationResponse start() {
        try {
            // set up socket
            socket = new DatagramSocket();

            socket.connect(addr, port);

            // Logger.getLogger("log").logln(USR.ERROR, "Socket has source port "+socket.getLocalPort());

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot open socket " + e.getMessage());
            return new ApplicationResponse(false,  "Cannot open socket " + e.getMessage());
        }

        running = true;

        return new ApplicationResponse(true, "");
    }

    /** Implement graceful shut down */
    public ApplicationResponse stop() {
        running = false;

        if (socket != null) {
            socket.close();

            Logger.getLogger("log").logln(USR.STDOUT, "Send stop");
        }

        return new ApplicationResponse(true, "");
    }

    /** Run the ping application */
    public void run()  {
        Datagram datagram = null;

        for (int i = 0; i < count; i++) {
            String line = "line " + i;
            ByteBuffer buffer = ByteBuffer.allocate(line.length());
            buffer.put(line.getBytes());

            datagram = DatagramFactory.newDatagram(buffer);

            try {
                socket.send(datagram);

            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.STDOUT, "Cant send: " + datagram + " with " + new String(datagram.getPayload()));
            }

        }

        Logger.getLogger("log").logln(USR.STDOUT, "Send: end of run()");


    }

}
