package usr.globalcontroller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Path;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.output.OutputNetwork;
import cc.clayman.console.BasicRequestHandler;

/**
 * A class to handle /graph/ requests
 * <p>
 * Examples:  /graph/dot  /graph/plain
 */
public class GraphRestHandler extends BasicRequestHandler {
    // get GlobalController
    GlobalController gc;

    public GraphRestHandler() {
    }

    /**
     * Handle a request and send a response.
     */
    @Override
	public boolean  handle(Request request, Response response) {
        // get GlobalController
        gc = (GlobalController)getManagementConsole().getAssociated();

        try {
            /*
               System.out.println("method: " + request.getMethod());
               System.out.println("target: " + request.getTarget());
               System.out.println("path: " + request.getPath());
               System.out.println("directory: " + request.getPath().getDirectory());
               System.out.println("name: " + request.getPath().getName());
               System.out.println("segments: " + java.util.Arrays.asList(request.getPath().getSegments()));
               System.out.println("query: " + request.getQuery());
               System.out.println("keys: " + request.getQuery().keySet());
             */

            long time = gc.getTime();

            response.set("Content-Type", "application/json");
            response.set("Server", "GlobalController/1.0 (SimpleFramework 4.0)");
            response.setDate("Date", time);
            response.setDate("Last-Modified", time);

            // get the path
            Path path = request.getPath();
            path.getDirectory();
            String name = path.getName();
            String[] segments = path.getSegments();

            // Get the method
            String method = request.getMethod();

            request.getQuery();

            // and evaluate the input
            if (method.equals("POST")) {
                badRequest(response, "POST bad request");
            } else if (method.equals("DELETE")) {
                badRequest(response, "DELETE bad request");
            } else if (method.equals("GET")) {
                if (name == null) {      // no arg, error
                    notFound(response, "GET bad request");
                } else if (segments.length == 2) {   // get graph info
                    getGraphInfo(request, response);
                } else {
                    notFound(response, "GET bad request");
                }
            } else if (method.equals("PUT")) {
                badRequest(response, "PUT bad request");
            } else {
                badRequest(response, "Unknown method" + method);
            }



            // check if the response is closed
            response.close();

            return true;

        } catch (IOException ioe) {
            System.err.println("IOException " + ioe.getMessage());
        } catch (JSONException jse) {
            System.err.println("JSONException " + jse.getMessage());
        }

        return false;

    }

    /**
     * Get the graph version of a file given a request and send a response.
     */
    public void getGraphInfo(Request request, Response response) throws IOException, JSONException {
        // if we got here we have 2 parts
        // /graph/ and another bit
        String name = request.getPath().getName();


        // allocate PrintStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);


        if (name.equals("dot")) {
            OutputNetwork on = new OutputNetwork();

            on.visualizeNetworkGraph(null, ps, gc);
        } else {
            // get the network in the PrintStream
            // the other part is the grpah style - e.g. usr.globalcontroller.visualization.PlainNetworkVisualization
            String graphStyle = name;

            OutputNetwork on = new OutputNetwork();

            on.visualizeNetworkGraph(graphStyle, ps, gc);
        }

        // convert the ByteArrayOutputStream to a String
        String theString = baos.toString();

        // and send it back as the return value
        PrintStream out = response.getPrintStream();

        // now send it as a response
        JSONObject jsobj = new JSONObject();

        jsobj.put("graph", theString);
        out.println(jsobj.toString());
        response.close();


    }

}
