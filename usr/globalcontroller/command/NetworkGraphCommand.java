package usr.globalcontroller.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.output.OutputNetwork;
import usr.protocol.MCRP;

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
        super(MCRP.NETWORK_GRAPH.CMD);
    }

    /**
     * Evaluate the Command.
     */
    @Override
	public boolean evaluate(Request request, Response response) {
        String graphStyle = null;

        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);

            String [] args = value.split(" ");

            if (args.length == 2) {
                graphStyle = args[1];
            } else {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Expected 1 argument for NetworkGraphCommand");

                out.println(jsobj.toString());
                response.close();

                return false;
            }


            // allocate PrintStream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);


            // get the network in the PrintStream
            OutputNetwork on = new OutputNetwork();

            on.visualizeNetworkGraph(graphStyle, ps, controller);

            // convert the ByteArrayOutputStream to a String
            String theString = baos.toString();

            // now send it as a response
            JSONObject jsobj = new JSONObject();

            jsobj.put("graph", theString);
            out.println(jsobj.toString());
            response.close();

            return true;
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        return false;


    }

}