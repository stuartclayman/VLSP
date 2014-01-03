package usr.globalcontroller;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import cc.clayman.console.BasicRequestHandler;

/**
 * A class to handle /ap/ requests
 */
public class AggPointRestHandler extends BasicRequestHandler {
    // get GlobalController
    GlobalController gc;

    public AggPointRestHandler() {
    }

    /**
     * Handle a request and send a response.
     */
    @Override
	public boolean handle(Request request, Response response) {
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

            System.out.println("REQUEST: " + request.getMethod() + " " +  request.getTarget());

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
                if (name == null) {
                    // looks like a create
                    setAP(request, response);
                } else {
                    notFound(response, "POST bad request");
                }
            } else if (method.equals("DELETE")) {
                if (segments.length == 2) {
                    // looks like a delete
                    notFound(response, "DELETE bad request");
                } else {
                    notFound(response, "DELETE bad request");
                }
            } else if (method.equals("GET")) {
                if (name == null) {      // no arg, so list agg points
                    listAggPoints(request, response);
                } else if (segments.length == 2) {   // get agg point info
                    getAggPointInfo(request, response);
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

            return true;

        } catch (IOException ioe) {
            System.err.println("IOException " + ioe.getMessage());
        } catch (JSONException jse) {
            System.err.println("JSONException " + jse.getMessage());
        }

        return true;

    }

    /**
     * Set an agg point given a request and send a response.
     */
    public void setAP(Request request, Response response) throws IOException, JSONException {
        // Args:
        // apID
        // routerID


        int apID;
        int routerID;


        Query query = request.getQuery();

        /* process compulsory args */

        // process arg routerID
        if (query.containsKey("routerID")) {
        	Scanner scanner = new Scanner(query.get("routerID"));

            if (scanner.hasNextInt()) {
            	scanner.close();
                routerID = scanner.nextInt();
            } else {
                complain(response, "arg routerID is not an Integer");
                response.close();
                scanner.close();
                return;
            }
        } else {
            complain(response, "missing arg routerID");
            response.close();
            return;
        }

        // process arg apID
        if (query.containsKey("apID")) {
        	Scanner scanner = new Scanner(query.get("apID"));

            if (scanner.hasNextInt()) {
                apID = scanner.nextInt();
                scanner.close();
            } else {
            	scanner.close();
                complain(response, "arg apID is not an Integer");
                response.close();
                return;
            }
        } else {
            complain(response, "missing arg apID");
            response.close();
            return;
        }

        /* do work */

        // if it exists, stop it, otherwise complain
        if (gc.isValidRouterID(routerID) && gc.isValidRouterID(apID)) {
            // and send back a the return value
            PrintStream out = response.getPrintStream();

            JSONObject jsobj = gc.setAggPoint(apID, routerID);

            out.println(jsobj.toString());
        } else {
            complain(response, "setAP arg is not valid router id: " + routerID + " OR not valid ap id: " + apID);
        }


    }

    /**
     * List agg points given a request and send a response.
     */
    public void listAggPoints(Request request, Response response) throws IOException, JSONException {
        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        JSONObject jsobj = gc.listAggPoints();

        out.println(jsobj.toString());

    }

    /**
     * Get info on an agg point given a request and send a response.
     */
    public void getAggPointInfo(Request request, Response response) throws IOException, JSONException {
        // if we got here we have 2 parts
        // /router/ and another bit
        String name = request.getPath().getName();
        Scanner sc = new Scanner(name);

        if (sc.hasNextInt()) {
            int id = sc.nextInt();

            // if it exists, stop it, otherwise complain
            if (gc.isValidRouterID(id)) {
                // and send them back as the return value
                PrintStream out = response.getPrintStream();

                JSONObject jsobj = gc.getAggPointInfo(id);

                out.println(jsobj.toString());

            } else {
                complain(response, "getAggPointInfo arg is not valid router id: " + name);
            }
        } else {
            complain(response, "getAggPointInfo arg is not Integer: " + name);
        }
        sc.close();

    }

}
