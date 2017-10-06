package usr.dcap;

import usr.router.Router;
import usr.router.RouterDirectory;
import usr.router.NetIF;
import usr.net.Address;
import usr.net.Datagram;
import usr.net.SocketTimeoutException;
import java.net.SocketException;
import java.util.List;
import java.util.ArrayList;

/**
 * The Dcap class will do Datagram capture from a NetIF.
 * It intercepts all the packets and they become available
 * to the reader.
 * 
 * Similar to pcap4j:
 * PcapNetworkInterface nif = PcapNetworkInterface.getDevByAddress(addr);
 */
public class Dcap {

    NetIF netIF;

    int snapLen;

    DcapListener captureImpl;

    Dcap(DcapNetworkInterface dcapNIF, int snapLen) {
        this.netIF = dcapNIF.netIF;
        this.snapLen = snapLen;

        captureImpl = new DcapListener(netIF);
    }


    public Datagram receive() throws CaptureException {
        return captureImpl.receive();
    }

    public boolean isClosed() {
        return captureImpl.isClosed();
    }


    /**
     * Close this Dcap.
     */
    public boolean close() {
        return captureImpl.close();
    }


}
