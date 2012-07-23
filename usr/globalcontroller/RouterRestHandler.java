package usr.globalcontroller;

import usr.logging.*;
import usr.common.BasicRouterInfo;
import usr.console.RequestHandler;
import usr.console.AbstractRestRequestHandler;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import us.monoid.json.*;
import java.util.Scanner;
import java.io.PrintStream;
import java.io.IOException;

/**
 * A class to handle /router/ requests
 */
public class RouterRestHandler extends AbstractRestRequestHandler implements RequestHandler {
        // get GlobalController
        GlobalController gc;

    public RouterRestHandler() {
    }

    /**
     * Handle a request and send a response.
     */
    public void  handle(Request request, Response response) {
        // get GlobalController
        gc = (GlobalController)getManagementConsole().getComponentController();

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

            System.out.println("REQUEST: " + request.getMethod() + " " +  request.getTarget());

            long time = System.currentTimeMillis();

            response.set("Content-Type", "application/json");
            response.set("Server", "GlobalController/1.0 (SimpleFramework 4.0)");
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
                if (name == null) {
                    // looks like a create
                    createRouter(request, response);
                } else {
                    notFound(response, "POST bad request");
                }
            } else if (method.equals("DELETE")) {
                if (segments.length == 2) {
                    // looks like a delete
                    deleteRouter(request, response);
                } else {
                    notFound(response, "DELETE bad request");
                }
            } else if (method.equals("GET")) {
                if (name == null) {      // no arg, so list routers
                    listRouters(request, response);
                } else if (segments.length == 2) {   // get router info
                    getRouterInfo(request, response);
                } else {
                    notFound(response, "GET bad request");
                }
            } else if (method.equals("PUT")) {
                {
                    badRequest(response, "PUT bad request");
                }
            } else {
                badRequest(response, "Unknown method" + method);
            }



            // check if the response is closed
            response.close();

        } catch (IOException ioe) {
            System.err.println("IOException " + ioe.getMessage());
        } catch (JSONException jse) {
            System.err.println("JSONException " + jse.getMessage());
        }

    }

    /**
     * Create router given a request and send a response.
     */
    public void createRouter(Request request, Response response) throws IOException, JSONException {
        // Args:
        // [name]
        // [address]

        String name = "";
        String address = "";

        Query query = request.getQuery();

        /* process optional args */

        if (query.containsKey("name")) {
            name = query.get("name");
        }

        if (query.containsKey("address")) {
            address = query.get("address");
        }


        /* do work */

        // start a router, and get it's ID
        int rID = gc.startRouter(System.currentTimeMillis(), address, name);

        if (rID < 0) {
            // error
            badRequest(response, "Error creating router");

        } else {
            // now lookup all the saved details
            BasicRouterInfo bri = gc.findRouterInfo(rID);

            // and send them back as the return value
            PrintStream out = response.getPrintStream();

            JSONObject jsobj = new JSONObject();

            jsobj.put("routerID", bri.getId());
            jsobj.put("name", bri.getName());
            jsobj.put("address", bri.getAddress());
            jsobj.put("mgmtPort", bri.getManagementPort());
            jsobj.put("r2rPort", bri.getRoutingPort());

            out.println(jsobj.toString());

        }
    }

    /**
     * Delete a router given a request and send a response.
     */
    public void deleteRouter(Request request, Response response) throws IOException, JSONException {
        // if we got here we have 2 parts
        // /router/ and another bit
        String name = request.getPath().getName();
        Scanner sc = new Scanner(name);

        if (sc.hasNextInt()) {
            int id = sc.nextInt();
            
            // if it exists, stop it, otherwise complain
            if (gc.isValidRouterID(id)) {
                // delete a router
                gc.endRouter(System.currentTimeMillis(), id);

                // and send them back as the return value
                PrintStream out = response.getPrintStream();

                JSONObject jsobj = new JSONObject();

                jsobj.put("status", "done");

                out.println(jsobj.toString());
            } else {
                badRequest(response, "deleteRouter arg is not valid router id: " + name);
            }

        } else {
            badRequest(response, "deleteRouter arg is not Integer: " + name);
        }
    }

    /**
     * List routers given a request and send a response.
     */
    public void listRouters(Request request, Response response) throws IOException, JSONException {
        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();


        for (BasicRouterInfo info : gc.getAllRouterInfo()) {
            array.put(info.getId());
        }

        jsobj.put("type", "router");
        jsobj.put("list", array);

        out.println(jsobj.toString());

    }

    /**
     * Get info on a router given a request and send a response.
     */
    public void getRouterInfo(Request request, Response response) throws IOException, JSONException {
        notFound(response, "getRouterInfo not implemented yet");
    }

}
