package usr.applications;

import java.nio.ByteBuffer;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.net.DatagramSocket;

/**
 * An application for transfer of data at a given rate
 */
public class Transfer implements Application {
    Address addr_ = null;  // Address to send to
    int port_ = 0;   // Port to send to
    int bytes_ = 0;   // Bytes to send
    double rate_ = 0;  // Rate in bytes per second

    boolean running = false;
    DatagramSocket socket = null;

    /**
     * Constructor for Send
     */
    public Transfer() {
    }

    /**
     * Initialisation for Send.
     * Send address port count
     */
    @Override
	public ApplicationResponse init(String[] args) {
        if (args.length != 4) {
            return new ApplicationResponse(false, "Need arguments addr port, bytes, rate");
        }
        try {
            addr_ = AddressFactory.newAddress(args[0]);
            port_ = new Integer(args[1]);
            bytes_ = new Integer(args[2]);
            rate_ = new Double(args[3]);

            if (port_ <= 0 || bytes_ <= 0 || rate_ < 0) {
                return new ApplicationResponse
                           (false, "Need +ve numerical arguments port, bytes, rate");
            }
        } catch (java.lang.NumberFormatException e) {
            return new ApplicationResponse(false, "Need numerical arguments port, bytes, rate");
        } catch (java.net.UnknownHostException e) {
            return new ApplicationResponse(false, "Cannot construct address from "+args[0]);
        }

        return new ApplicationResponse(true, "");
    }

    /** Start application with argument  */
    @Override
	public ApplicationResponse start() {
        try {
            // set up socket
            socket = new DatagramSocket();
            socket.connect(addr_, port_);
            // Logger.getLogger("log").logln(USR.ERROR, "Socket has source port "+socket.getLocalPort());
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot open socket " + e.getMessage());
            return new ApplicationResponse(false, "Cannot open socket " + e.getMessage());
        }

        running = true;

        return new ApplicationResponse(true, "");
    }

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
        Datagram datagram = null;
        int MTU = 1500;
        byte [] blank = new byte[MTU];

        for (int i = 0; i< MTU; i++) {
            blank[i] = 0;
        }
        long interval = (long)(MTU*1000/rate_);
        //System.err.println("Interval "+interval+ " rate "+rate_);
        long nextSendTime = System.currentTimeMillis()+interval;

        while (bytes_ > 0 && running) {
            int packetLen = MTU;

            if (packetLen > bytes_) {
                packetLen = bytes_;
            }

            ByteBuffer buffer = ByteBuffer.allocate(packetLen);
            buffer.put(blank, 0, packetLen);

            datagram = DatagramFactory.newDatagram(buffer);

            try {
                socket.send(datagram);
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.STDOUT, "Cant send: "
                                              + datagram + " with " + new String(datagram.getPayload()));
            }

            bytes_ -= packetLen;
            long delay = nextSendTime-System.currentTimeMillis();

            //System.err.println("To send "+bytes_+ " delay "+delay);
            if (delay > 0) {
                try {
                    synchronized (this) {
                        this.wait(delay);
                    }
                } catch (InterruptedException e) {
                }
            }
            nextSendTime += interval;

        }
        closeDown();

    }

}