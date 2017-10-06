package usr.router;

import usr.net.Datagram;

/** A Fabric Device takes in packets from a DatagramDevice and
   eventually outputs them to a DatagramDevice -- the packets should
   eventually be sent to the outQueueHandler method of a DatagramDevice
 */
public interface FabricDeviceInterface {

    /** Start the device */
    public void start();

    /** Stop the device*/
    public void stop();

}
