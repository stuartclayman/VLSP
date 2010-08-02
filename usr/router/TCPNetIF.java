package usr.router;

import usr.net.Address;
import usr.net.IPV4Address;
import usr.net.Datagram;
import usr.net.DatagramConnection;
import java.io.*;
import java.net.*;
import java.util.Map;

/**
 * A Network Interface for a Router using TCP
 */
public class TCPNetIF implements NetIF {
    // The connection
    DatagramConnection connection;

    // The name
    String name;

    // The weight
    int weight;

    // int ID
    int id;

    // Remote router name
    String routerName;

    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(Socket s) {
        connection = new DatagramConnection(s);
    }

    /**
     * Get the name of this NetIF.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this NetIF.
     */
    public NetIF setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the weight of this NetIF.
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Set the weight of this NetIF.
     */
    public NetIF setWeight(int w) {
        weight = w;
        return this;
    }

    /**
     * Get the ID of this NetIF.
     */
    public int getID() {
        return id;
    }

    /**
     * Set the ID of this NetIF.
     */
    public NetIF setID(int id) {
        this.id = id;
        return this;
    }


    /**
     * Get the Address for this connection.
     */
    public Address getAddress() {
        return connection.getAddress();
    }

    /**
     * Set the Address for this connection.
     */
    public NetIF setAddress(Address addr) {
        connection.setAddress(addr);
        return this;
    }


    /**
     * Get the name of the remote router this NetIF is connected to.
     */
    public String getRemoteRouterName() {
        return routerName;
    }


    /**
     * Set the name of the remote router this NetIF is connected to.
     */
    public NetIF setRemoteRouterName(String name) {
        routerName = name;
        return this;
    }

    /**
     * Get the socket.
     */
    Socket getSocket() {
         return connection.getSocket();
    }

    /**
     * Send a Datagram.
     */
    public boolean sendDatagram(Datagram dg) {
        return connection.sendDatagram(dg);
    }

    /**
     * Read a Datagram.
     */
    public Datagram readDatagram() {
        return connection.readDatagram();
    }

    /**
     * Get the interface stats.
     * A map of values like:
     * "in_bytes" -> in_bytes
     * "in_packets" -> in_packets
     * "in_errors" -> in_errors
     * "in_dropped" -> in_dropped
     * "out_bytes" -> out_bytes
     * "out_packets" -> out_packets
     * "out_errors" -> out_errors
     * "out_dropped" -> out_dropped
     */
    public Map<String, Number> getStats() {
        return null;
    }

    /**
     * To String
     */
    public String toString() {
        return "TCPNetIF: " + getName() + " @ " + connection.toString();
    }
}
