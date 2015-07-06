package ikms.core;

import ikms.IKMS;
import ikms.console.IKMSManagementConsole;
import ikms.interfaces.InformationExchangeInterface;
import ikms.util.JSONData;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.json.XML;
import cc.clayman.console.BasicRequestHandler;
import cc.clayman.console.RequestHandler;

/**
 * A class to handle /data/ requests
 */
public class DataHandler extends BasicRequestHandler implements RequestHandler {
	IKMSManagementConsole mc;

	public DataHandler() {
	}

	/*
      Extend Knowledge Block with REST interface for Information Exchange Interface:

      information retrieval - to get data from IKMS

      GET /data/uriPath?entity=entityid

      e.g.  http://ikms:9090/data/NetworkResources/VirtualNetworks/network1/Routers/router1/Interfaces/if0/Metrics/

      information sharing - to get data into the IKMS

      POST value='{ "inpackets": 12, "inloss": 1, "outpackets": 23, "outloss": 0 }' /data/uriPath?entity=entityid

      e.g.  http://ikms:9090/data/NetworkResources/VirtualNetworks/network1/Routers/router1/Interfaces/if0/Metrics/


      information dissemination - to publish data (via pub-sub) from the IKMS


      information collection - to get the IKMS to collect information (via a process) and respond at some later time (using a callback, possibly)
	 */

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

			System.out.println("/data/ REQUEST: " + request.getMethod() + " " +  request.getTarget());

			long time = System.currentTimeMillis();

			response.set("Content-Type", "application/json");
			response.set("Server", "Knowledge Block/1.0 (SimpleFramework 4.0)");
			response.setDate("Date", time);
			response.setDate("Last-Modified", time);

			// get the path
			Path path =  request.getPath();
			//String directory = path.getDirectory();
			//String name = path.getName();
			String[] segments = path.getSegments();

			// Get the method
			String method = request.getMethod();

			// Get the Query
			//Query query = request.getQuery();


