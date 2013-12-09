package demo_usr.ikms.client;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import demo_usr.ikms.TFTP.RestOverTFTPClient;

public class InformationManagementInterface {
	// A client of the IKMS itself
	IKMSUSRClient knowledgeBlockUSR=null;

	/**
	 * Access the InformationManagementInterface on host:port.
	 * All interaction is via REST calls.
	 */
	public InformationManagementInterface(String host, String port, RestOverTFTPClient tftpClient) {
		knowledgeBlockUSR = new IKMSUSRClient(host, port, tftpClient);
	}

	/**
	 * New registration given a JSONObject which contains is the registration details.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject RegisterEntity(JSONObject registrationInfo) throws IOException, JSONException {
			return knowledgeBlockUSR.registerEntity(registrationInfo);
	}


	/**
	 * Update Entity registration given a JSONObject which contains is the registration details.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject UpdateEntityRegistration (JSONObject registrationInfo) throws IOException, JSONException {
		// same as RegisterEntity
			return knowledgeBlockUSR.registerEntity(registrationInfo);
	}

	/**
	 * Retrieve Entity registration information given the entityid.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject GetEntityRegistration (int entityid) throws IOException, JSONException {
			return knowledgeBlockUSR.getEntityRegistration(entityid);
	}

	/**
	 * New global performance optimizaiton goal received from a Governance component
	 * Provides a JSONObject which contains the performance goal details.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject UpdatePerformanceGoal(int entityid, JSONObject performanceGoal) throws IOException, JSONException {
			return knowledgeBlockUSR.UpdatePerformanceGoal(entityid, performanceGoal);
	}

	/**
	 * Retrieve global performance optimization goal set from a Governance component
	 * @return The result of the REST call as a JSONObject
	 */

	public JSONObject RetrievePerformanceGoal (int entityid) throws IOException, JSONException {
			return knowledgeBlockUSR.RetrievePerformanceGoal(entityid);
	}
}
