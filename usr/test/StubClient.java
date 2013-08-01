package usr.test;

import usr.net.*;
import usr.logging.*;
import usr.logging.*;
import usr.router.NetIF;
import usr.router.TCPNetIF;
import usr.router.NetIFListener;
import usr.router.FabricDevice;
import usr.router.DatagramDevice;
import usr.protocol.Protocol;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class StubClient implements NetIFListener {
    final static int PORT_NUMBER = 4433;
    ConnectionOverTCP connection;
    NetIF netIF;
    Logger logger;

    public StubClient(String host, int port) {
        try {
            logger = Logger.getLogger("log");
            // tell it to output to stdout
            // and tell it what to pick up
            // it will actually output things where the log has bit 1 set
            logger.addOutput(System.out, new BitMask(USR.STDOUT));
            // tell it to output to stderr
            // and tell it what to pick up
            // it will actually output things where the log has bit 2 set
            logger.addOutput(System.err, new BitMask(USR.ERROR));
            // initialise socket
            TCPEndPointSrc src = new TCPEndPointSrc(host, port);

            netIF = new TCPNetIF(src, this);
            netIF.setAddress(new GIDAddress(1));

            netIF.setName("StubClient");
            netIF.setRemoteRouterAddress(new GIDAddress(555));
            netIF.connect();
            int i;

            Logger.getLogger("log").logln(USR.ERROR, "StubClient: Connected to: " + host);


        } catch (UnknownHostException uhe) {
            Logger.getLogger("log").logln(USR.ERROR, "StubClient: Unknown host " + host);
            System.exit(1);
        } catch (IOException ioexc) {
            Logger.getLogger("log").logln(USR.ERROR, "StubClient: Cannot connect to " + host + "on port " + port);
            System.exit(1);
        }
    }

    /**
     * Write stuff
     */
    void writeALot(int count) {
        Datagram datagram = null;

        for (int i = 0; i < count; i++) {
            String line = "line " + i;
            ByteBuffer buffer = ByteBuffer.allocate(line.length());
            buffer.put(line.getBytes());

            datagram = DatagramFactory.newDatagram(buffer);


            datagram.setDstAddress(netIF.getRemoteRouterAddress());
            datagram.setDstPort(3333);
            try {
                if (netIF.sendDatagram(datagram) == false) {
                    Logger.getLogger("log").logln(USR.ERROR, "Failed: " + datagram + " with " + new String(datagram.getPayload()));
                }
            } catch (NoRouteToHostException e) {
                Logger.getLogger("log").logln(USR.ERROR, "No route to host");
            }
        }

        netIF.close();
    }

    /** Deal with TTL expire */
    public void TTLDrop(Datagram dg) {
    }

    /** A datagram device has closed and must be removed */
    public void closedDevice(DatagramDevice dd) {

    }

    /**
     * Fake interface
     */
    public FabricDevice getRouteFabric(Datagram d) {
        return netIF.getFabricDevice();
    }

    /** Client does not accept incoming traffic and has no address */
    public boolean ourAddress(Address a) {
        return false;
    }

    /**
     * A NetIF has a datagram.
     */
    public boolean datagramArrived(NetIF netIF, Datagram datagram) {
        return true;
    }

    /**
     * get name
     */
    public String getName() {
        return netIF.getName();
    }

    public static void main(String[] args) throws IOException {
        int count = 100;
        int port = PORT_NUMBER;

        if (args.length == 1) {
            // get no of writes
            Scanner scanner = new Scanner(args[0]);

            count = scanner.nextInt();
        }

        if (args.length == 2) {
            // get no of writes
            Scanner scanner = new Scanner(args[1]);

            port = scanner.nextInt();
        }

        StubClient client = new StubClient("localhost", port);
        client.writeALot(count);
    }

}