package ikms.client;

import static us.monoid.web.Resty.content;
import static us.monoid.web.Resty.put;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

class IKMSClient {
	String kbHost;
	String kbPort;

	Resty rest;

	/**
	 * Constructor 
	 */
	IKMSClient(String host, String port) {
		kbHost = host;
		kbPort = port;

		// Make a Resty connection
		rest = new Resty();
	}

	/**
	 * New registration given a JSONObject which contains is the registration details.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject registerEntity(JSONObject registrationInfo) throws IOException, JSONException {
		String kbURL = "http://" + kbHost + ":" + kbPort + "/register/";

		System.out.println (registrationInfo.toString());
		// Call the relevant URL
		JSONObject jsobj = rest.json(kbURL, content(registrationInfo)).toObject();

		return jsobj;
	}

	/**
	 * Unregistering entity by its entityid.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject unRegisterEntity(int entityid) throws IOException, JSONException {
		String kbURL = "http://" + kbHost + ":" + kbPort + "/register/remove?entityid=" + entityid;

		// Call the relevant URL
		JSONObject jsobj = rest.json(kbURL).toObject();

		//JSONObject result = jsobj.getJSONObject("result");

		return jsobj;
	}


	/**
	 * Retrieve Entity registration information given the entityid.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject getEntityRegistration(int entityid) throws IOException, JSONException {
		String kbURL = "http://" + kbHost + ":" + kbPort + "/register/ENTITY?entityid="+entityid;

		// Call the relevant URL
		JSONObject jsobj = rest.json(kbURL).toObject();

		//JSONObject result = jsobj.getJSONObject("result");

		return jsobj;
	}

	/**
	 * Get some data using KnowledgeExchangeInterface.RequestInformation
	 */
	public JSONObject requestInformation(int entityid, String key) throws IOException, JSONException {
		// DO curl 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?entityid=200'
		String kbURL = "http://" + kbHost + ":" + kbPort + "/data" + key + "?entityid=" + entityid;

		// Call the relevant URL

		JSONObject jsobj = rest.json(kbURL).toObject();

		JSONObject result = null;
		if (jsobj.has("result"))
			result = jsobj.getJSONObject("result");

		// for compact version result
		if (jsobj.has("r"))
			result = jsobj.getJSONObject("r");

		return result;
	}

	/**
	 * Get some data using KnowledgeExchangeInterface.RequestInformation
	 */
	public JSONObject requestDirectInformation(int entityid, String key, String uri) throws IOException, JSONException {
		// DO curl 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?entityid=200'
		String kbURL = uri + key + "?entityid=" + entityid;
		System.out.println ("Directly requesting information from uri:" + kbURL);
		// Call the relevant URL

		JSONObject jsobj = rest.json(kbURL).toObject();
		//System.out.println ("Lefty:"+jsobj.toString());

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
		String kbURL = "http://" + kbHost + ":" + kbPort + "/data" + key + "?entityid=" + entityid+"&stats="+statistics;

		// Call the relevant URL

		JSONObject jsobj = rest.json(kbURL).toObject();

		JSONObject result = null;
		if (jsobj.has("result"))
			result = jsobj.getJSONObject("result");

		// for compact version result
		if (jsobj.has("r"))
			result = jsobj.getJSONObject("r");

		return result;
	}

	/**
	 * Set some data using KnowledgeExchangeInterface.ShareInformation
	 */
	public JSONObject shareInformation(int entityid, String key, JSONObject value) throws IOException, JSONException  {
		// DO curl -X POST -d '{"value": 550}' 
		// 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?entityid=100'
		String kbURL = "http://" + kbHost + ":" + kbPort + "/data" + key + "?entityid=" + entityid;

		// Call the relevant URL
		JSONObject jsobj = rest.json(kbURL, content(value)).toObject();

		return jsobj;
	}

	/**
	 * Communicating statistical information with the IKMS
	 */
	public void communicateStatistics (int entityid, JSONObject statistics) throws IOException, JSONException  {
		// DO curl -X POST -d '{"value": 550}' 
		// 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?entityid=100'
		String kbURL = "http://" + kbHost + ":" + kbPort + "/data" + "/measurements/?entityid=" + entityid+"&stats=1";

		// Call the relevant URL
		rest.json(kbURL, content(statistics)).toObject();
	}


	/**
	 * Set some data using KnowledgeExchangeInterface.PublishInformation
	 */
	public JSONObject publishInformation(int entityid, String key, JSONObject value) throws IOException, JSONException {
		// DO curl -X POST -d '{"value": 550}' 
		// 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?entityid=100&publish=1'
		String kbURL = "http://" + kbHost + ":" + kbPort + "/data" + key + "?entityid=" + entityid + "&publish=1";

		// Call the relevant URL
		JSONObject jsobj = rest.json(kbURL, content(value)).toObject();

		return jsobj;
	}

	/**
	 * Subscribe for data given a request and send a response.
	 * Calls KnowledgeExchangeInterface.SubscribeForInformation
	 * by doing a PUT on the KnowledgeBlock.
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
		String kbURL = "http://" + kbHost + ":" + kbPort + "/data" + key + "?entityid=" + entityid + "&callback=" + callbackURI;

		// Call the relevant URL
		JSONObject jsobj = rest.json(kbURL, put(content(""))).toObject();

		return jsobj;
	}

	/**
	 * New global performance optimization goal received from a Governance component
	 * Provides a JSONObject which contains the performance goal details.
	 * @return The result of the REST call as a JSONObject
	 */

	public JSONObject UpdatePerformanceGoal (int entityid, JSONObject performanceGoal) throws IOException, JSONException {
		String kbURL = "http://" + kbHost + ":" + kbPort + "/register/goal?entityid=" + entityid;
		//System.out.println ("kbURL:"+kbURL);
		// Call the relevant URL
		JSONObject jsobj = rest.json(kbURL, content(performanceGoal)).toObject();

		return jsobj;
	}

	/**
	 * Retrieve global performance optimization goal set from a governance component
	 * @return The result of the REST call as a JSONObject
	 */

	public JSONObject RetrievePerformanceGoal  (int entityid) throws IOException, JSONException {
		String kbURL = "http://" + kbHost + ":" + kbPort + "/register/goal?entityid=" + entityid;

		// Call the relevant URL
		JSONObject jsobj = rest.json(kbURL).toObject();

		//JSONObject result = jsobj.getJSONObject("result");

		return jsobj;
	}
}
