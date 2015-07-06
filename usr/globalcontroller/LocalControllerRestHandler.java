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
import usr.localcontroller.LocalControllerInfo;
import cc.clayman.console.BasicRequestHandler;

/**
 * A class to handle /localcontroller/ requests
 */
public class LocalControllerRestHandler extends BasicRequestHandler {
	// get GlobalController
	GlobalController controller_;

	public LocalControllerRestHandler() {
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
				//if (name == null) {
				// looks like a create
				//createLocalController(request, response);
				//} else {
				notFound(response, "POST bad request");
				//}
			} else if (method.equals("DELETE")) {
				//if (segments.length == 2) {
				// looks like a delete
				//    deleteLocalController(request, response);
				//} else {
				notFound(response, "DELETE bad request");
				// }
			} else if (method.equals("GET")) {
				if (name == null) {      // no arg, so list routers
					listLocalControllers(request, response);
				} else if (segments.length == 2) {   // get router info
					getLocalControllerInfo(request, response);
					//} else if (segments.length == 3 || segments.length == 4) {   // get router other data e.g. link stats
					//  getLocalControllerOtherData(request, response);
				} else {
					notFound(response, "GET bad request");
				}
			} else if (method.equals("PUT")) {
                            if (segments.length == 2) {   // set localcontroller attributes
                                setLCAttributes(request, response);
                            } else {
                                notFound(response, "PUT bad request. segment size = " + segments.length + " " + java.util.Arrays.toString(segments));
                            }


			} else {
				badRequest(response, "Unknown method" + method);
			}

			// check if the response is closed
			response.close();

			return true;

		} catch (IOException ioe) {
                    ioe.printStackTrace();
			System.err.println("IOException " + ioe.getMessage());
		} catch (JSONException jse) {
                    jse.printStackTrace();
			System.err.println("JSONException " + jse.getMessage());
		} catch (Exception e) {
                    e.printStackTrace();
                }

		return false;
	}

	/**
	 * Create a local controller given a request and send a response.
	 */
	/**
	 * Delete a local controller given a request and send a response.
	 */

	/**
	 * List local controllers given a request and send a response.
	 */
	public void listLocalControllers(Request request, Response response) throws IOException, JSONException {
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
			if (detail.equals("name") ||
					detail.equals("all")) {
				// fine
			} else {
				complain(response, "Bad detail: " + detail);
			}

			/* process optional args */

		} else if (query.containsKey("name")) {
			name = query.get("name");
			value = name;

		} else if (query.containsKey("address")) {
			address = query.get("address");
			value = address;

		} else {
			detail = "name";
		}

		// and send them back as the return value
		PrintStream out = response.getPrintStream();

		JSONObject jsobj;

		if (detail != null) {
			jsobj = controller_.listLocalControllers("detail=" + detail);
		} else {
			if (name != null) {
				jsobj = controller_.listLocalControllers("name=" + name);
			} else if (address != null) {
				jsobj = controller_.listLocalControllers("address=" + address);
			} else {
				jsobj = controller_.listLocalControllers("detail=" + "id");
			}
		}

		out.println(jsobj.toString());

	}

	/**
	 * Get info on a local controller given a request and send a response.
	 */
	public void getLocalControllerInfo(Request request, Response response) throws IOException, JSONException {
		// if we got here we have 2 parts
		// /localcontroller/ and another bit
		String name = request.getPath().getName();
		Scanner sc = new Scanner(name);
		
		if (sc.hasNext()) {
			String localControllerName = sc.next();
			sc.close();
			// if it exists, get data, otherwise complain
			// if (!controller_.isValidRouterID(routerID)) {
			//    complain(response, " arg is not valid router id: " + name);
			//    response.close();
			//    return;
			// }


			// and send them back as the return value
			PrintStream out = response.getPrintStream();

			JSONObject jsobj = controller_.getLocalControllerInfo(name);

			out.println(jsobj.toString());


			/*  } else {
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

                out.println(jsobj.toString());*/

		} else {
			complain(response, "getLocalControllerInfo arg is not appropriate: " + name);
		}

		// }

	}

	/**
	 * Get other data on a local controller given a request and send a response.
	 * e.g. link stats
	 */
	/*  public void getLocalControllerOtherData(Request request, Response response) throws IOException, JSONException {
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
            } else if (segments.length == 4) {
                // get specified link stats
                jsobj = controller_.getRouterLinkStats(routerID, dstID);
            }


            out.println(jsobj.toString());

        } else {
            complain(response, "getRouterOtherData arg is not appropriate: " + name);
        }

    }*/



        /**
     * Set the attributes of a localcontroller given a request and send a response.
     * Currently can only modify the online/offline status.
     */
    public void setLCAttributes(Request request, Response response) throws IOException, JSONException {
        String status = null;
        Scanner scanner;

        Query query = request.getQuery();

        /* process compulsory args */

        // process arg weight
        if (query.containsKey("status")) {
            scanner = new Scanner(query.get("status"));

            if (scanner.hasNext()) {
                status = scanner.next();
                scanner.close();


                if (! (status.equals("online") || status.equals("offline"))) {
                    complain(response, "arg status is not appropriate - must be 'online' or 'offline'");
                    response.close();
                    return;
                }
                
            } else {
            	scanner.close();
                complain(response, "arg status is not appropriate");
                response.close();
                return;
            }
        } else {
            complain(response, "missing arg status");
            response.close();
            return;
        }

        // /localcontroller/ and another bit
        String name = request.getPath().getName();
        Scanner sc = new Scanner(name);

        if (sc.hasNext()) {
            String lcValue = sc.next();
            sc.close();

            // if it exists, stop it, otherwise complain
            if (controller_.isValidLocalControllerID(lcValue)) {
            } else {
                complain(response, " arg is not valid localcontroller id: " + name);
                return;
            }


            JSONObject jsobj = null;
            boolean success = true;
            String failMessage = null;

            if (status.equals("online")) {

                jsobj = controller_.takeLocalControllerOnlineJSON(lcValue);
            } else  if (status.equals("offline")) {

                jsobj = controller_.takeLocalControllerOfflineJSON(lcValue);
            } else {
                throw new Error("LocalControllerRestHandler: coding error");
            }

            if (jsobj.get("success").equals(false)) {
                success = false;
                failMessage = (String)jsobj.get("msg");
            }

            if (success) {
                PrintStream out = response.getPrintStream();

                // jsobj = controller_.findLinkInfoAsJSON(id);

                out.println(jsobj.toString());
            } else {
                complain(response, "Error: " + failMessage);
            }



        } else {
            complain(response, "arg is not valid: " + name);
        }
    }

}
