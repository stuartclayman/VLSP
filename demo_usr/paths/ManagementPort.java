package demo_usr.paths;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * This class reads USR Datagrams
 * and sends them to a queue to be processed for some management job.
 */
public class ManagementPort extends USRReader {
    public ManagementPort(int usrPort, LinkedBlockingDeque<usr.net.Datagram> queue, int verbose) throws Exception {
        super(usrPort, queue, verbose);
    }

}
