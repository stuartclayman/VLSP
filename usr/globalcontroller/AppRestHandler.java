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
import java.util.Map;;
import java.io.PrintStream;
import java.io.IOException;
import java.util.concurrent.*;
import usr.events.*;

/**
 * A class to handle /router/[0-9]+/app/ requests
 */
public class AppRestHandler extends AbstractRestRequestHandler
    implements RequestHandler
{
// get GlobalController
GlobalController controller_;

public AppRestHandler()
{
}

/**
 * Handle a request and send a response.
 */
public void  handle(Request request, Response response)                    
{
    // get GlobalController
    controller_ = (GlobalController)getManagementConsole().
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
            "REQUEST: " + request.getMethod() + " " +
            request.getTarget());

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
            if (name == null && segments.length == 3)
                // looks like a create
                createApp(request, response);
            else
                notFound(response, "POST bad request");
        } else if (method.equals("DELETE")) {
            if (segments.length == 4)
                // looks like a delete
                deleteApp(request, response);
            else
                notFound(response, "DELETE bad request");
        } else if (method.equals("GET")) {
            if (name == null)  // no arg, so list apps
                listApps(request, response);
            else if (segments.length == 4)              // get app info
                getAppInfo(request, response);
            else
                notFound(response, "GET bad request");
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
 * Create app given a request and send a response.
 */
public void createApp(Request request, Response response) 
throws IOException, JSONException
{
    // args

    int routerID;
    String className= null;
    String rawArgs = null;
    String[] args = null;

    // get the path
    Path path = request.getPath();
    String[] segments = path.getSegments();

    Query query = request.getQuery();
    Scanner scanner;

    // process router ID
    // it is 2nd element of segments
    String routerValue = segments[1];
    scanner = new Scanner(routerValue);
    if (scanner.hasNextInt()) {
        routerID = scanner.nextInt();
    } else {
        badRequest(response, "arg routerID is not an Integer");
        response.close();
        return;
    }

    /* process compulsory args */

    // process className
    if (query.containsKey("className")) {
        className = query.get("className");
    } else {
        badRequest(response, "missing arg className");
        response.close();
        return;
    }

    /* process optional args */

    // process app args
    if (query.containsKey("args")) {
        rawArgs = query.get("args");

        rawArgs = rawArgs.trim();
        rawArgs = rawArgs.replaceAll("  +", " ");
        // now convert raw args to String[]
        args = rawArgs.split(" ");
    } else {
        args= new String[0];
    }

    /* do work */

    boolean success= true;
    String failMessage=null;
    JSONObject jsobj= null;
    try {
        OnRouterEvent ore= new OnRouterEvent
            (controller_.getElapsedTime(),null,routerID,
                className, args);
        jsobj= controller_.executeEvent(ore);
        if (jsobj.get("success").equals(false)) {
            success= false;
            failMessage= (String)jsobj.get("msg");
        }
    } catch (InterruptedException ie) {
        success= false;
        failMessage="Signal interrupted in global controller";
    } catch (InstantiationException ine) {
        success= false;
        failMessage="Unexplained failure executing OnRouterEvent";
    } catch (TimeoutException to) {
        success= false;
        failMessage="Semaphore timeout in global controller -- too busy";
    }
    if (success) {
        PrintStream out = response.getPrintStream();
        out.println(jsobj.toString());
    } else {
        badRequest(response, "Error starting application: "+failMessage);
        response.close();
    }
}

/**
 * Delete a app given a request and send a response.
 */
public void deleteApp(Request request, Response response) 
throws IOException, JSONException 
{
     notFound(response, "deleteApp not implemented yet");
}

/**
 * List apps given a request and send a response.
 */
public void listApps(Request request, Response response) 
    throws IOException, JSONException {
    int routerID;

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
    } else {
        badRequest(response, "arg routerID is not an Integer");
        response.close();
        return;
    }
    
    boolean success= true;
    String failMessage=null;
    JSONObject jsobj= null;
    try {
        ListAppsEvent lae= new ListAppsEvent
            (controller_.getElapsedTime(),null,routerID);
        jsobj= controller_.executeEvent(lae);
        if (jsobj.get("success").equals(false)) {
            success= false;
            failMessage= (String)jsobj.get("msg");
        }
    } catch (InterruptedException ie) {
        success= false;
        failMessage="Signal interrupted in global controller";
    } catch (InstantiationException ine) {
        success= false;
        failMessage="Unexplained failure executing OnRouterEvent";
    } catch (TimeoutException to) {
        success= false;
        failMessage="Semaphore timeout in global controller -- too busy";
    }
    if (success) {
        PrintStream out = response.getPrintStream();
        out.println(jsobj.toString());
    } else {
        badRequest(response, "Error starting application: "+failMessage);
    }
    

}

/**
 * Get info on a app given a request and send a response.
 */
public void getAppInfo(Request request, Response response) 
throws IOException, JSONException 
{
    notFound(response, "getAppInfo not implemented yet");
}
}
