package demo_usr.paths;

import usr.net.*;
import usr.logging.*;
import usr.applications.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.net.UnknownHostException;
import java.net.SocketException;
import usr.net.ClosedByInterruptException;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.CountDownLatch;

/**
 * This class reads USR Datagrams
 * and sends them to a queue to be processed for some management job.
 */
public class ManagementPort extends USRReader {
    public ManagementPort(int usrPort, LinkedBlockingDeque<usr.net.Datagram> queue, int verbose) throws Exception {
        super(usrPort, queue, verbose);
    }

}
