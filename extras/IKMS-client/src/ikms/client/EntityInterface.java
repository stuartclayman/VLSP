package ikms.client;

import us.monoid.json.JSONObject;

public interface EntityInterface {
	public void InformationFlowPoliciesUpdated (JSONObject informationFlowPolicies);
	public void InformationFlowPoliciesUpdatedUSR (JSONObject informationFlowPolicies, String targetURIFileName);
	public JSONObject CollectValue (String uri);
	public JSONObject CollectValueUSR (String uri, String targetURIFileName);
	public void UpdateValue (String uri, String value);
	public void UpdateValueUSR (String uri, String value, String ircallbackURL);
	public boolean CheckCompactMode ();
}
