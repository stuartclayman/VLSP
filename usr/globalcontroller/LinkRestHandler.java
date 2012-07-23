package usr.globalcontroller;

import usr.logging.*;
import usr.common.LinkInfo;
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
 * A class to handle /link/ requests
 */
public class LinkRestHandler extends AbstractRestRequestHandler implements RequestHandler {
    // get GlobalController
    GlobalController gc;

    public LinkRestHandler() {
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
                    createLink(request, response);
                } else {
                    notFound(response, "POST bad request");
                }
            } else if (method.equals("DELETE")) {
                if (segments.length == 2) {
                    // looks like a delete
                    deleteLink(request, response);
                } else {
                    notFound(response, "DELETE bad request");
                }
            } else if (method.equals("GET")) {
                if (name == null) {      // no arg, so list links
                    listLinks(request, response);
                } else if (segments.length == 2) {   // get link info
                    getLinkInfo(request, response);
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
     * Create link given a request and send a response.
     */
    public void createLink(Request request, Response response) throws IOException, JSONException {
        // Args:
        // router1
        // router2
        // [weight]
        // [linkName]


        int router1;
        int router2;
        int weight = 1;
        String linkName = "";
        Scanner scanner ;

        Query query = request.getQuery();

        /* process compulsory args */

        // process arg router1
        if (query.containsKey("router1")) {
            scanner = new Scanner(query.get("router1"));

            if (scanner.hasNextInt()) {
                router1 = scanner.nextInt();
            } else {
                badRequest(response, "arg router1 is not an Integer");
                response.close();
                return;
            }
        } else {
            badRequest(response, "missing arg router1");
            response.close();
            return;
        }

        // process arg router2
        if (query.containsKey("router2")) {
            scanner = new Scanner(query.get("router2"));

            if (scanner.hasNextInt()) {
                router2 = scanner.nextInt();
            } else {
                badRequest(response, "arg router2 is not an Integer");
                response.close();
                return;
            }
        } else {
            badRequest(response, "missing arg router2");
            response.close();
            return;
        }

        /* process optional args */

        // process arg weight
        if (query.containsKey("weight")) {
            scanner = new Scanner(query.get("weight"));

            if (scanner.hasNextInt()) {
                weight = scanner.nextInt();
            } else {
                badRequest(response, "arg weight is not an Integer");
                response.close();
                return;
            }
        }

        if (query.containsKey("linkName")) {
            linkName = query.get("linkName");
        }

        /* do work */

        // start a router, and get it's ID
        int linkID = gc.startLink(System.currentTimeMillis(), router1, router2, weight, linkName);

        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        if (linkID < 0) {
            // error
            badRequest(response, "Error creating link");

        } else if (linkID == 0) {
            // already exists
            badRequest(response, "Link already exists");

        } else {
            // now lookup all the saved link info details
            LinkInfo li = gc.findLinkInfo(linkID);

            JSONObject jsobj = new JSONObject();

            jsobj.put("linkID", li.getLinkID());
            jsobj.put("linkName", li.getLinkName());
            jsobj.put("weight", li.getLinkWeight());
            jsobj.put("router1", (Integer)li.getEndPoints().getFirst());
            jsobj.put("router2", (Integer)li.getEndPoints().getSecond());

            out.println(jsobj.toString());

        }

    }

    /**
     * Delete a link given a request and send a response.
     */
    public void deleteLink(Request request, Response response) throws IOException, JSONException {
        // if we got here we have 2 parts
        // /link/ and another bit
        String name = request.getPath().getName();
        Scanner sc = new Scanner(name);

        if (sc.hasNextInt()) {
            int id = sc.nextInt();
            
            // if it exists, stop it, otherwise complain
            if (gc.isValidLinkID(id)) {

                // now lookup all the saved link info details
                LinkInfo li = gc.findLinkInfo(id);

                // delete a link
                gc.endLink(System.currentTimeMillis(), (Integer)li.getEndPoints().getFirst(), (Integer)li.getEndPoints().getSecond());

                // and send them back as the return value
                PrintStream out = response.getPrintStream();

                JSONObject jsobj = new JSONObject();

                jsobj.put("status", "done");

                out.println(jsobj.toString());
            } else {
                badRequest(response, "deleteLink arg is not valid router id: " + name);
            }

        } else {
            badRequest(response, "deleteLink arg is not Integer: " + name);
        }
    }

    /**
     * List links given a request and send a response.
     */
    public void listLinks(Request request, Response response) throws IOException, JSONException {
        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();


        for (LinkInfo info : gc.getAllLinkInfo()) {
            array.put(info.getLinkID());
        }

        jsobj.put("type", "link");
        jsobj.put("list", array);

        out.println(jsobj.toString());

    }

    /**
     * Get info on a link given a request and send a response.
     */
    public void getLinkInfo(Request request, Response response) throws IOException, JSONException {
        notFound(response, "getLinkInfo not implemented yet");
    }
}
