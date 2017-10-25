package demo_usr.nfv;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;
import usr.protocol.Protocol;


public class NetPrint extends NetFn  {


    /**
     * Constructor for NetPrint
     */
    public NetPrint() {
    }


    /**
     * Process the recevied Datagram
     */
    /**
     * The callback for when a Datagram is received by an Intercepter.
     * Return true to forward Datagram, Return false to throw it away.
     */
    public boolean datagramProcess(InterceptListener intercepter, Datagram datagram) {
        if (datagram.getProtocol() == Protocol.CONTROL) {
            Logger.getLogger("log").log(USR.STDOUT, "INTERCEPT: " + "CONTROL" + ". \n");
            return false;
            
        } else {
            Logger.getLogger("log").log(USR.STDOUT, "INTERCEPT: " + count + ". ");
            Logger.getLogger("log").log(USR.STDOUT, intercepter.getName() + " ");
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

            return true;
        }
    }

}
