package ikms.client;

import us.monoid.json.JSONObject;

public interface EntityInterface {
	public void InformationFlowPoliciesUpdated (JSONObject informationFlowPolicies);
	public void InformationFlowPoliciesUpdatedUSR (JSONObject informationFlowPolicies, String targetURIFileName);
	public JSONObject GenerateTestValue (String uri);
	public void StoreInLocalStorage (String uri, String value);
	public boolean CheckCompactMode ();
}
