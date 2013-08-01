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
import java.util.concurrent.*;
import usr.events.*;

/**
 * A class to handle /router/ requests
 */
public class RouterRestHandler extends AbstractRestRequestHandler
implements RequestHandler {
    // get GlobalController
    GlobalController controller_;

    public RouterRestHandler() {
    }

    /**
     * Handle a request and send a response.
     */
    public void handle(Request request, Response response) {
        // get GlobalController
        controller_
            = (GlobalController)getManagementConsole().
                getComponentController();

        try {
            /*
             * System.out.println("method: " + request.getMethod());
             * System.out.println("target: " + request.getTarget());
             * System.out.println("path: " + request.getPath());
             * System.out.println("directory: " +
             *    request.getPath().getDirectory());
             * System.out.println("name: " +
             *    request.getPath().getName());
             * System.out.println("segments: " +
             *    java.util.Arrays.asList(request.getPath().getSegments()));
             * System.out.println("query: " + request.getQuery());
             * System.out.println("keys: " +
             *    request.getQuery().keySet());
             */

            System.out.println(
                "REQUEST: " + request.getMethod() + " "
                + request.getTarget());

            long time = System.currentTimeMillis();

            response.set("Content-Type", "application/json");
            response.set("Server",
                         "GlobalController/1.0 (SimpleFramework 4.0)");
            response.setDate("Date", time);
            response.setDate("Last-Modified", time);

            // get the path
            Path path = request.getPath();
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
                if (name == null) {                                                   //
                                                                                      // no
                                                                                      // arg,
                                                                                      // so
                    // list routers
                    listRouters(request, response);
                } else if (segments.length == 2) {                                                 //
                                                                                                   // get
                                                                                                   // router
                    // info
                    getRouterInfo(request, response);
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
        } catch (IOException ioe) {
            System.err.println("IOException " + ioe.getMessage());
        } catch (JSONException jse) {
            System.err.println("JSONException " + jse.getMessage());
        }
    }

    /**
     * Create router given a request and send a response.
     */
    public void createRouter(Request request, Response response)
    throws IOException, JSONException //Args: [name][address]
    {
        String name = null;
        String address = null;

        Query query = request.getQuery();

        /* process optional args */
        if (query.containsKey("name")) {
            name = query.get("name");
        }

        if (query.containsKey("address")) {
            address = query.get("address");
        }

        /* do work */

        boolean success = true;
        String failMessage = null;
        JSONObject jsobj = null;
        try {
            StartRouterEvent ev = new StartRouterEvent
                (
                    controller_.getElapsedTime(), null, address, name);
            jsobj = controller_.executeEvent(ev);

            if (jsobj.get("success").equals(false)) {
                success = false;
                failMessage = (String)jsobj.get("msg");
            }
        } catch (InterruptedException ie) {
            success = false;
            failMessage = "Signal interrupted in global controller";
        } catch (InstantiationException ine) {
            success = false;
            failMessage = "Unexplained failure executing startRouter";
        } catch (TimeoutException to) {
            success = false;
            failMessage =
                "Semaphore timeout in global controller -- too busy";
        }

        if (success) {
            PrintStream out = response.getPrintStream();
            out.println(jsobj.toString());
        } else {
            badRequest(response, "Error creating router: " + failMessage);
        }
    }

    /**
     * Delete a router given a request and send a response.
     */
    public void deleteRouter(Request request, Response response)
    throws IOException, JSONException {
        // if we got here we have 2 parts
        // /router/ and another bit
        String name = request.getPath().getName();
        JSONObject jsobj = null;
        boolean success = true;
        String failMessage = null;
        EndRouterEvent ev;

        try         // Could be integer or could be string
        {
            int rId = Integer.parseInt(name);
            ev =
                new EndRouterEvent(controller_.getElapsedTime(), null, rId);
        } catch (NumberFormatException nfe) {
            try {
                ev = new EndRouterEvent(controller_.getElapsedTime(),
                                        null, name, controller_);
            } catch (InstantiationException ie) {
                badRequest(response,
                           "deleteRouter cannot find router with address " + name);
                return;
            }
        }

        try {
            jsobj = controller_.executeEvent(ev);

            if (jsobj.get("success").equals(false)) {
                success = false;
                failMessage = (String)jsobj.get("msg");
            }
        } catch (InterruptedException ie) {
            success = false;
            failMessage = "Signal interrupted in global controller";
        } catch (InstantiationException ine) {
            success = false;
            failMessage = "Unexplained failure executing endRouter";
        } catch (TimeoutException to) {
            success = false;
            failMessage =
                "Semaphore timeout in global controller -- too busy";
        }

        if (success) {
            PrintStream out = response.getPrintStream();
            out.println(jsobj.toString());
        } else {
            badRequest(response,
                       "Error deleting router " + failMessage);
        }
    }

    /**
     * List routers given a request and send a response.
     */
    public void listRouters(Request request, Response response)
    throws IOException, JSONException {
        JSONObject jsobj = new JSONObject();
        boolean success = true;
        String failMessage = null;

        try {
            ListRoutersEvent ev = new ListRoutersEvent
                    (controller_.getElapsedTime(), null);
            jsobj = controller_.executeEvent(ev);
        } catch (InterruptedException ie) {
            success = false;
            failMessage = "Signal interrupted in global controller";
        } catch (InstantiationException ine) {
            success = false;
            failMessage = "Unexplained failure executing endRouter";
        } catch (TimeoutException to) {
            success = false;
            failMessage =
                "Semaphore timeout in global controller -- too busy";
        }

        if (success) {
            PrintStream out = response.getPrintStream();
            out.println(jsobj.toString());
        } else {
            badRequest(response,
                       "Error listing routers: " + failMessage);
        }
    }

    /**
     * Get info on a router given a request and send a response.
     */
    public void getRouterInfo(Request request, Response response)
    throws IOException, JSONException {
        notFound(response, "getRouterInfo not implemented yet");
    }

}