			// and evaluate the input
			if (method.equals("POST")) {
				setData(request, response);
			} else if (method.equals("DELETE")) {
				// can't delete data via REST
				notFound(response, "DELETE bad request");
			} else if (method.equals("GET")) {
				if (segments.length >= 2) {   // get data info
					getData(request, response);
				} else {
					notFound(response, "GET bad request");
				}
			} else if (method.equals("PUT")) {
				if (segments.length >= 2) {   // subscribe for data 
					subscribeForData(request, response);
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
	 * Get data given a request and send a response.
	 * Calls KnowledgeExchangeInterface.RequestInformation
	 */
	public void getData(Request request, Response response) throws IOException, JSONException {
		// see if we should send xml
		boolean xml = false;

		boolean hasHTTPAccept = request.contains("Accept");

		if (hasHTTPAccept) {
			String accept = request.getValue("Accept");
			if (accept.equals("application/xml") || accept.equals("text/xml")) {
				xml = true;
			}
		}


		// get the path
		Path path =  request.getPath();
		String name = path.getName();

		Query query = request.getQuery();
		Scanner scanner;

		String uriPath;
		int entityid=0;

		// Convert path to uri
		uriPath = path.getPath(1);
		//uriPath = uriPath.replaceFirst("/","");
		if (name == null) {
			uriPath = uriPath + "/";
		}
		/* process  args */

		// process entityid args
		if (query.containsKey("entityid")) {
			String entityArg = query.get("entityid");

			scanner = new Scanner(entityArg);

			if (scanner.hasNextInt()) {
				entityid = scanner.nextInt();
			} else {
				complain(response, "arg entityid is not an Integer");
				response.close();
				scanner.close();
				return;
			}
			scanner.close();

		} else {
			complain(response, "arg entityid not provided");
			response.close();
			return;
		}

		// interact with IKMS
		IKMS ikms = mc.getIKMS();
		InformationExchangeInterface iei = ikms.getInformationExchangeInterface();

		System.out.println(System.currentTimeMillis() + " _IKMS_ RequestInformation: " + entityid + "  " + uriPath);

		String result = null;

		if (query.containsKey("stats")) {
			// statistical information (i.e., response time) provided
			result = iei.RequestInformation (entityid, uriPath, query.get("stats"));
		} else {
			result = iei.RequestInformation (entityid, uriPath);
		}		
		System.out.println (result);

		// and send them back as the return value
		PrintStream out = response.getPrintStream();

		JSONObject jsobj = new JSONObject();

		if (result == null) {
			jsobj.put("uri", uriPath);
			jsobj.put("error", "error");
			jsobj.put("result", new JSONObject("{}"));
		} else {
			JSONObject JSONResult = null;

			if (result.contains("=compact")) {
				result = result.replace("=compact", "");
				JSONResult = new JSONObject (result);
				jsobj.put("r", JSONResult);
			} else {
				JSONResult = new JSONObject (result);
				jsobj.put("uri", uriPath);
				jsobj.put("result", JSONResult);
			}
		}

		if (xml) {
			out.println(XML.toString(jsobj, "response"));
		} else {                       
			out.println(jsobj.toString());
		}

	}

	/**
	 * Set data given a request and send a response.
	 * Calls KnowledgeExchangeInterface.PublishInformation
	 */
	public void setData(Request request, Response response) throws IOException, JSONException {
		// see if we should send xml
		//boolean xml = false;

		//boolean hasHTTPAccept = request.contains("Accept");

		//if (hasHTTPAccept) {
			//String accept = request.getValue("Accept");
			//if (accept.equals("application/xml") || accept.equals("text/xml")) {
			//	xml = true;
			//}
		//}


		// get the path
		Path path =  request.getPath();
		String name = path.getName();

		Query query = request.getQuery();
		Scanner scanner;

		String uriPath;
		int entityid = 0;
		boolean publish = false;
		boolean stats = false;
		JSONData value = null;

		// Convert path to uri
		uriPath = path.getPath(1);
		//uriPath = uriPath.replaceFirst("/","");
		if (name == null) {
			uriPath = uriPath + "/";
		}

		/* process  args */

		// process entityid args
		if (query.containsKey("entityid")) {
			String entityArg = query.get("entityid");

			scanner = new Scanner(entityArg);

			if (scanner.hasNextInt()) {
				entityid = scanner.nextInt();
			} else {
				complain(response, "arg entityid is not an Integer");
				response.close();
				scanner.close();
				return;
			}
			scanner.close();
		} else {
			complain(response, "arg entityid not provided");
			response.close();
			return;
		}

		// process publish args
		if (query.containsKey("publish")) {
			String publishArg = query.get("publish");

			scanner = new Scanner(publishArg);

			if (scanner.hasNextInt()) {
				int publishValue = scanner.nextInt();

				if (publishValue >= 1) {
					publish = true;
				}
			} else {
				complain(response, "arg publish is not an Integer");
				response.close();
				scanner.close();
				return;
			}
			scanner.close();
		}

		// process publish args
		if (query.containsKey("stats")) {
			String statsArg = query.get("stats");
			scanner = new Scanner(statsArg);

			if (scanner.hasNextInt()) {
				int statsValue = scanner.nextInt();

				if (statsValue >= 1) {
					stats = true;
				}
			} else {
				complain(response, "arg stats is not an Integer");
				response.close();
				scanner.close();
				return;
			}
			scanner.close();
		}

		// process value arg
		if (query.containsKey("value")) {
			String valueArg = query.get("value");

			value = new JSONData(valueArg);


			System.out.println("just got value" + value);
		} else {
			value = new JSONData(request.getContent());

			System.out.println("JSON value " + value);

		}


		// did we get everything
		if (entityid == 0 || value == null) {
			complain(response, "error in setData " + " entityid == " + entityid + " value == " + value);
		}

		// interact with IKMS
		IKMS ikms = mc.getIKMS();
		InformationExchangeInterface iei = ikms.getInformationExchangeInterface();

		System.out.println(System.currentTimeMillis() + " _IKMS_ setData: " + entityid + " publish " + publish + "  " + uriPath + " -> " + value);

		//String result = kei.ShareInformation(entityid, uriPath, value);
		long result = 0;
		String output = null;

		if (stats) {
			// communicating statistical information to IKMS
			iei.CommunicateStatistics(entityid, value);
		} else {

			if (publish) {
				iei.PublishInformation(entityid, uriPath, value);
			} else {
				output = iei.ShareInformation(entityid, uriPath, value);
			}
		}

		// and send them back as the return value
		PrintStream out = response.getPrintStream();

		JSONObject jsobj = new JSONObject();

		if (stats) {
			jsobj.put("message", "");
			jsobj.put("output", "");
			jsobj.put("extra", "stats:1 uri:"+uriPath);

		} else {
			if (output.equals("")) {
				jsobj.put("message", "entity is not registered");
				jsobj.put("output", "");
				jsobj.put("extra", "publish:"+(publish ? 1 : 0)+" uri:"+uriPath);

			} else {
				jsobj.put("message", "information shared/published");
				jsobj.put("result", result);
				jsobj.put("extra", "publish:"+(publish ? 1 : 0)+" uri:"+uriPath);
			}
		}
		//		if (xml) {
		//			out.println(XML.toString(jsobj, "response"));
		//		} else {                       
		//			out.println(jsobj.toString());
		//		}
		out.println(jsobj.toString());
	}


	/**
	 * Subscribe for data given a request and send a response.
	 * Calls KnowledgeExchangeInterface.SubscribeForInformation
	 */
	public void subscribeForData(Request request, Response response) throws IOException, JSONException {
		// see if we should send xml
		boolean xml = false;

		boolean hasHTTPAccept = request.contains("Accept");

		if (hasHTTPAccept) {
			String accept = request.getValue("Accept");
			if (accept.equals("application/xml") || accept.equals("text/xml")) {
				xml = true;
			}
		}


		// get the path
		Path path =  request.getPath();
		String name = path.getName();

		Query query = request.getQuery();
		Scanner scanner;

		String uriPath;
		int entityid=0;
		String callbackURL;

		// Convert path to uri
		uriPath = path.getPath(1);
		//uriPath = uriPath.replaceFirst("/","");
		if (name == null) {
			uriPath = uriPath + "/";
		}

		/* process  args */

		// process entityid args
		if (query.containsKey("entityid")) {
			String entityArg = query.get("entityid");

			scanner = new Scanner(entityArg);

			if (scanner.hasNextInt()) {
				entityid = scanner.nextInt();
			} else {
				complain(response, "arg entityid is not an Integer");
				response.close();
				scanner.close();
				return;
			}
			scanner.close();

		} else {
			complain(response, "arg entityid not provided");
			response.close();
			return;
		}


		// process value arg
		if (query.containsKey("callback")) {
			String valueArg = query.get("callback");

			callbackURL = valueArg;

		} else {
			complain(response, "arg value not provided");
			response.close();
			return;
		}

		// interact with IKMS
		IKMS ikms = mc.getIKMS();
		InformationExchangeInterface iei = ikms.getInformationExchangeInterface();

		System.out.println("DataHandler: SubscribeForInformation: " + uriPath + " for " + entityid + " callback to " + callbackURL);

		String result = iei.SubscribeForInformation(entityid, callbackURL, uriPath);

		// and send them back as the return value
		PrintStream out = response.getPrintStream();

		JSONObject jsobj = new JSONObject();

		jsobj.put("uri", uriPath);
		jsobj.put("callback", callbackURL);
		jsobj.put("result", result);

		if (xml) {
			out.println(XML.toString(jsobj, "response"));
		} else {                       
			out.println(jsobj.toString());
		}

	}
}
