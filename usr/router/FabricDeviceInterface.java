package usr.router;

import usr.net.*;
import usr.logging.*;
import usr.protocol.Protocol;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/** A Fabric Device takes in packets from a DatagramDevice and
   eventually outputs them to a DatagramDevice -- the packets should
   eventually be sent to the outQueueHandler method of a DatagramDevice
 */
public interface FabricDeviceInterface {

    /** Start the device */
    public void start();

    /** Stop the device*/
    public void stop();

    /** Takes a datagram from a datagram device and returns once the datagram
       is inserted onto the fabric */
    public boolean blockingAddToInQueue(Datagram dg, DatagramDevice dd)
    throws java.net.NoRouteToHostException;

    /** Takes a datagram from a datagram device and immediately
        either returns true (inserted)
        or false (dropped) */
    public boolean addToInQueue(Datagram dg, DatagramDevice dd)
    throws java.net.NoRouteToHostException;

    /** Takes a datagram from a datagram device and either returns true
       (inserted) or false (dropped/blocked) -- if the queue is blocking
       a notify will be sent to waitObj when there is room in the queue*/
    //public boolean addToInQueue(Datagram dg, DatagramDevice dd, Object waitObj)
    //throws java.net.NoRouteToHostException, usr.net.InterfaceBlockedException;

}