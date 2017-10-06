package usr.dcap;

import usr.net.Address;
import usr.net.Datagram;
import usr.net.DatagramPatch;
import usr.net.DatagramFactory;
import usr.router.NetIF;
import usr.router.DatagramCapture;
import usr.logging.Logger;
import usr.logging.USR;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.nio.ByteBuffer;

public class DcapListener implements DatagramCapture {
    // The NetIF we are capturing
    NetIF netIF;

    // The queue take thread
    Thread takeThread;

    // The queue to take from
    LinkedBlockingQueue<Datagram> queue;

    int timeout = 0;

    boolean isClosed = false;

    DcapListener(NetIF netIF) {
        queue = new LinkedBlockingQueue<Datagram>();

        // bind a DatagramCapture to the FabricDevice of the NetIF
        netIF.getFabricDevice().addDatagramCaptureListener(this);

    }

    /**
     * The DatagramCapture object is the passed in Datagram.
     */
    public boolean sendDatagram(Datagram dg) {
        if (isClosed) {
            // it's closed so no need to copy
            return false;
        } else {
            queue.add(dg);

            return true;
        }
    }


    private int checksum(Datagram dg) {
        System.out.println("TOP checksum dg = " + dg);
        byte[] header = dg.getHeader();
        byte[] data = dg.getData();

        int sum = 0;

        for (int b=0; b<header.length; b++) {
            sum += header[b];
        }

        for (int b=0; b<data.length; b++) {
            sum += data[b];
        }

        System.out.println("BOTTOM checksum dg = " + dg);

        return sum;

    }


    public Datagram receive() throws CaptureException {
        if (isClosed) {
            throw new CaptureException("DatagramCapture closed");
        }

        takeThread = Thread.currentThread();
        try {
            if (timeout == 0) {
                return queue.take();
            } else {
                Datagram obj = queue.poll(timeout, TimeUnit.MILLISECONDS);

                if (obj != null) {
                    return obj;
                } else {
                    throw new CaptureException("timeout: " + timeout);
                }
            }
        } catch (InterruptedException ie) {
            if (isClosed) {
                Logger.getLogger("log").logln(USR.STDOUT, "DatagramCapture closed on shutdown");
                return null;
            } else {
                Logger.getLogger("log").logln(USR.ERROR, "DatagramCapture receive interrupted");
                throw new CaptureException("DatagramCapture receive interrupted");
            }
        }

    }

    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Close this Dcap.
     */
    public boolean close() {
        if (!isClosed) {

            // bind a DatagramCapture to the FabricDevice of the NetIF
            netIF.getFabricDevice().removeDatagramCaptureListener(this);

            isClosed = true;


            if (takeThread != null) {
                takeThread.interrupt();
            }

            return true;
        } else {
            return false;
        }
    }


}
