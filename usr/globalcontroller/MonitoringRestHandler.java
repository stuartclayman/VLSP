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
import cc.clayman.console.BasicRequestHandler;

/**
 * A class to handle /monitoring/ requests
 *
 */
public class MonitoringRestHandler extends BasicRequestHandler {
    // get GlobalController
    GlobalController controller_;

    public MonitoringRestHandler() {
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
               System.out.println("seg length: " + request.getPath().getSegments().length);
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
                    //addMonProbe(request, response);
                } else {
                    notFound(response, "POST bad request");
                }
            } else if (method.equals("DELETE")) {
                if (segments.length == 2) {
                    // looks like a delete
                    //stopMonProbe(request, response);
                } else {
                    notFound(response, "DELETE bad request");
                }
            } else if (method.equals("GET")) {
                if (name == null) {      // no arg, so list monitoring info
                    listMonitoring(request, response);
                } else if (segments.length == 2) {   // get monitoring info
                    getMonitoringInfo(request, response);
                } else {
                    notFound(response, "GET bad request");
                }
            } else if (method.equals("PUT")) {
                if (segments.length == 2) {   // set link attributes
                    setMonitoringAttributes(request, response);
                } else {
                    notFound(response, "PUT bad request");
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
     * List apps given a request and send a response.
     */
    public void listMonitoring(Request request, Response response) throws IOException, JSONException {
        // send them back as the return value
        PrintStream out = response.getPrintStream();


        JSONObject jsobj = controller_.getMonitoringInfo();

        out.println(jsobj.toString());

    }
    
    /**
     * Get info on a app given a request and send a response.
     */
    public void getMonitoringInfo(Request request, Response response) throws IOException, JSONException {
        
    }

    
    /**
     * Set the attributes for the monitoring given a request and send a response.
     * Currently can only modify a monitoring Forwarding.
     */
    public void setMonitoringAttributes(Request request, Response response) throws IOException, JSONException {
        // Args:
        // [host]
        // [port]
        
        String host = "";
        int port = 0;
        boolean connect = false;
        boolean disconnect = false;

        Scanner scanner;

        Query query = request.getQuery();

        /* process args */

        // Can have following modes:
        // 1: connect
        // 2: host / port
        // 3: host / port / connect

        int mode = 0;

        if (query.containsKey("host") && query.containsKey("port") && query.containsKey("connect")) {
            mode = 3;
        } else if (query.containsKey("host") && query.containsKey("port")) {
            mode = 2;
        } else if (query.containsKey("connect")) {
            mode = 1;
        } else {
            complain(response, "missing arg host or port or connect");
            response.close();
            return;
        }

        
        /* got right args */
            
        // process arg connect
        if (query.containsKey("connect")) {
            String connStr = query.get("connect").toLowerCase();

            if (connStr.equals("true") || connStr.equals("1")) {
                connect = true;
            } else if (connStr.equals("false") || connStr.equals("0")) {
                disconnect = true;
            } else {
                complain(response, "arg connect has bad value: " + connStr);
                response.close();
                return;
            }
        }
        
        // process arg host
        if (query.containsKey("host")) {
            host = query.get("host");
        }
        
        // process arg port
        if (query.containsKey("port")) {
            scanner = new Scanner(query.get("port"));

            if (scanner.hasNextInt()) {
                port = scanner.nextInt();
                scanner.close();
            } else {
            	scanner.close();
                complain(response, "arg port is not an Integer");
                response.close();
                return;
            }
        }

        // /monitoring/ and another bit
        String name = request.getPath().getName();

        if (name.equals("forwarder")) {
            // dealing with forwarder

            PrintStream out = response.getPrintStream();
            
            JSONObject jsobj = new JSONObject();
                
            boolean isChanged = false;
            
            if (mode == 2 || mode == 3) {
                // we need to set the address

                //if ( ! (host.equals("") && port == 0)) {
                    // user passed in host and port
                    isChanged = controller_.setMonitoringForwarderAddress(host, port);

                    jsobj.put("host", host);
                    jsobj.put("port", port);
                //}
            } else {
                // mode 1 has no address info
            }


            if (mode == 1 || mode == 3) {
                // we need to connect or disconnect explicitly

                try {
                    if (connect) {
                        boolean result = controller_.connectMonitoringForwarder();
                        jsobj.put("connected", result);
                    } else if (disconnect) {
                        // no explicit connect
                        boolean result = controller_.disconnectMonitoringForwarder();
                        jsobj.put("disconnected", result);
                    } else {
                        // no explicit connect or disconnected
                        // should never get here
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // mode 2 might have changed address
                // so we need to disconnect and connect
                if (isChanged) {
                    boolean result = controller_.disconnectMonitoringForwarder();
                    result = controller_.connectMonitoringForwarder();

                    jsobj.put("reconnected", result);
                }
                
            }

            jsobj.put("mode", mode);


            out.println(jsobj.toString());

        } else {
            complain(response, "Setting monitoring option not valid: " + name);
        }
    }

}
