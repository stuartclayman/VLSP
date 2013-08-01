package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.common.LocalHostInfo;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * The GetRouterStatsCommand command.
 */
public class GetRouterStatsCommand extends LocalCommand implements Callable<Boolean> {
    // the original command
    String command;
    Response response;
    PrintStream out;

    /**
     * Construct a GetRouterStatsCommand.
     */
    public GetRouterStatsCommand() {
        super(MCRP.GET_ROUTER_STATS.CMD);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(Request request, Response response) {

        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);


            // save values for future
            this.command = value;
            this.response = response;
            this.out = out;


            // create an Executor pool
            ExecutorService pool = Executors.newCachedThreadPool();

            // run GET_ROUTER_STATS collection in separate thread
            Future<Boolean> future = pool.submit(this);

            // wait for result
            boolean result = false;

            try {
                result = future.get(); // use future
            } catch (ExecutionException ex) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "GetRouterStatsCommand: ExecutionException " + ex);

                out.println(jsobj.toString());
                response.close();

                return false;

            } catch (InterruptedException ie) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "GetRouterStatsCommand: InterruptedException " + ie);

                out.println(jsobj.toString());
                response.close();

                return false;

            }

            // shutdown pool
            pool.shutdown();

            return result;
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        finally {
            return false;
        }


    }

    public Boolean call() {
        try {
            // the result list
            List<String> list;

            // get arg for specified router
            String [] args = command.split(" ");

            if (args.length == 1) {
                // no args so get data for all routers

                // Get controller to do the work
                // and get stats for the router
                list = controller.getRouterStats();

                if (list == null) {
                    // no router with that name
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "No routers on this LocalController");

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }

            } else if (args.length == 2) {
                Scanner sc = new Scanner(args[1]);
                int routerID;

                if (sc.hasNextInt()) {
                    routerID = sc.nextInt();
                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "Argument for GET_ROUTER_STATS command must be int");

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }

                // Get controller to do the work
                // and get stats for the router
                list = controller.getRouterStats(routerID);

                if (list == null) {
                    // no router with that name
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "No router with ID " + routerID + " on this LocalController");

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }

            } else {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Expected 2 arguments GET_ROUTER_STATS [router_id]");

                out.println(jsobj.toString());
                response.close();

                return false;
            }



            // now return the list
            int size = list.size();

            JSONObject jsobj = new JSONObject();

            for (int r = 0; r < size; r++) {
                // pick out the r-th stat
                jsobj.put(Integer.toString(r), list.get(r));
            }

            jsobj.put("size", size);

            out.println(jsobj.toString());
            response.close();

            return true;

        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        finally {
            return false;
        }



    }

}