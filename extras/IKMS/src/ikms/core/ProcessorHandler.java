package ikms.core;

import ikms.IKMS;
import ikms.console.IKMSManagementConsole;
import ikms.functions.InformationCollectionAndDisseminationFunction;
import ikms.processor.ProcessorHandle;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

import org.simpleframework.http.Path;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import cc.clayman.console.BasicRequestHandler;
import cc.clayman.console.RequestHandler;

/**
 * A class to handle /processor/ requests
 */
public class ProcessorHandler extends BasicRequestHandler implements RequestHandler {
    IKMSManagementConsole mc;

    public ProcessorHandler() {
    }

    /**
     * Handle a request and send a response.
     */
    public boolean handle(Request request, Response response) {
        // get ManagementConsole
        mc = (IKMSManagementConsole)getManagementConsole();

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
            //String directory = path.getDirectory();
            String name = path.getName();
            String[] segments = path.getSegments();

            // Get the method
            String method = request.getMethod();

            // Get the Query
            //Query query = request.getQuery();


            // and evaluate the input
            if (method.equals("POST")) {
                // can't create a processor via REST
                notFound(response, "POST bad request");
            } else if (method.equals("DELETE")) {
                // can't delete a processor via REST
                // but it can close one
                notFound(response, "DELETE bad request");
            } else if (method.equals("GET")) {
                if (name == null) {      // no arg, so list links
                    listProcessors(request, response);
                } else if (segments.length == 2) {   // get link info
                    getProcessorInfo(request, response);
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
     * List processors given a request and send a response.
     */
    public void listProcessors(Request request, Response response) throws IOException, JSONException {
        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();

        IKMS ikms = mc.getIKMS();
        InformationCollectionAndDisseminationFunction psm = ikms.getInformationCollectionAndDisseminationFunction();

        Collection<ProcessorHandle> procs = psm.listProcessors();

        for (ProcessorHandle proc : procs) {
            array.put(proc.getName());
        }



        jsobj.put("type", "processor");
        jsobj.put("list", array);

        out.println(jsobj.toString());

    }

    /**
     * Get info on a link given a request and send a response.
     */
    public void getProcessorInfo(Request request, Response response) throws IOException, JSONException {
        notFound(response, "getLinkInfo not implemented yet");
    }
}
