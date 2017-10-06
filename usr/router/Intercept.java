package usr.router;

import usr.router.Router;
import usr.router.RouterDirectory;
import usr.router.NetIF;
import usr.router.InterceptNetIF;
import usr.net.Address;
import usr.net.Datagram;
import usr.net.SocketTimeoutException;
import usr.net.NetworkException;
import java.net.SocketException;
import java.net.NoRouteToHostException;
import java.util.List;
import java.util.ArrayList;


/**
 * The Intercept class will do Datagram capture from a NetIF.
 * It intercepts all the packets and they become available
 * to the reader.
 * 
 * Similar to pcap4j:
 * PcapNetworkInterface nif = PcapNetworkInterface.getDevByAddress(addr);
 */
public class Intercept {

    NetIF netIF;

    int snapLen;

    InterceptNetIF captureImpl;

    public Intercept(NetIF dcapNIF) {
        this(dcapNIF, 65536);
    }

    public Intercept(NetIF dcapNIF, int snapLen) {
        this.netIF = dcapNIF;
        this.snapLen = snapLen;

        captureImpl = new InterceptNetIF(netIF);
        captureImpl.start();

        // bind a DatagramCapture to the FabricDevice of the NetIF
        netIF.getFabricDevice().setDatagramIntercepter(captureImpl);

    }


    public Datagram receive() throws NetworkException {
        return captureImpl.receive();
    }


    public boolean send(Datagram dg) throws NoRouteToHostException {
        return captureImpl.enqueueDatagram(dg);        
    }

    public boolean isClosed() {
        return captureImpl.isClosed();
    }


    /**
     * Close this Intercept
     */
    public boolean close() {
        captureImpl.stop();
        captureImpl.close();
        netIF.getFabricDevice().setDatagramIntercepter(null);
        return true;
    }


}
