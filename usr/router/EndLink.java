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
public class EndLink extends ChannelResponder implements Runnable {
    RouterController controller;
    Request request;

    /**
     * Create a new connection.
     * CREATE_CONNECTION ip_addr/port connection_weight - create a new network
     * interface to a router on the address ip_addr/port with a 
     * connection weight of connection_weight
     */
    public EndLink(RouterController controller, Request request) {
        this.controller = controller;
        this.request = request;
        setChannel(request.channel);
    }

    public void run() {
        // process the request
        String value = request.value;
        SocketChannel channel = request.channel;

        // check command
        String[] parts = value.split(" ");
        if (parts.length != 2) {
            System.err.println(leadin() + "INVALID END_LINK command: " + request);
            respond(MCRP.END_LINK.ERROR + " END_LINK wrong no of args");
            return;
        }
        String rId= parts[1];
        
        NetIF netif= controller.findNetIF(rId);
        if (netif == null) {
            respond(MCRP.END_LINK.ERROR + " END_LINK cannot find link to "+rId);
            return;
        } 
        controller.removeNetIF(netif);
        respond(MCRP.END_LINK.CODE + " END_LINK to " + rId);
        
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String EL = "EL: ";

        if (controller == null) {
            return EL;
        } else {
            return controller.getName() + " " + EL;
        }

    }


}
