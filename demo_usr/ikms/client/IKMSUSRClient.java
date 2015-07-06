package demo_usr.ikms.client;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import demo_usr.ikms.TFTP.RestOverTFTPClient;
import demo_usr.ikms.client.utils.Converters;

class IKMSUSRClient {
	String ikmsHost;
	String ikmsPort;

	RestOverTFTPClient tftpClient;

	/**
	 * Constructor 
	 */
	IKMSUSRClient(String host, String port, RestOverTFTPClient tftpClient) {
		ikmsHost = host;
		ikmsPort = port;

		this.tftpClient = tftpClient;
	}

	/**
	 * New registration given a JSONObject which contains is the registration details.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject registerEntity(JSONObject registrationInfo) throws IOException, JSONException {
		String kbURL = "http://" + ikmsHost + ":" + ikmsPort + "/register/";

		System.out.println (registrationInfo.toString());
		// Call the relevant URL
		
		boolean success = tftpClient.ApplyRestPostRequest(kbURL, registrationInfo.toString());
		//JSONObject jsobj = rest.json(kbURL, content(registrationInfo)).toObject();
		
		if (success) 
			return CreateResponseMessage ("registered successfully", "OK");	
		else
			return CreateResponseMessage ("registered unsuccessfully", "");				
	}

	/**
	 * Unregistering entity by its entityid.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject unRegisterEntity(int entityid) throws IOException, JSONException {
		String ikmsURL = "http://" + ikmsHost + ":" + ikmsPort + "/register/remove?entityid=" + entityid;

		// Call the relevant URL
		String result = 	tftpClient.ApplyRestGetRequest(ikmsURL);

		JSONObject jsobj = new JSONObject(result);

		return jsobj;
	}
	
	/**
	 * Retrieve Entity registration information given the entityid.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject getEntityRegistration(int entityid) throws IOException, JSONException {
		String kbURL = "http://" + ikmsHost + ":" + ikmsPort + "/register/ENTITY?entityid="+entityid;

		// Call the relevant URL
		//JSONObject jsobj = rest.json(kbURL).toObject();

		//JSONObject result = jsobj.getJSONObject("result");
		
		String result = 	tftpClient.ApplyRestGetRequest(kbURL);

		JSONObject jsobj = new JSONObject(result);
		return jsobj;
	}

	/**
	 * Get some data using InformationExchangeInterface.RequestInformation
	 */
	public JSONObject requestInformation(int entityid, String key) throws IOException, JSONException {
		// DO curl 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?entityid=200'
		String kbURL = "http://" + ikmsHost + ":" + ikmsPort + "/data" + key + "?entityid=" + entityid;

		// Call the relevant URL

		//JSONObject jsobj = rest.json(kbURL).toObject();

		//JSONObject result = jsobj.getJSONObject("result");
		//return result;
		
		String resultStr = 	tftpClient.ApplyRestGetRequest(kbURL);

		JSONObject jsobj = new JSONObject (resultStr);
		JSONObject result;
		
		if (jsobj.has("result"))
			result = jsobj.getJSONObject("result");
		
		// for compact version result
		if (jsobj.has("r"))
			result = jsobj.getJSONObject("r");		
		
		return jsobj;

	}

	/**
	 * Get some data using KnowledgeExchangeInterface.RequestInformation
	 */
	public JSONObject requestDirectInformation(int entityid, String key, String uri) throws IOException, JSONException {
		// DO curl 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?entityid=200'
		String kbURL = uri + "&u="+key + "&entityid=" + entityid;
		System.out.println ("Directly requesting information from uri:" + kbURL);
		
		// Call the relevant URL
		// We need to change the address from knowHost to the Entity to be directly connected to
		String oldHost = tftpClient.getHostName();
		int oldPort = tftpClient.getPort();
		
		Map<String, String> parameters = Converters.SplitQuery(kbURL);
		
		String newAddress = parameters.get("n");
		String newPort = parameters.get("p");
		System.out.println ("Directly requesting information from host:" + newAddress+":"+newPort);

		tftpClient.setHostName(String.valueOf(newAddress));
		tftpClient.setPort(Integer.valueOf(newPort));
		
		String resultstr = tftpClient.ApplyRestGetRequest(kbURL);
		// reverting back to the old address
		tftpClient.setHostName(oldHost);
		tftpClient.setPort(oldPort);
		
		JSONObject jsobj = null;

		if (resultstr == null || resultstr.equals(""))
			jsobj = new JSONObject("{}");
		else
			jsobj = new JSONObject(resultstr);

		JSONObject result = null;
		if (jsobj.has("result"))
			result = jsobj.getJSONObject("result");
		
		// for compact version result
		if (jsobj.has("r"))
			result = jsobj.getJSONObject("r");	
		
		return result;
	}

