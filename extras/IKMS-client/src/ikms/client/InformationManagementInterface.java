package ikms.client;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

public interface InformationManagementInterface {
	 /**
     * New registration given a JSONObject which contains is the registration details.
     * @return The result of the REST call as a JSONObject
     */
    public JSONObject RegisterEntity(JSONObject registrationInfo) throws IOException, JSONException;
    
    /**
	 * Unregistering management entity by its entityid.
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject UnregisterEntity (int entityid) throws IOException, JSONException;
    
    /**
     * Update Entity registration given a JSONObject which contains is the registration details.
     * @return The result of the REST call as a JSONObject
     */
    public JSONObject UpdateEntityRegistration (JSONObject registrationInfo) throws IOException, JSONException;
    
    /**
     * Retrieve Entity registration information given the entityid.
     * @return The result of the REST call as a JSONObject
     */
    public JSONObject GetEntityRegistration (int entityid) throws IOException, JSONException;
    
    /**
     * New global performance optimization goal received from a governance component
     * Provides a JSONObject which contains the performance goal details.
     * @return The result of the REST call as a JSONObject
     */
    public JSONObject UpdatePerformanceGoal(int entityid, JSONObject performanceGoal) throws IOException, JSONException;
    
	/**
	 * Retrieve global performance optimization goal set from a governance component
	 * @return The result of the REST call as a JSONObject
	 */
	
	public JSONObject RetrievePerformanceGoal (int entityid) throws IOException, JSONException;
}
