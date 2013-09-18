// SimpleConsumer.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.monitoring.appl;

import java.util.Scanner;

import plugins_usr.monitoring.distribution.USRDataPlaneConsumerWithNames;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.net.SocketAddress;
import eu.reservoir.monitoring.appl.BasicConsumer;

/**
 * This receives measurements from a USR Data Plane.
 * It is run as an Application.
 */
public class SimpleConsumer implements Application
{
// The Basic consumer
BasicConsumer consumer;

// The port to listen on
int dataPort;

/**
 * Construct a SimpleConsumer using USR
 */
public SimpleConsumer(){
}

/**
 * Init.
 */
@Override
public ApplicationResponse init(String[] args){
    if (args.length == 1) {
        Scanner sc = new Scanner(args[0]);
        dataPort = sc.nextInt();
        sc.close();
        return new ApplicationResponse(true, "");
    } else {
        return new ApplicationResponse(false,
            "usage: SimpleConsumer port");
    }
}

/**
 * Start.
 */
@Override
public ApplicationResponse start(){
    try {
        // set up a BasicConsumer
        consumer = new BasicConsumer();

        // set up multicast address for data
        SocketAddress address = new SocketAddress(dataPort);

        // set up data plane
        consumer.setDataPlane(new USRDataPlaneConsumerWithNames(
                address));

        consumer.connect();

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
    consumer.disconnect();

    synchronized (this) {
        notifyAll();
    }

    return new ApplicationResponse(true, "");
}

@Override
public void run(){
    try {
        synchronized (this) {
            wait();
        }
    } catch (InterruptedException ie) {
    }
}
}