	/**
	 * Get some data using KnowledgeExchangeInterface.RequestInformation and pass response time statistical information
	 */
	public JSONObject requestInformation(int entityid, String key, String statistics) throws IOException, JSONException {
		// DO curl 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?entityid=200'
		String kbURL = "http://" + ikmsHost + ":" + ikmsPort + "/data" + key + "?entityid=" + entityid+"&stats="+statistics;

		// Call the relevant URL

		//JSONObject jsobj = rest.json(kbURL).toObject();
		String resultstr = 	tftpClient.ApplyRestGetRequest(kbURL);

		JSONObject jsobj = new JSONObject(resultstr);

		JSONObject result = null;
		if (jsobj.has("result"))
			result = jsobj.getJSONObject("result");
		
		// for compact version result
		if (jsobj.has("r"))
			result = jsobj.getJSONObject("r");
		
		return result;
	}

	/**
	 * Set some data using InformationExchangeInterface.ShareInformation
	 */
	public JSONObject shareInformation(int entityid, String key, JSONObject value) throws IOException, JSONException  {
		// DO curl -X POST -d '{"value": 550}' 
		// 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?entityid=100'
		String kbURL = "http://" + ikmsHost + ":" + ikmsPort + "/data" + key + "?entityid=" + entityid;

		// Call the relevant URL
		//JSONObject jsobj = rest.json(kbURL, content(value)).toObject();

		//return jsobj;

		tftpClient.ApplyRestPostRequest(kbURL, value.toString());
		//JSONObject jsobj = rest.json(kbURL, content(registrationInfo)).toObject();
		return new JSONObject();
	}

	/**
	 * Communicating statistical information with the IKMS
	 */
	public void communicateStatistics (int entityid, JSONObject statistics) throws IOException, JSONException  {
		// DO curl -X POST -d '{"value": 550}' 
		// 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?entityid=100'
		String kbURL = "http://" + ikmsHost + ":" + ikmsPort + "/data" + "/measurements/?entityid=" + entityid+"&stats=1";

		// Call the relevant URL
		//JSONObject jsobj = rest.json(kbURL, content(statistics)).toObject();
		tftpClient.ApplyRestPostRequest(kbURL, statistics.toString());

	}


	/**
	 * Set some data using InformationExchangeInterface.PublishInformation
	 */
	public JSONObject publishInformation(int entityid, String key, JSONObject value) throws IOException, JSONException  {
		// DO curl -X POST -d '{"value": 550}' 
		// 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?entityid=100&publish=1'
		String kbURL = "http://" + ikmsHost + ":" + ikmsPort + "/data" + key + "?entityid=" + entityid + "&publish=1";

		// Call the relevant URL
		//JSONObject jsobj = rest.json(kbURL, content(value)).toObject();

		//return jsobj;
		tftpClient.ApplyRestPostRequest(kbURL, value.toString());
		return new JSONObject();
	}

	/**
	 * Subscribe for data given a request and send a response.
	 * Calls InformationExchangeInterface.SubscribeForInformation
	 * by doing a PUT on the IKMS.
	 * <p>
	 * The caller on host remotehost, must listen for REST calls
	 * and handle the request http://remotehost:8500/update/
	 * and the key as part of the path.
	 * e.g. http://remotehost:8500/update/VIM/Removed
	 * The latest data is sent as a POST to the caller.     
	 */
	public JSONObject subscribeForInformation(int entityid, String callbackURI, String key) throws IOException, JSONException {
		// DO curl -X PUT
		// 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?entityid=100&callback=http://remotehost:8500/update/'
		String kbURL = "http://" + ikmsHost + ":" + ikmsPort + "/data" + key + "?entityid=" + entityid + "&callback=" + callbackURI;

		// Call the relevant URL
		//JSONObject jsobj = rest.json(kbURL, put(content(""))).toObject();

		//return jsobj;
		tftpClient.ApplyRestPostRequest(kbURL, "");
		//JSONObject jsobj = rest.json(kbURL, content(registrationInfo)).toObject();
		return new JSONObject();
	}

	/**
	 * New global performance optimization goal received from a Governance component
	 * Provides a JSONObject which contains the performance goal details.
	 * @return The result of the REST call as a JSONObject
	 */

	public JSONObject UpdatePerformanceGoal (int entityid, JSONObject performanceGoal) throws IOException, JSONException {
		String kbURL = "http://" + ikmsHost + ":" + ikmsPort + "/register/goal?entityid=" + entityid;

		// Call the relevant URL
		//JSONObject jsobj = rest.json(kbURL, content(performanceGoal)).toObject();

		//return jsobj;
		tftpClient.ApplyRestPostRequest(kbURL, performanceGoal.toString());
		return new JSONObject();
	}

	/**
	 * Retrieve global performance optimization goal set from a Governance component
	 * @return The result of the REST call as a JSONObject
	 */

	public JSONObject RetrievePerformanceGoal  (int entityid) throws IOException, JSONException {
		String kbURL = "http://" + ikmsHost + ":" + ikmsPort + "/register/goal?entityid=" + entityid;

		// Call the relevant URL
		//JSONObject jsobj = rest.json(kbURL).toObject();

		//JSONObject result = jsobj.getJSONObject("result");

		//return jsobj;
		String resultstr = 	tftpClient.ApplyRestGetRequest(kbURL);

		JSONObject jsobj = new JSONObject(resultstr);

		return jsobj;
	}

	public JSONObject CreateResponseMessage (String message, String output) {
		JSONObject jsobj = new JSONObject();

		try {
			jsobj.put("message", message);
			jsobj.put("output", output);  
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return jsobj;
	}
}
