package usr.globalcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.globalcontroller.*;
import java.nio.channels.SocketChannel;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import usr.output.*;

/**
 * A NetworkGraphCommand
 */
public class NetworkGraphCommand extends GlobalCommand {
    /**
     * Construct a NetworkGraphCommand.
     * Process arg, it is a string to pass on.
     *
     */
    public NetworkGraphCommand() {
        super(MCRP.NETWORK_GRAPH.CMD, MCRP.NETWORK_GRAPH.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        String graphStyle = null;

        String [] args= req.split(" ");
        if (args.length == 2) {
            graphStyle = args[1];
        } else {
            error ("Expected 1 argument for NetworkGraphCommand");
            return false;
        }


        // allocate PrintStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);


        // get the network in the PrintStream
        OutputNetwork on= new OutputNetwork();
        
        on.visualizeNetworkGraph(graphStyle, ps,controller);

        // convert the ByteArrayOutputStream to a String
        String theString = baos.toString();

        // now send it as a response
        respond(theString);
        respond(".");

        success("");
        return true;
    }

}
