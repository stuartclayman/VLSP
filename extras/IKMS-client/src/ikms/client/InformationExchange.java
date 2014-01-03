package ikms.client;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

public class InformationExchange implements InformationExchangeInterface {
	// A client of the IKMS
	IKMSClient ikms;

	/**
	 * Access the KnowledgeExchangeInterface on host:port.
	 * All interaction is via REST calls.
	 */
	public InformationExchange(String host, String port) {
		ikms = new IKMSClient(host, port);
	}


	/**
	 * Request Information value using the Pull Method
	 * passes the id of the requesting Entity
	 */
	public JSONObject RequestInformation (int entityid, String key) throws IOException, JSONException {
		JSONObject output = null;

		output = ikms.requestInformation(entityid, key);
		return output;
	}

	public void CommunicateStatistics (int entityid, JSONObject statisticsJSON) throws IOException, JSONException {
		ikms.communicateStatistics(entityid, statisticsJSON);
	}

	/**
	 * Request Information value using the Pull Method
	 * passes the id of the requesting Entity and provide statistical information
	 */
	public JSONObject RequestInformation (int entityid, String key, String statistics) throws IOException, JSONException {
		return ikms.requestInformation(entityid, key, statistics);
	}

	/**
	 * Request Directly Information value from Entity using the Pull Method
	 * passes the id of the requesting Entity and provide statistical information
	 */
	public JSONObject RequestDirectInformation (int entityid, String key, String uri) throws IOException, JSONException {
		JSONObject output = null;

		output = ikms.requestDirectInformation(entityid, key, uri);
		return output;
	}


	/**
	 * Share Information value to IKMS
	 * passes the id of the Entity sharing the information
	 */
	public JSONObject ShareInformation (int entityid, String key, JSONObject value) throws IOException, JSONException {
		return ikms.shareInformation(entityid, key, value);
	}

	/**
	 * Publish an Information value to IKMS
	 * passes the id of the Entity sharing the information
	 */
	public JSONObject PublishInformation (int entityid, String key, JSONObject value) throws IOException, JSONException {
		return ikms.publishInformation(entityid, key, value);
	}

	/**
	 * An Entity subscribes for information to be sent out at a later date
	 */
	public JSONObject SubscribeForInformation (int entityid, String callbackURI, String key) throws IOException, JSONException {
		return ikms.subscribeForInformation(entityid, callbackURI, key);
	}
}
