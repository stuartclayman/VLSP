package ikms.client;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

public interface InformationExchangeInterface {

	/**
	 * Request Information value using the Pull Method
	 * passes the id of the requesting Entity
	 */
	public JSONObject RequestInformation (int entityid, String key) throws IOException, JSONException;

	public void CommunicateStatistics (int entityid, JSONObject statisticsJSON) throws IOException, JSONException;
	
	/**
	 * Request Information value using the Pull Method
	 * passes the id of the requesting Entity and provide statistical information
	 */
	public JSONObject RequestInformation (int entityid, String key, String statistics) throws IOException, JSONException;
	
	/**
	 * Request Directly Information value from Entity using the Pull Method
	 * passes the id of the requesting Entity and provide statistical information
	 */
	public JSONObject RequestDirectInformation (int entityid, String key, String uri) throws IOException, JSONException;


	/**
	 * Share Information value to IKMS
	 * passes the id of the Entity sharing the information
	 */
	public JSONObject ShareInformation (int entityid, String key, JSONObject value) throws IOException, JSONException;

	/**
	 * Publish an Information value to IKMS
	 * passes the id of the Entity sharing the information
	 */
	public JSONObject PublishInformation (int entityid, String key, JSONObject value) throws IOException, JSONException;

	/**
	 * An Entity subscribes for information to be sent out at a later date
	 */
	public JSONObject SubscribeForInformation (int entityid, String callbackURI, String key) throws IOException, JSONException;
}
