package usr.router;

import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;



/**
 * A EndLink ends connection between two routers
 */
public class EndLink {
    RouterController controller;
    Request request;
    Response response;

    /**
     * End a link / connection.
     * END_LINK remote_addr
     */
    public EndLink(RouterController controller, Request request, Response response) {
        this.controller = controller;
        this.request = request;
        this.response = response;
    }

    public boolean run() throws IOException, JSONException {
        PrintStream out = response.getPrintStream();

        // get full request string
        String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
        // strip off /command
        String value = path.substring(9);


        // check command
        String[] parts = value.split(" ");

        //Logger.getLogger("log").logln(USR.ERROR, "END LINK ENTRY");
        if (parts.length != 2) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "INVALID END_LINK command: " + request);
            respondError("END_LINK wrong no of args");
            return false;
        }

        String rId = parts[1];

        // try and find the NetIF by ID
        NetIF netif = controller.findNetIF(rId);

        if (netif == null) {
            respondError("END_LINK cannot find link to "+rId);
            return false;
        }

        // now remove it
        controller.removeNetIF(netif);

        JSONObject jsobj = new JSONObject();

        jsobj.put("id", rId);
        out.println(jsobj.toString());
        response.close();

        return true;



        // send EVENT
        // respond("700 " + netif.getName());

        //Logger.getLogger("log").logln(USR.ERROR, "END LINK EXIT");
    }

    /**
     * An error response
     */
    private void respondError(String msg) throws IOException, JSONException {
        JSONObject jsobj = new JSONObject();
        jsobj.put("error", msg);

        PrintStream out = response.getPrintStream();
        out.println(jsobj.toString());
        response.close();
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