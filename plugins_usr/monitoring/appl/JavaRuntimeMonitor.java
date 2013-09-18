// JavaRuntimeMonitor.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.monitoring.appl;

import java.util.Scanner;

import plugins_usr.monitoring.distribution.USRDataPlaneProducerWithNames;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.net.Address;
import usr.net.GIDAddress;
import usr.net.SocketAddress;
import eu.reservoir.demo.JavaMemoryProbe;
import eu.reservoir.monitoring.appl.BasicDataSource;
import eu.reservoir.monitoring.appl.datarate.EveryNSeconds;
import eu.reservoir.monitoring.core.DataSource;
import eu.reservoir.monitoring.core.Probe;

/**
 * This monitor sends java runtime data  uses a USR Data Plane.
 * It runs as an Application.
 */
public class JavaRuntimeMonitor implements Application
{
// The DataSource
DataSource ds;

// The Probe
Probe p;

// The Address to send to
Address addr;

// The port to send to
int dataPort;

/*
 * Construct a JavaRuntimeMonitor
 */
public JavaRuntimeMonitor(){
}

/**
 * Init.
 */
@Override
public ApplicationResponse init(String[] args){
    if (args.length == 2) {
        Scanner sc = new Scanner(args[0]);
        addr = new GIDAddress(sc.nextInt());
        sc.close();
        sc = new Scanner(args[1]);
        dataPort = sc.nextInt();
        sc.close();

        return new ApplicationResponse(true, "");
    } else {
        return new ApplicationResponse(
            false, "usage: JavaRuntimeMonitor GID-address port");
    }
}

/**
 * Start.
 */
@Override
public ApplicationResponse start(){
    try {
        // set up data source
        ds = new BasicDataSource();

        // set up socket address for data
        SocketAddress address = new SocketAddress(addr, dataPort);

        // set up data plane
        ds.setDataPlane(new USRDataPlaneProducerWithNames(address));

        ds.connect();

        // set up probe
        p = new JavaMemoryProbe("localhost" + ".javaMemory");
        p.setDataRate(new EveryNSeconds(5));

        ds.addProbe(p);
        ds.turnOnProbe(p);

        return new ApplicationResponse(true, "");
    } catch (Exception e) {
        return new ApplicationResponse(false, e.getMessage());
    }
}

/**
 * Stop.
 */
@Override
public ApplicationResponse stop(){
    ds.deactivateProbe(p);
    ds.removeProbe(p);

    ds.disconnect();

    synchronized (this) {
        notifyAll();
    }

    return new ApplicationResponse(true, "");
}

/**
 * Run
 */
@Override
public void run(){
    // A DataSource already runs in itws own thread
    // so this one can wait and do nothing.
    try {
        synchronized (this) {
            wait();
        }
    } catch (InterruptedException ie) {
    }
}
}