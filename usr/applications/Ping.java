package usr.applications;

import java.net.SocketException;
import java.nio.ByteBuffer;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.net.DatagramSocket;
import usr.protocol.Protocol;

/**
 * An application for Ping
 */
public class Ping implements Application {
    Address address = null;
    boolean running_ = false;
    DatagramSocket socket_ = null;



    /**
     * Do a ping
     */
    public Ping() {
    }

    /**
     * Initialisation for ping.
     */
    @Override
	public ApplicationResponse init(String[] argv) throws NumberFormatException {

        if (argv.length != 1) {
            return new ApplicationResponse(false, leadin()+"PING COMMAND REQUIRES ROUTER ADDRESS AS ARGUMENT");
        }
        try {
            address = AddressFactory.newAddress(argv[0]);
            return new ApplicationResponse(true, "");
        } catch (Exception e) {
            return new ApplicationResponse(false, leadin()+"PING COMMAND REQUIRES ROUTER ADDRESS");
        }
    }

    /** Start application with argument  */
    @Override
	public ApplicationResponse start() {
        running_ = true;
        return new ApplicationResponse(true, "");
    }

    /** Implement graceful shut down */
    @Override
	public ApplicationResponse stop() {
        running_ = false;

        if (socket_ != null) {
            socket_.close();
        }

        Logger.getLogger("log").logln(USR.ERROR, "Ping stop");

        return new ApplicationResponse(true, "");
    }

    /** Run the ping application */
    @Override
	public void run() {

        try {
            socket_ = new DatagramSocket();

            Address dst = address;
            // and we want to connect to port 0 (router fabric)
            socket_.connect(dst, 0);

            // Logger.getLogger("log").logln(USR.ERROR, "Socket has source port "+socket_.getLocalPort());

            ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put("P".getBytes());
            Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);
            socket_.send(datagram);

        } catch (SocketException se) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Cannot open socket to write");
            Logger.getLogger("log").logln(USR.ERROR, leadin()+se.getMessage());
            return;
        }

        long now = System.currentTimeMillis();
        Datagram dg;

        while (running_) {

            try {
                dg = socket_.receive();

                if (dg == null) {

                    Logger.getLogger("log").logln(USR.STDOUT, "Ping waiting");
                    continue;
                }
                Logger.getLogger("log").logln(USR.STDOUT,
                                              "Ping received in time: "+ (System.currentTimeMillis()- now) + " milliseconds ");

                return;
            } catch (SocketException se) {
                Logger.getLogger("log").logln(USR.STDOUT, "Ping receive error");
                return;
            }

        }


    }

    String leadin() {
        String li = "PA: ";
        return li;
    }

}