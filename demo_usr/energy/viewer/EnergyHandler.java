package demo_usr.energy.viewer;

import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import us.monoid.json.*;
import java.util.Scanner;
import java.util.Collection;
import java.io.PrintStream;
import java.io.IOException;
import cc.clayman.console.*;

/**
 * A class to handle /energy/ requests
 */
public class EnergyHandler extends BasicRequestHandler implements RequestHandler {
    EnergyViewerConsole console;


    public EnergyHandler() {
    }


    /**
     * Handle a request and send a response.
     */
    public boolean  handle(Request request, Response response) {
        // get RestListener
        console = (EnergyViewerConsole)getManagementConsole();

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

            //System.out.println("\n/energy/ REQUEST: " + request.getMethod() + " " +  request.getTarget());

            long time = System.currentTimeMillis();

            response.set("Content-Type", "application/json");
            response.set("Server", "Knowledge Block/1.0 (SimpleFramework 4.0)");
            response.setDate("Date", time);
            response.setDate("Last-Modified", time);

            // get the path
            Path path =  request.getPath();
            String directory = path.getDirectory();
            String name = path.getName();
            String[] segments = path.getSegments();

            // Get the method
            String method = request.getMethod();

            // Get the Query
            Query query = request.getQuery();


            // and evaluate the input
            if (method.equals("POST")) {
                updateData(request, response);
            } else if (method.equals("DELETE")) {
                // can't delete data via REST
                notFound(response, "DELETE bad request");
            } else if (method.equals("GET")) {
                if (name == null) {      // no arg, so get energy data
                    getEnergyData(request, response);
                } else if (segments.length == 2) {   // get specific energy info
                } else {
                    notFound(response, "GET bad request");
                }
            } else if (method.equals("PUT")) {
                notFound(response, "PUT bad request");
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
     * Update some data given a request and send a response.
     */
    public void updateData(Request request, Response response) throws IOException, JSONException  {
        // see if we should send xml
        boolean xml = false;

        boolean hasHTTPAccept = request.contains("Accept");

        if (hasHTTPAccept) {
            String accept = request.getValue("Accept");
            if (accept.equals("application/xml") || accept.equals("text/xml")) {
                xml = true;
            }
        }

        // get the path
        Path path =  request.getPath();
        String name = path.getName();

        Query query = request.getQuery();
        Scanner scanner;

        /* process  args */


        // process value arg
        JSONString value = null;
        Double price = 0.0;
        boolean success = true;
        String failMessage = null;
        
        if (query.containsKey("value")) {
            try {
                String valueArg = query.get("value");
                price = Double.parseDouble(valueArg);
            } catch (Exception e) {
                complain(response, "Price value not a float or double passed in");
                return;
            }

        } else {
            String content = request.getContent();

            if (content != null && content.length() > 0) {
            
                value = new JSONData(content);
            
                //System.err.println("\nGot: " + path + " '" + value + "'");

                JSONObject recvd = new JSONObject(value.toString());

                //System.err.println("recvd = " + recvd + " ");
        
                JSONObject payload = recvd.getJSONObject("payload");
                price = payload.getDouble("price");
                
            } else {
                complain(response, "No Price value passed in");
                return;
            }

        }
        


        System.err.println("Recv price: " + price);

        // set the price into the console
        console.setEnergyPrice(price);


        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        JSONObject jsobj = new JSONObject();

        jsobj.put("value", price);
        jsobj.put("result", "OK");

        if (xml) {
            out.println(XML.toString(jsobj, "response"));
        } else {                       
            out.println(jsobj.toString());
        }


    }


    /**
     * Get some data given a request and send a response.
     */
    public void getEnergyData(Request request, Response response) throws IOException, JSONException  {
        // process query

        Query query = request.getQuery();

        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        JSONObject jsobj = new JSONObject();

        // get the energy usage from the console
        double usage = console.getEnergyUsage();

        // {
        //     message: "energy_usage"
        //     timestamp: 449498234292
        //     payload: {
	//         type: "Wh" 
        //         value: 4612.34
        //      }
        // }

        jsobj.put("message", "energy_usage");
        jsobj.put("timestamp", System.currentTimeMillis());

        JSONObject payload = new JSONObject();

        payload.put("type", "Wh");
        payload.put("value", usage);

        jsobj.put("payload", payload);

        out.println(jsobj.toString());
    }

}

class JSONData extends JSONObject implements JSONString {

    public JSONData() throws JSONException {
        super();
    }

    public JSONData(String s) throws JSONException {
        super(s);
    }

    public JSONData(JSONObject js) throws JSONException {
        super(js, JSONObject.getNames(js));

    }


    public String toJSONString() {
        return super.toString();
    }
}
