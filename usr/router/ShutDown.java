package usr.router;

import usr.protocol.MCRP;
import usr.interactor.RouterInteractor;
import usr.interactor.MCRPException;
import usr.console.*;
import usr.net.*;
import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;


/**
 * A EndLink ends connection between two links
 */
public class ShutDown extends ChannelResponder implements Runnable {
    RouterController controller;
    Request request;

    /**
     * Create a new connection.
     * CREATE_CONNECTION ip_addr/port connection_weight - create a new network
     * interface to a router on the address ip_addr/port with a 
     * connection weight of connection_weight
     */
    public ShutDown(RouterController controller, Request request) {
        this.controller = controller;
        this.request = request;
        setChannel(request.channel);
    }

    public void run() {
        // process the request
        System.out.println(leadin()+"Sending response code for shut down"+MCRP.SHUT_DOWN.CODE);
        
        
      
        controller.shutDown();
        respond(MCRP.SHUT_DOWN.CODE + " SHUTTING DOWN ROUTER");
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String EL = "SD: ";

        if (controller == null) {
            return EL;
        } else {
            return controller.getName() + " " + EL;
        }

    }


}
