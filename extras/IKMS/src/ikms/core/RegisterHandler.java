package ikms.core;

import ikms.IKMS;
import ikms.console.IKMSManagementConsole;
import ikms.data.EntityRegistrationInformation;
import ikms.data.IKMSOptimizationGoal;
import ikms.data.InformationFlowRequirementsAndConstraints;
import ikms.interfaces.InformationManagementInterface;
import ikms.util.JSONData;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Scanner;

import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONString;
import cc.clayman.console.BasicRequestHandler;
import cc.clayman.console.RequestHandler;

/**
 * A class to handle /register/ requests
 */
public class RegisterHandler extends BasicRequestHandler implements RequestHandler {
	IKMSManagementConsole mc;

	public RegisterHandler() {
	}


	/**
	 * Handle a request and send a response.
	 */
	public boolean handle(Request request, Response response) {
		// get ManagementConsole
		mc = (IKMSManagementConsole)getManagementConsole();

		try {

			/*System.out.println("method: " + request.getMethod());
            System.out.println("target: " + request.getTarget());
            System.out.println("path: " + request.getPath());
            System.out.println("directory: " + request.getPath().getDirectory());
            System.out.println("name: " + request.getPath().getName());
            System.out.println("segments: " + java.util.Arrays.asList(request.getPath().getSegments()));
            System.out.println("query: " + request.getQuery());
            System.out.println("keys: " + request.getQuery().keySet());*/


			System.out.println("/register/ REQUEST: " + request.getMethod() + " " +  request.getTarget());

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
				newRegistration(request, response);
			} else if (method.equals("DELETE")) {
				// can't delete data via REST
				// but in future will be endRegistration()
				notFound(response, "DELETE bad request");
			} else if (method.equals("GET")) {
				if (name == null) {      // no arg, so list registrations
					listRegistrations(request, response);
				} else if (segments.length == 2) {   // get registration info
					System.out.println ("NAME:"+name);
					if (name.equals("goal")) {
						//retrieve performance optimization goal
						getPerformanceGoal (request, response);
					} else if (name.equals("terminate")) {
						// IKMS termination signal
						terminateIKMS ();
					} else if (name.equals("measureon")) {
						// Start IKMS measurements (for textmode only)
						startIKMSMeasurements(request);
					} else if (name.equals("measureoff")) {
						// stop IKMS measurements (for textmode only)
						stopIKMSMeasurements();
					} else if (name.equals("remove")) {
						// removing entity registration
						removeRegistration(request, response);
					} else {
						// retrieve Entity registration information
						getRegistration(request, response);
					}
				} else {
					notFound(response, "GET bad request");
				}
			} else if (method.equals("PUT")) {
				if (segments.length == 2) {   // update

					updateRegistration(request, response);
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
	 */
	public void getRegistration(Request request, Response response) throws IOException, JSONException {
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
		//Path path =  request.getPath();
		//String name = path.getName();

		Query query = request.getQuery();
		Scanner scanner;
		int entityid=0;

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
		InformationManagementInterface imi = ikms.getInformationManagementInterface();

		String result = null;
		try {
			result = imi.GetEntityRegistrationInfo(entityid);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("getRegistration: " + " for entity with entityid:" + entityid);

		JSONObject jsobj = new JSONObject();

		// and send them back as the return value
		PrintStream out = response.getPrintStream();

		//JSONObject jsobj = new JSONObject();

		if (result==null) {
			jsobj.put("message", "entity registration cannot be found or retrieved");
			jsobj.put("output", "");
		} else {
			jsobj.put("message", "retrieved successfully");
			jsobj.put("output", result);
		}
		//if (xml) {
		//	out.println(XML.toString(jsobj, "response"));
		//} else {                       
		//	out.println(jsobj.toString());
		//}
		out.println(jsobj.toString());
	}

	/**
	 * Get data given a request and send a response.
	 */
	public void removeRegistration(Request request, Response response) throws IOException, JSONException {
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
		//Path path =  request.getPath();
		//String name = path.getName();

		Query query = request.getQuery();
		Scanner scanner;
		int entityid=0;

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
		InformationManagementInterface imi = ikms.getInformationManagementInterface();

		String result = null;
		try {
			result = imi.UnregisterEntity(entityid);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("removingRegistration for entity with entityid:" + entityid);

		JSONObject jsobj = new JSONObject();

		// and send them back as the return value
		PrintStream out = response.getPrintStream();

		//JSONObject jsobj = new JSONObject();

		if (result==null) {
			jsobj.put("message", "entity registration cannot be found or retrieved");
			jsobj.put("output", "");
		} else {
			jsobj.put("message", "registration removed successfully");
			jsobj.put("output", result);
		}
		//if (xml) {
		//	out.println(XML.toString(jsobj, "response"));
		//} else {                       
		//	out.println(jsobj.toString());
		//}
		out.println(jsobj.toString());
	}

	public void getPerformanceGoal(Request request, Response response) throws IOException, JSONException {
		// see if we should send xml
		//boolean xml = false;

		/*boolean hasHTTPAccept = request.contains("Accept");

		if (hasHTTPAccept) {
			String accept = request.getValue("Accept");
			if (accept.equals("application/xml") || accept.equals("text/xml")) {
				xml = true;
			}
		}*/

		// get the path
		//Path path =  request.getPath();
		//String name = path.getName();

		Query query = request.getQuery();
		Scanner scanner;

		int entityid=0;

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
		InformationManagementInterface imi = ikms.getInformationManagementInterface();

		IKMSOptimizationGoal result = imi.GetPerformanceGoal(entityid);
		JSONObject jsobj = new JSONObject (result.toJSONString());

		// and send them back as the return value
		PrintStream out = response.getPrintStream();

		//JSONObject jsobj = new JSONObject();

		//if (xml) {
		//	out.println(XML.toString(jsobj, "response"));
		//} else {                       
		out.println(jsobj.toString());
		//}
	}

	/**
	 * New registration given a request and send a response.
	 */
	public void newRegistration(Request request, Response response) throws IOException, JSONException {
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

		/* process  args */


		// process value arg
		JSONString value = null;

		if (query.containsKey("value")) {
			String valueArg = query.get("value");

			value = new JSONData(valueArg);

			System.out.println("JSON value" + value);

		} else {
			value = new JSONData(request.getContent());

			System.out.println("JSON value" + value);

		}

		int entityid=0;

		/* process  args */



		try {
			if (name!=null) {

				// process entityid args
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
				}
				// new performance optimization goal received
				IKMSOptimizationGoal knowGoal = new IKMSOptimizationGoal (value.toJSONString());

				// interact with IKMS
				IKMS ikms = mc.getIKMS();
				InformationManagementInterface imi = ikms.getInformationManagementInterface();

				String result=null;
				try {
					result = imi.UpdatePerformanceGoal(entityid, knowGoal);
				} catch (Throwable t) {
					t.printStackTrace();
				}

				// and send them back as the return value
				PrintStream out = response.getPrintStream();

				JSONObject jsobj = new JSONObject();

				//jsobj.put("value", value);
				jsobj.put("message", result);
				jsobj.put("output", "");

				//if (xml) {
				//	out.println(XML.toString(jsobj, "response"));
				//} else {                       
				//	out.println(jsobj.toString());
				//}
				System.out.println ("goal set:"+jsobj.toString());
				out.println(jsobj.toString());
			} else {
				// new Entity registration received

				EntityRegistrationInformation entityRI=null;
				try {
					entityRI = new EntityRegistrationInformation(value.toJSONString());

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// interact with IKMS
				IKMS ikms = mc.getIKMS();
				InformationManagementInterface imi = ikms.getInformationManagementInterface();

				JSONObject jsobj = new JSONObject();
				PrintStream out = response.getPrintStream();

				if (entityRI!=null) {
					String forwarderIP = request.getClientAddress().getAddress().getHostAddress();

					// updating correct ikmsForwarderAddress
					if (entityRI.GetInformationFlowConstraints()!=null) {
						if (entityRI.GetInformationFlowConstraints().getIKMSClientURL()!=null) {
							System.out.println ("Received Registration Information with IKMSClientURL. Updating URL with forwarders IP:"+forwarderIP);
							String initialForwarderURI = entityRI.GetInformationFlowConstraints().getIKMSClientURL();
							URL initialForwarderURL = new URL (initialForwarderURI);
							InformationFlowRequirementsAndConstraints constraints = entityRI.GetInformationFlowConstraints();
							constraints.setIKMSClientURL("http://" + forwarderIP + ":"+initialForwarderURL.getPort()+"/update/");
							entityRI.EmbeddInformationFlowConstraints(constraints);
						}
					}
					// updating correct iccallbackurl
					/*if ((! entityRI.GetIcCallbackURL().equals(null))&&(! entityRI.GetIcCallbackURL().equals(""))) {
						System.out.println ("Received Registration Information with ICCallBackURL. Updating URL with forwarders IP:"+forwarderIP);
						String initialIcCallBackURI = entityRI.GetIcCallbackURL();
						URL initialIcCallBackURL = new URL (initialIcCallBackURI);
						entityRI.SetIcCallbackURL("http://" + forwarderIP + ":"+initialIcCallBackURL.getPort()+initialIcCallBackURL.getPath());
					}*/

					imi.RegisterEntity(entityRI, false);

					jsobj.put("message", "registered successfully");
					jsobj.put("output", "OK");  // add result, in case you need to return the negotiated policies

					//if (xml) {
					//	out.println(XML.toString(jsobj, "output"));
					//} else {                       
					//}
				} else {
					jsobj.put("message", "invalid MA Registration Info data structure provided.");
					jsobj.put("output", "");
				}

				out.println(jsobj.toString());
			}

		}  catch (IOException ioe) {
			System.err.println("IOException " + ioe);
			ioe.printStackTrace();
		} catch (JSONException je) {
			System.err.println("JSONException " + je);
			je.printStackTrace();
		} catch (Exception e) {
			System.err.println("Exception " + e);
			e.printStackTrace();
		}


	}

	/**
	 * List registrations given a request and send a response.
	 */
	public void listRegistrations(Request request, Response response) throws IOException, JSONException {
	}

	/**
	 * Update a registration given a request and send a response.
	 */
	public void updateRegistration(Request request, Response response) throws IOException, JSONException {
	}

	public void terminateIKMS () {
		mc.getIKMS().stop();
		System.exit(0);
	}

	public void startIKMSMeasurements (Request request) {
		Query query = request.getQuery();
		double warmup = 0;
		double totaltime = 0;
		if (query.containsKey("warmup")) {
			warmup = Integer.valueOf(query.get("warmup").toString());
		}
		if (query.containsKey("totaltime")) {
			totaltime = Integer.valueOf(query.get("totaltime").toString());
		}

		mc.getIKMS().startMeasurements(warmup, totaltime);
	}

	public void stopIKMSMeasurements () {
		mc.getIKMS().stopMeasurements();
	}

}

