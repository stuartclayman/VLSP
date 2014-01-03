package demo_usr.ikms.client;

import ikms.client.InformationExchangeInterface;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import demo_usr.ikms.TFTP.RestOverTFTPClient;

public class InformationExchange implements InformationExchangeInterface {
	// A client of the IKMS itself
	IKMSUSRClient knowledgeBlockUSR=null;

	long informationRetrievalResponseTime = 0;

	/**
	 * Access the InformationExchangeInterface on host:port.
	 * All interaction is via REST calls.
	 */
	public InformationExchange(String host, String port, RestOverTFTPClient tftpClient) {
		knowledgeBlockUSR = new IKMSUSRClient(host, port, tftpClient);		
	}

	/**
	 * Request Information value using the Pull Method
	 * passes the id of the requesting Entity
	 */
	public JSONObject RequestInformation (int entityid, String key) throws IOException, JSONException {
		JSONObject output = null;

		output = knowledgeBlockUSR.requestInformation(entityid, key, String.valueOf(informationRetrievalResponseTime));

		return output;
	}

	public void CommunicateStatistics (int entityid, JSONObject statisticsJSON) throws IOException, JSONException {
		knowledgeBlockUSR.communicateStatistics(entityid, statisticsJSON);
	}

	/**
	 * Request Information value using the Pull Method
	 * passes the id of the requesting Entity and provide statistical information
	 */
	public JSONObject RequestInformation (int entityid, String key, String statistics) throws IOException, JSONException {
		return knowledgeBlockUSR.requestInformation(entityid, key, statistics);
	}

	/**
	 * Request Directly Information value from Entity using the Pull Method
	 * passes the id of the requesting Entity
	 */
	public JSONObject RequestDirectInformation (int entityid, String key, String uri) throws IOException, JSONException {
		JSONObject output = null;

		output = knowledgeBlockUSR.requestDirectInformation(entityid, key, uri);

		return output;
	}

	/**
	 * Share Information value to the IKMS
	 * passes the id of the Entity sharing the information
	 */
	public JSONObject ShareInformation (int entityid, String key, JSONObject value) throws IOException, JSONException {
		return knowledgeBlockUSR.shareInformation(entityid, key, value);
	}

	/**
	 * Publish an Information value to the IKMS
	 * passes the id of the Entity sharing the information
	 */
	public JSONObject PublishInformation (int entityid, String key, JSONObject value) throws IOException, JSONException {
		return knowledgeBlockUSR.publishInformation(entityid, key, value);
	}

	/**
	 * An entity subscribes for information to be sent out at a later date
	 */
	public JSONObject SubscribeForInformation (int entityid, String callbackURI, String key) throws IOException, JSONException {
		return knowledgeBlockUSR.subscribeForInformation(entityid, callbackURI, key);
	}
}
