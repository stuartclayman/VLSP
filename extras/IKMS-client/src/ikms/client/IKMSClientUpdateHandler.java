package ikms.client;

import ikms.data.AbstractRestRequestHandler;
import ikms.data.RequestHandler;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;


/**
 * A class to handle /update/ requests
 */
public class IKMSClientUpdateHandler extends AbstractRestRequestHandler implements RequestHandler {
	IKMSClientRestListener listener;

	public IKMSClientUpdateHandler() {
	}

	/**
	 * Handle a request and send a response.
	 */
	public void  handle(Request request, Response response) {
		// get RestListener
		listener = (IKMSClientRestListener)getManagementConsole();

		try {
			/*System.out.println("method: " + request.getMethod());
			System.out.println("target: " + request.getTarget());
			System.out.println("path: " + request.getPath());
			System.out.println("directory: " + request.getPath().getDirectory());
			System.out.println("name: " + request.getPath().getName());
			System.out.println("segments: " + java.util.Arrays.asList(request.getPath().getSegments()));
			System.out.println("query: " + request.getQuery());
			System.out.println("keys: " + request.getQuery().keySet());*/

			System.out.println("/update/ REQUEST: " + request.getMethod() + " " +  request.getTarget());

			long time = System.currentTimeMillis();

			response.set("Content-Type", "application/json");
			response.set("Server", "Knowledge Block/1.0 (SimpleFramework 4.0)");
			response.setDate("Date", time);
			response.setDate("Last-Modified", time);

			// get the path
			//Path path =  request.getPath();
			//String directory = path.getDirectory();
			//String name = path.getName();
			//String[] segments = path.getSegments();

			// Get the method
			String method = request.getMethod();

			// Get the Query
			Query query = request.getQuery();

			// and evaluate the input
			if (method.equals("POST")) {

				if (query.toString().contains("register=1")) {

					System.out.println ("Updating registration data");
					updateRegistrationData (request, response);
				} else {
					updateData(request, response);
				}
			} else if (method.equals("DELETE")) {
				// can't delete data via REST
				notFound(response, "DELETE bad request");
			} else if (method.equals("GET")) {
				getData (request, response);
			} else if (method.equals("PUT")) {
				notFound(response, "PUT bad request");
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
	 * Update some data given a request and send a response.
	 */
	public void updateData(Request request, Response response) throws IOException, JSONException  {

		// see if we should send xml
		//boolean xml = false;
		//boolean hasHTTPAccept = request.contains("Accept");

		/*if (hasHTTPAccept) {
			String accept = request.getValue("Accept");
			if (accept.equals("application/xml") || accept.equals("text/xml")) {
				xml = true;
			}
		}*/
		// get the path
		Path path =  request.getPath();
		String name = path.getName();

		Query query = request.getQuery();
		//Scanner scanner;

		/*		String uriPath;
		JSONObject value = null;

		// Convert path to uri
		uriPath = path.getPath(1);
		//uriPath = uriPath.replaceFirst("/","");
		if (name == null) {
			uriPath = uriPath + "/";
		}*/

		// process uriPath arg
		String uriPath=null;
		JSONObject value = null;

		if (query.containsKey("u")) {
			String entityArg = query.get("u");

			Scanner scanner = new Scanner(entityArg);

			if (scanner.hasNext()) {
				uriPath = scanner.next();
			} else {
				complain(response, "arg uriPath not correctly set");
				response.close();
				return;
			}

		} else {
			complain(response, "arg u not provided");
			response.close();
			return;
		}

		/* process  args */

		// process value arg
		if (query.containsKey("value")) {
			String valueArg = query.get("value");

			value = new JSONObject(valueArg);

			System.out.println("just got value" + value);
		} else {
			value = new JSONObject(request.getContent());

			System.out.println("JSON value " + value);

		}

		// did we get everything
		//if (value == null) {
		//	complain(response, "error in update " + " value == null");
		//}

		System.out.println(System.currentTimeMillis() + " UpdateHandler: " + uriPath + " -> " + value);

		// check if ircallbackURL is passed (i.e., for distributed virtual infrastructure deployment)
		String ircallbackURL=null;

		if (query.containsKey("ircallbackURL")) {
			ircallbackURL = query.get("ircallbackURL");
		}

		// checking compact version as well
		if (query.containsKey("ircbu")) {
			ircallbackURL = query.get("ircbu");
		}

		// interact with the entity to post value
		if (ircallbackURL==null)
			// pass the value in 
			listener.getEntity().UpdateValue(uriPath, value.toString());
		else 
			listener.getEntity().UpdateValueUSR(uriPath, value.toString(), ircallbackURL);

		/* try {
            if (uriPath.equals("/VIM/Routers/Detail/All")) {
                gc.processRouterJSONData(value);

            } else if (uriPath.equals("/VIM/Links/Detail/All")) {
                gc.processLinkJSONData(value);

            } else if (uriPath.equals("/VIM/Removed/")) {
                gc.processShutdownJSONData(value);

            } else {
                complain(response, "Bad key path " + uriPath);
                // check if the response is closed
                response.close();

                return;
            }

        } catch (JSONException jse) {
            System.err.println("JSONException " + jse.getMessage());
            jse.printStackTrace();
        }*/

		// and send them back as the return value
		PrintStream out = response.getPrintStream();

		JSONObject jsobj = new JSONObject();

		if (listener.getEntity().CheckCompactMode()) {
			jsobj.put("r", "OK");			
		} else {
			jsobj.put("uri", uriPath);
			jsobj.put("result", "OK");
		}
		/*		if (xml) {
			out.println(XML.toString(jsobj, "response"));
		} else {                       
			out.println(jsobj.toString());
		}*/

		out.println(jsobj.toString());


		//complain(response, "Bad key path " + uriPath);
		// check if the response is closed
		//response.close();

		//return;

	}

	/**
	 * Update some data given a request and send a response.
	 */
	public void updateRegistrationData(Request request, Response response) throws IOException, JSONException  {

		// see if we should send xml
		//boolean xml = false;
		//boolean hasHTTPAccept = request.contains("Accept");

		/*if (hasHTTPAccept) {
			String accept = request.getValue("Accept");
			if (accept.equals("application/xml") || accept.equals("text/xml")) {
				xml = true;
			}
		}*/

		//System.out.println ("LALAKIS");
		// get the path
		//Path path =  request.getPath();
		//String name = path.getName();

		Query query = request.getQuery();
		//Scanner scanner;

		//String uriPath;
		JSONObject value = null;

		// Convert path to uri

		/* process  args */

		// process value arg
		if (query.containsKey("value")) {
			String valueArg = query.get("value");

			value = new JSONObject(valueArg);

			System.out.println("just got value" + value);
		} else {
			value = new JSONObject(request.getContent());

			System.out.println("JSON value " + value);
		}

		// check if ifpcallbackURL passed (i.e., for distributed virtual infrastructure deployment)
		// process value arg
		String ifpcallbackURL=null;

		if (query.containsKey("ifpcallbackURL")) {
			ifpcallbackURL = query.get("ifpcallbackURL")+"&register=1";
		}

		// checking compact version as well
		if (query.containsKey("icurl")) {
			ifpcallbackURL = query.get("ifpcallbackURL")+"&register=1";
		}

		// did we get everything
		//if (value == null) {
		//	complain(response, "error in update " + " value == " + value);
		//	}

		// updating information flow policies
		if (ifpcallbackURL==null)
			listener.getEntity().InformationFlowPoliciesUpdated(value);
		else
			listener.getEntity().InformationFlowPoliciesUpdatedUSR(value, ifpcallbackURL);

		// and send them back as the return value
		PrintStream out = response.getPrintStream();

		JSONObject jsobj = new JSONObject();

		if (listener.getEntity().CheckCompactMode()) {
			jsobj.put("r", "OK");
		} else {
			jsobj.put("uri", "");
			jsobj.put("result", "OK");
		}

		//if (xml) {
		//	out.println(XML.toString(jsobj, "response"));
		//} else {                       
		out.println(jsobj.toString());
		//	}

		//complain(response, "Bad key path " + uriPath);
		// check if the response is closed
		//response.close();

		//return;

	}

	/**
	 * Get data given a request and send a response.
	 * Calls InformationExchangeInterface.RequestInformation
	 */
	public void getData(Request request, Response response) throws IOException, JSONException {
		// see if we should send xml
		/*boolean xml = false;
		boolean hasHTTPAccept = request.contains("Accept");

		if (hasHTTPAccept) {
			String accept = request.getValue("Accept");
			if (accept.equals("application/xml") || accept.equals("text/xml")) {
				xml = true;
			}
		}*/
		// get the path
		Path path =  request.getPath();
		String name = path.getName();

		Query query = request.getQuery();
		Scanner scanner;

		/*String uriPath;

		// Convert path to uri
		uriPath = path.getPath(1);
		//uriPath = uriPath.replaceFirst("/","");
		if (name == null) {
			uriPath = uriPath + "/";
		}*/
		/* process  args */

		// process entityid args
		/*@SuppressWarnings("unused")
		int entityid=0;
		if (query.containsKey("entityid")) {
			String entityArg = query.get("entityid");

			scanner = new Scanner(entityArg);

			if (scanner.hasNextInt()) {
				entityid = scanner.nextInt();
			} else {
				complain(response, "arg entityid is not an Integer");
				response.close();
				return;
			}

		} else {
			complain(response, "arg entityid not provided");
			response.close();
			return;
		}*/

		// process uriPath arg
		String uriPath=null;
		if (query.containsKey("u")) {
			String entityArg = query.get("u");

			scanner = new Scanner(entityArg);

			if (scanner.hasNext()) {
				uriPath = scanner.next();
			} else {
				complain(response, "arg uriPath not correctly set");
				response.close();
				return;
			}

		} else {
			complain(response, "arg u not provided");
			response.close();
			return;
		}

		// check if iccallbackURL passed (i.e., for distributed virtual infrastructure deployment)
		String iccallbackURL=null;

		if (query.containsKey("iccallbackURL")) {
			iccallbackURL = query.get("iccallbackURL");
		}

		// checking compact version as well
		if (query.containsKey("iccbu")) {
			iccallbackURL = query.get("iccbu");
		}

		// did we get everything
		//if (value == null) {
		//	complain(response, "error in update " + " value == " + value);
		//	}

		// interact with the entity to retrieve value
		JSONObject result = null;
		if (iccallbackURL==null)
			result = listener.getEntity().CollectValue(uriPath);
		else 
			result = listener.getEntity().CollectValueUSR(uriPath, iccallbackURL);

		// and send them back as the return value
		PrintStream out = response.getPrintStream();

		JSONObject jsobj = new JSONObject();

		//in case of distributed infrastructure
		//this does not need to be done in the last hop 

		if (iccallbackURL==null) {
			if (result == null) {
				if (listener.getEntity().CheckCompactMode()) {
					jsobj.put("e", "error");
				} else {
					jsobj.put("uri", uriPath);
					jsobj.put("error", "error");
					jsobj.put("result", new JSONObject("{}"));				
				}
			} else {
				//JSONObject JSONResult = new JSONObject(result);

				if (listener.getEntity().CheckCompactMode()) {
					jsobj.put("r", result);
				} else {
					jsobj.put("uri", uriPath);
					jsobj.put("result", result);
				}
			}
		} else {
			jsobj = result;
		}

		//if (xml) {
		//	out.println(XML.toString(jsobj, "response"));
		//} else {            

		out.println(jsobj.toString());
		//}

	}
}


