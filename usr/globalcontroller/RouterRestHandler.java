package usr.globalcontroller;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import cc.clayman.console.BasicRequestHandler;

/**
 * A class to handle /router/ requests
 */
public class RouterRestHandler extends BasicRequestHandler {
    // get GlobalController
    GlobalController controller_;

    public RouterRestHandler() {
    }

    /**
     * Handle a request and send a response.
     */
    @Override
	public boolean handle(Request request, Response response) {
        // get GlobalController
        controller_ = (GlobalController)getManagementConsole().getAssociated();

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

            long time = controller_.getTime();

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
                } else if (segments.length == 3 || segments.length == 4) {   // get router other data e.g. link stats
                    getRouterOtherData(request, response);
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

        return false;
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
        String parameters = "";

        Query query = request.getQuery();

        /* process optional args */

        if (query.containsKey("name")) {
            name = query.get("name");
        }

        if (query.containsKey("address")) {
            address = query.get("address");
        }

        if (query.containsKey("parameters")) {
        		parameters = query.get("parameters").replace("%20", " ");
        }


        /* do work */

        // start a router, and get it's ID

        boolean success = true;
        String failMessage = null;
        JSONObject jsobj = null;

        jsobj = controller_.createRouter(name, address, parameters);

        if (jsobj.get("success").equals(false)) {
            success = false;
            failMessage = (String)jsobj.get("msg");
        }

        if (success) {
            // now lookup all the saved details
            // and send them back as the return value
            PrintStream out = response.getPrintStream();

            // WAS JSONObject jsobj = controller_.findRouterInfoAsJSON(rID);

            out.println(jsobj.toString());
        } else {
            complain(response, "Error creating router: " + failMessage);
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

        JSONObject jsobj = null;
        boolean success = true;
        String failMessage = null;

        if (sc.hasNextInt()) {
            int id = sc.nextInt();
            sc.close();
            // if it exists, stop it, otherwise complain
            if (controller_.isValidRouterID(id)) {
                // delete a router
                jsobj = controller_.deleteRouter(id);

                if (success) {
                    // now lookup all the saved details
                    // and send them back as the return value
                    PrintStream out = response.getPrintStream();

                    //jsobj.put("routerID", id);
                    //jsobj.put("status", "deleted");

                    out.println(jsobj.toString());
                } else {
                    complain(response, "Error deleting Router router: " + failMessage);
                }

            } else {
                complain(response, "deleteRouter arg is not valid router id: " + name);
            }

        } else {
            complain(response, "deleteRouter arg is not Integer: " + name);
        }
    }

    /**
     * List routers given a request and send a response.
     */
    public void listRouters(Request request, Response response) throws IOException, JSONException {
        // process query

        Query query = request.getQuery();

        // the attribute we want about the router
        String detail = null;
        String name = null;
        String address = null;
        String value = null;


        if (query.containsKey("detail")) {
            detail = query.get("detail");

            // check detail
            if (detail.equals("id") || detail.equals("thread") || detail.equals("threadgroup") || 
                detail.equals("all")) {
                // fine
            } else {
                complain(response, "Bad detail: " + detail);
                return;
            }

        /* process optional args */

        } else if (query.containsKey("name")) {
            name = query.get("name");
            value = name;

        } else if (query.containsKey("address")) {
            address = query.get("address");
            value = address;

        } else {
            detail = "id";
        }


        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        JSONObject jsobj;

        if (detail != null) {

            jsobj = controller_.listRouters("detail=" + detail);
        } else {
            if (name != null) {
                jsobj = controller_.listRouters("name=" + name);
            } else if (address != null) {
                jsobj = controller_.listRouters("address=" + address);
            } else {
                jsobj = controller_.listRouters("detail=" + "id");
            }
        }
        

        out.println(jsobj.toString());

    }

    /**
     * Get info on a router given a request and send a response.
     */
    public void getRouterInfo(Request request, Response response) throws IOException, JSONException {
        // if we got here we have 2 parts
        // /router/ and another bit
        String name = request.getPath().getName();
        Scanner sc = new Scanner(name);

        if (sc.hasNextInt()) {
            int routerID = sc.nextInt();
            sc.close();
            // if it exists, get data, otherwise complain
            if (!controller_.isValidRouterID(routerID)) {
                complain(response, " arg is not valid router id: " + name);
                response.close();
                return;
            }


            // and send them back as the return value
            PrintStream out = response.getPrintStream();

            JSONObject jsobj = controller_.getRouterInfo(routerID);

            out.println(jsobj.toString());


        } else {
            // not an Integer
            if (name.equals("maxid")) {
                // and send them back as the return value
                PrintStream out = response.getPrintStream();

                JSONObject jsobj = controller_.getMaxRouterID();

                out.println(jsobj.toString());

            } else if (name.equals("count")) {
                // and send them back as the return value
                PrintStream out = response.getPrintStream();

                JSONObject jsobj = controller_.getRouterCount();

                out.println(jsobj.toString());

            } else {
                complain(response, "getRouterInfo arg is not appropriate: " + name);
            }

        }

    }

    /**
     * Get other data on a router given a request and send a response.
     * e.g. link stats
     */
    public void getRouterOtherData(Request request, Response response) throws IOException, JSONException {
        // if we got here we have 3 parts
        // /router/ an id and another bit
        int routerID = 0;
        int dstID = 0;

        Scanner scanner;

        // get the path
        Path path = request.getPath();
        String[] segments = path.getSegments();


        // process router ID
        // it is 2nd element of segments
        String routerValue = segments[1];

        scanner = new Scanner(routerValue);

        if (scanner.hasNextInt()) {
            routerID = scanner.nextInt();
            scanner.close();

        } else {
        	scanner.close();
            badRequest(response, "arg routerID is not an Integer");
            response.close();
            return;
        }

        // if it exists, get data, otherwise complain
        if (!controller_.isValidRouterID(routerID)) {
            complain(response, " arg is not valid router id: " + routerValue);
            response.close();
            return;
        }


        // process name
        // it is 3rd element of segments
        String name = segments[2];


        // check if we need the dstID
        if (segments.length == 4) {
            // process dst router ID
            // it is 4th element of segments
            String dstValue = segments[3];

            scanner = new Scanner(dstValue);

            if (scanner.hasNextInt()) {
                dstID = scanner.nextInt();
                scanner.close();
            } else {
                badRequest(response, "arg dstID is not an Integer");
                response.close();
                scanner.close();
                return;
            }

            // if it exists, get data, otherwise complain
            if (!controller_.isValidRouterID(dstID)) {
                complain(response, " arg is not valid router id: " + dstValue);
                response.close();
                return;
            }

        }

        // not an Integer
        if (name.equals("link_stats")) {
            // and send them back as the return value
            PrintStream out = response.getPrintStream();

            JSONObject jsobj = null;

            if (segments.length == 3) {
                // get all link stats
                jsobj = controller_.getRouterLinkStats(routerID);
                out.println(jsobj.toString());

            } else if (segments.length == 4) {
                // get specified link stats
                jsobj = controller_.getRouterLinkStats(routerID, dstID);
                out.println(jsobj.toString());

            }



        } else {
            complain(response, "getRouterOtherData arg is not appropriate: " + name);
        }

    }


}
