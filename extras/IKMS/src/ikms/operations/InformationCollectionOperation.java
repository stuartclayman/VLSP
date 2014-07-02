package ikms.operations;

import ikms.util.LoggerFrame;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

// The IKMS is collecting information from a number of entities according to the information collection requirements 
// and should meet certain information collection constraints. Information collection could be one of four types: 
// (i) 1-time queries, which collect information that can be considered static, e.g., the number of CPUs 
// (ii) N-time queries, which collect information periodically for a certain number of times 
// (iii) continuous queries that collect information in an on-going manner
// (iv) unsolicited acquisition of subscribed information units. 

// Information collection is triggered from the IKMS, in response of an information retrieval request (in case 
// the requested information is not available in the storage).

public class InformationCollectionOperation {

	// InformationCollectionOperation Constructor
	public InformationCollectionOperation () {

	}

	/**
	 * Call remote entity using locationURL.
	 * We might use uri in future as a validation mechanism.
	 */
	public String CollectInformationUsingPull (int entityid, String locationURL, String uri) {

		System.out.println("InformationCollectionOperation: Collecting information using PULL for uri:"+uri+" from location:"+locationURL);


		// We  have code here that collects information from entities
		// using a REST interface on the entity
		try {
			// Make a Resty connection
			Resty rest = new Resty();

			// Call the relevant URL
			//System.out.println ("DEBUG:"+locationURL+uri);
			JSONObject jsobj = null;
			if (locationURL.contains("iccallbackURL")) {
				//distributed test-bed version running
				jsobj = rest.json(locationURL+"&entityid="+entityid+"&u="+uri).toObject();
			} else {
				jsobj = rest.json(locationURL+ uri+"?entityid="+entityid).toObject();
			}
			
			// Logging for internal IKMS functions workflow diagram
			LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationExchangeName, LoggerFrame.ICDFunctionName, "Collected Information", "ikmsfunctions");				
			System.out.println ("Returning the collected value:"+jsobj.toString());

			return jsobj.toString();
		}  catch (IOException ioe) {
			//ioe.printStackTrace();
			System.out.println("InformationCollectionOperation: cannot connect to location:"+locationURL);
			return null;
		} catch (JSONException je) {
			je.printStackTrace();
			System.out.println("InformationCollectionOperation: data error when collecting from location:"+locationURL);
		}
		
		// Logging for internal IKMS functions workflow diagram
		LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationExchangeName, LoggerFrame.ICDFunctionName, "Information cannot be collected", "ikmsfunctions");				

		return null;
	}

	public String CollectInformationUsingSubscribe (int entityid, String uri) {
		System.out.println("Collecting information for uri:"+uri+" from location:"+entityid);
		return null;
	}

}
