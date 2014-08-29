package usr.applications;

import java.util.Scanner;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.net.DatagramSocket;

/**
 * An application for Sending some data
 */
public class Send implements Application {
    Address addr = null;
    int port = 0;
    int count = 0;
    int interPacketDelay = 0;
    int startDelay = 0;
    int sendSize = 0;
    int burstSize = 1;

    boolean running = false;
    DatagramSocket socket = null;

    /**
     * Constructor for Send
     */
    public Send() {
    }

    /**
     * Initialisation for Send.
     * Send address port count [optionals]
     * Optional args:
     * -i inter send delay (in milliseconds) (default: 0)
     * -d start-up delay (in milliseconds) (default: 0)
     * -s size of send buffer (in bytes)
     * -b burst size - no of packets to send between each delay (default: 1) 
     */
    @Override
	public ApplicationResponse init(String[] args) {
        if (args.length >= 3) {
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
                scanner.close();
            } else {
            	scanner.close();
                return new ApplicationResponse(false, "Bad port " + args[1]);
            }

            // try count
            scanner = new Scanner(args[2]);

            if (scanner.hasNextInt()) {
                count = scanner.nextInt();
                scanner.close();
            } else {
            	scanner.close();
                return new ApplicationResponse(false, "Bad count " + args[2]);
            }

            Logger.getLogger("log").logln(USR.STDOUT, "Send addr: " + addr + " port: " + port + " count: " + count);

            if (args.length == 3) {
                return new ApplicationResponse(true, "");
            } else {
                // try and process extra args
                for (int extra = 3; extra < args.length; extra++) {
                    String thisArg = args[extra];

                    // check if its a flag
                    if (thisArg.charAt(0) == '-') {
                        // get option
                        char option = thisArg.charAt(1);

                        // gwet next arg
                        String argValue = args[++extra];

                        switch (option) {
                        case 'i': {
                            try {
                                interPacketDelay = Integer.parseInt(argValue);
                            } catch (Exception e) {
                                return new ApplicationResponse(false, "Bad interPacketDelay " + argValue);
                            }

                            break;
                        }

                        case 'd': {
                            try {
                                startDelay = Integer.parseInt(argValue);
                            } catch (Exception e) {
                                return new ApplicationResponse(false, "Bad startDelay " + argValue);
                            }

                            break;
                        }

                        case 's': {
                            try {
                                sendSize = Integer.parseInt(argValue);
                            } catch (Exception e) {
                                return new ApplicationResponse(false, "Bad sendSize " + argValue);
                            }

                            break;
                        }

                        case 'b': {
                            try {
                                burstSize = Integer.parseInt(argValue);
                            } catch (Exception e) {
                                return new ApplicationResponse(false, "Bad burstSize " + argValue);
                            }

                            break;
                        }



                        default:
                            return new ApplicationResponse(false, "Bad option " + option);
                        }
                    }
                }

                return new ApplicationResponse(true, "");

            }

        } else {
            return new ApplicationResponse(false, "Usage: Send address port count");
        }
    }

    /** Start application with argument  */
    @Override
	public ApplicationResponse start() {
        try {
            // set up socket
            socket = new DatagramSocket();

            socket.connect(addr, port);

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

            Logger.getLogger("log").logln(USR.STDOUT, "Send stop");
        }

        return new ApplicationResponse(true, "");
    }

    /** Run the Send application */
    @Override
	public void run() {
        Datagram datagram = null;

        // Start Delay
        if (startDelay > 0) {
            try {
                Thread.sleep(startDelay);
            } catch (Exception e) {
            }
        }

        // a buffer for data
        byte [] buffer;
        int thisBurst = 0;

        for (int i = 0; i < count; i++) {

            if (sendSize == 0) {
                buffer = ("line " + i).getBytes();
            } else {
                buffer = new byte[sendSize];
            }

            datagram = DatagramFactory.newDatagram(buffer);

            try {
                socket.send(datagram);

                // burst
                thisBurst++;

                // Inter Packet Delay
                if (thisBurst == burstSize && interPacketDelay > 0) {
                    Thread.sleep(interPacketDelay);
                    thisBurst = 0;
                }


            } catch (Exception e) {
                if (socket.isClosed()) {
                    break;
                } else {
                    Logger.getLogger("log").logln(USR.STDOUT, "Cant send: " + e + " " + datagram + " with " + new String(datagram.getPayload()));
                }
            }

        }

        Logger.getLogger("log").logln(USR.STDOUT, "Send: end of run()");


    }

}
