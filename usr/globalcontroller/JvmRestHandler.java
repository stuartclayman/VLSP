package usr.globalcontroller;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicJvmInfo;
import cc.clayman.console.BasicRequestHandler;

/**
 * A class to handle /jvm/[0-9]+ requests
 *
 * It sets up a standalone Java application in its own JVM
 */
public class JvmRestHandler extends BasicRequestHandler {
    // get GlobalController
    GlobalController controller_;

    public JvmRestHandler() {
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
                if (name == null && segments.length == 1) {
                    // looks like a create
                    createJvm(request, response);
                } else {
                    notFound(response, "POST bad request");
                }
            } else if (method.equals("DELETE")) {
                if (segments.length == 2) {
                    // looks like a delete
                    deleteJvm(request, response);
                } else {
                    notFound(response, "DELETE bad request");
                }
            } else if (method.equals("GET")) {
                if (name == null) {      // no arg, so list apps
                    listJvms(request, response);
                } else if (segments.length == 2) {   // get app info
                    getJvmInfo(request, response);
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
     * Create app given a request and send a response.
     */
    public void createJvm(Request request, Response response) throws IOException, JSONException {
        // Args:
        // jvmID
        // className
        // args

        int jvmID;
        String className = null;
        String rawArgs = "";

        // get the path
        Path path = request.getPath();
        String[] segments = path.getSegments();

        Query query = request.getQuery();
        Scanner scanner;

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

        }

        //System.err.println("createJvm:  className = " + className + " args = " + rawArgs);

        /* do work */

        // Start Java application


        boolean success = true;
        String failMessage = null;
        JSONObject jsobj = null;

        jsobj = controller_.createJvm(className, rawArgs);

        if (jsobj.get("success").equals(false)) {
            success = false;
            failMessage = (String)jsobj.get("msg");
        }

        if (success) {
            PrintStream out = response.getPrintStream();

            // WAS JSONObject jsobj = controller_.findJvmInfoAsJSON(jvmID);

            out.println(jsobj.toString());
        } else {
            badRequest(response, "Error creating app " + className + " " + rawArgs + " " + failMessage);
            response.close();
        }
    }

    /**
     * Delete a app given a request and send a response.
     */
    public void deleteJvm(Request request, Response response) throws IOException, JSONException {
        System.err.println("deleteJvm");

        // if we got here we have 2 parts
        // /jvm/ and another bit
        String name = request.getPath().getName();
        Scanner sc = new Scanner(name);

        if (sc.hasNextInt()) {
            int id = sc.nextInt();
            sc.close();

            // if it exists, stop it, otherwise complain
            if (controller_.isValidJvmID(id)) {
            } else {
                badRequest(response, "deleteJvm arg is not valid router id: " + name);
                return;
            }

            // delete a jvm
            JSONObject jsobj = null;
            boolean success = true;
            String failMessage = null;

            jsobj = controller_.stopJvm(id);

            if (jsobj.get("success").equals(false)) {
                success = false;
                failMessage = (String)jsobj.get("msg");
            }

            if (success) {
                PrintStream out = response.getPrintStream();

                //jsobj.put("jvmID", id);
                //jsobj.put("status", "deleted");


                out.println(jsobj.toString());
            } else {
                badRequest(response,
                           "Error destroying jvm: " + failMessage);
            }

        } else {
            complain(response, "deleteJvm arg is not Integer: " + name);
        }

    }

    /**
     * List apps given a request and send a response.
     */
    public void listJvms(Request request, Response response) throws IOException, JSONException {
        // process query

        Query query = request.getQuery();

        // the attribute we want about the link
        String detail;

        if (query.containsKey("detail")) {
            detail = query.get("detail");

            // check detail
            if (detail.equals("id") ||
                detail.equals("all")) {
                // fine
            } else {
                complain(response, "Bad detail: " + detail);
            }

        } else {
            detail = "id";
        }


        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        JSONObject jsobj = controller_.listJvms("detail=" + detail);

        out.println(jsobj.toString());

    }

    /**
     * Get info on a app given a request and send a response.
     */
    public void getJvmInfo(Request request, Response response) throws IOException, JSONException {
        // if we got here we have 2 parts
        // /jvm/ and another bit
        String name = request.getPath().getName();
        Scanner sc = new Scanner(name);

        if (sc.hasNextInt()) {
            int jvmID = sc.nextInt();
            sc.close();
            // if it exists, get data, otherwise complain
            if (!controller_.isValidJvmID(jvmID)) {
                complain(response, " arg is not valid jvm id: " + name);
                response.close();
                return;
            }

            // and send them back as the return value
            PrintStream out = response.getPrintStream();


            JSONObject jsobj = controller_.getJvmInfo(jvmID);

            out.println(jsobj.toString());


        } else {
            // not an Integer
            if (name.equals("count")) {
                // and send them back as the return value
                PrintStream out = response.getPrintStream();

                JSONObject jsobj = controller_.getJvmCount();

                out.println(jsobj.toString());

            } else {
                complain(response, "getJvmInfo arg is not appropriate: " + name);
            }

        }
    }

}
