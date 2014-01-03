package demo_usr.ikms.client;

import ikms.client.InformationManagementInterface;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import demo_usr.ikms.TFTP.RestOverTFTPClient;

public class InformationManagement implements InformationManagementInterface {
	// A client of the IKMS itself
	IKMSUSRClient ikmsUSR=null;

	/**
	 * Access the InformationManagementInterface on host:port.
	 * All interaction is via REST calls.
	 */
	public InformationManagement(String host, String port, RestOverTFTPClient tftpClient) {
		ikmsUSR = new IKMSUSRClient(host, port, tftpClient);
	}

	/**
	 * New registration given a JSONObject which contains is the registration details.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject RegisterEntity(JSONObject registrationInfo) throws IOException, JSONException {
		return ikmsUSR.registerEntity(registrationInfo);
	}

	/**
	 * Unregistering management entity by its entityid.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject UnregisterEntity (int entityid) throws IOException, JSONException {
		return ikmsUSR.unRegisterEntity(entityid);
	}

	/**
	 * Update Entity registration given a JSONObject which contains is the registration details.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject UpdateEntityRegistration (JSONObject registrationInfo) throws IOException, JSONException {
		// same as RegisterEntity
		return ikmsUSR.registerEntity(registrationInfo);
	}

	/**
	 * Retrieve Entity registration information given the entityid.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject GetEntityRegistration (int entityid) throws IOException, JSONException {
		return ikmsUSR.getEntityRegistration(entityid);
	}

	/**
	 * New global performance optimizaiton goal received from a Governance component
	 * Provides a JSONObject which contains the performance goal details.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject UpdatePerformanceGoal(int entityid, JSONObject performanceGoal) throws IOException, JSONException {
		return ikmsUSR.UpdatePerformanceGoal(entityid, performanceGoal);
	}

	/**
	 * Retrieve global performance optimization goal set from a Governance component
	 * @return The result of the REST call as a JSONObject
	 */

	public JSONObject RetrievePerformanceGoal (int entityid) throws IOException, JSONException {
		return ikmsUSR.RetrievePerformanceGoal(entityid);
	}
}
