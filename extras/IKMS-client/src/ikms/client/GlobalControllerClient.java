package ikms.client;

import static us.monoid.web.Resty.content;
import static us.monoid.web.Resty.put;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

public class GlobalControllerClient {
	String gcHost;
	String gcPort;

	Resty rest;

	/**
	 * Constructor 
	 */
	public GlobalControllerClient (String gcHost_, String gcPort_) {
		gcHost = gcHost_;
		gcPort = gcPort_;

		// Make a Resty connection
		rest = new Resty();
	}

	/**
	 * Retrieve a JSONObject from the GC with localcontroller information, including energy efficiency related measurements
	 * @return The result of the REST call as a JSONObject
	 */
	public JSONObject retrieveLocalControllerInfo() throws IOException, JSONException {
		String kbURL = "http://" + gcHost + ":" + gcPort + "/localcontroller/?detail=all";

		// Call the relevant URL
		JSONObject jsobj = rest.json(kbURL).toObject();

		return jsobj;
	}

}
