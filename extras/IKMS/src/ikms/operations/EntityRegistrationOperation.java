package ikms.operations;

// All Entities should be registered to the IKMS. This process include their information requirements and capabilities. 
// The ISI function maintains an Entities registry, including specifications for the available information to be collected, retrieved 
// or disseminated. 

import ikms.data.DataStoreManager;
import ikms.data.EntityRegistrationInformation;
import ikms.data.InformationFlowRequirementsAndConstraints;
import ikms.util.LoggerFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class EntityRegistrationOperation {
	//DataStoreManager dataStoreManager;	

	public EntityRegistrationOperation () {
		//dataStoreManager = dataStoreManager_;
	}

	public EntityRegistrationInformation GetIKMSEntityRegistrationInformation () {
		InformationFlowRequirementsAndConstraints ikmsConstraints = new InformationFlowRequirementsAndConstraints();
		EntityRegistrationInformation ikmsRegistrationInfo = new EntityRegistrationInformation(-1,"IKMS",null, ikmsConstraints, null, null, null, null, null, null, null, null, null);

		return ikmsRegistrationInfo;
	}

	public String GetEntityRegistrationInfoFromStorage (int entityid) {
		String value = null;

		String uri = "EntityInfo://"+entityid+"/";
		value = DataStoreManager.IKMSDBGet(uri);
		System.out.println ("Fetching Entity registration info:"+entityid+" from Entity registration storage, value:"+value);
		return value;
	}

	public String StoreEntityRegistrationInfo (int entityid, String entityInformation) {
		String output = DataStoreManager.IKMSDBSet("EntityInfo://"+entityid, entityInformation);
		System.out.println ("Storing Entity registration info for entity:"+entityid+" to Entity registration storage, output:"+output);

		return output;
	}

	public long RemoveEntityRegistrationInfo (int entityid) {
		long output = DataStoreManager.IKMSDBDel("EntityInfo://"+entityid);
		System.out.println ("Removing Entity registration info for entity:"+entityid+" from Entities registration storage, output:"+output);

		return output;
	}

	@SuppressWarnings("rawtypes")
	public long RemoveEntityRegistrationInfo (int entityid, ArrayList urisprovided, ArrayList urisrequested) {
		long output=0;

		System.out.println ("Removing Entity registration info for entity:"+entityid+" to Entities registration storage (entityid)");
		output = DataStoreManager.IKMSDBDel("EntityInfo://"+entityid);

		System.out.println ("Removing Entity registration info for entity:"+entityid+" to Entities registration storage (provided uris), output:"+output);
		output = RemoveArrayListFromEntitiesRegistry (entityid, urisprovided, "provided");
		RemoveEntityIndexesFromUris (entityid, urisprovided, "available");

		System.out.println ("Removing Entity registration info for entity:"+entityid+" to Entities registration storage (required uris), output:"+output);
		output = RemoveArrayListFromEntitiesRegistry (entityid, urisrequested, "required");
		RemoveEntityIndexesFromUris (entityid, urisrequested, "required");

		return output;
	}

	public String StoreEntityRegistrationInfo (int entityid, ArrayList<String> urisprovided, ArrayList<String> urisrequested) {

		String output = null;

		System.out.println ("Storing Entity registration info for entity:"+entityid+" to Entity registration storage (entityid), output:"+output);
		output = DataStoreManager.IKMSDBSet("EntityInfo://"+entityid, "Test Entity");

		System.out.println ("Storing Entity registration info for entity:"+entityid+" to Entity registration storage (provided uris), output:"+output);
		output = StoreArrayListToEntityRegistry (entityid, urisprovided, "provided");
		StoreEntityIndexesFromUris (entityid, urisprovided, "available");

		System.out.println ("Storing Entity registration info for entity:"+entityid+" to Entity registration storage (required uris), output:"+output);
		output = StoreArrayListToEntityRegistry (entityid, urisrequested, "required");
		StoreEntityIndexesFromUris (entityid, urisrequested, "required");

		return output;
	}

	public String StoreEntityRegistrationInfo (EntityRegistrationInformation entityRegistrationInfo) {

		String registrationInfoJsonString = null;

		if (entityRegistrationInfo.GetInformationFlowConstraints()!=null)
			registrationInfoJsonString = entityRegistrationInfo.toJSONString(entityRegistrationInfo.GetInformationFlowConstraints().getFlowOptimizationGoal());
		else
			registrationInfoJsonString = entityRegistrationInfo.toJSONString();

		String output=null;
		int entityid = entityRegistrationInfo.GetEntityId();

		System.out.println ("Storing Entity registration info for entity:"+entityid+" to Entities registration storage (entityid), output:"+registrationInfoJsonString);
		output = DataStoreManager.IKMSDBSet("EntityInfo://"+entityid+"/", registrationInfoJsonString);

		System.out.println ("Storing Entity registration info for entity:"+entityid+" to Entities registration storage (provided uris), output:"+output);
		StoreArrayListToEntityRegistry (entityid, entityRegistrationInfo.GetUrisForAvailableInformation(), "provided");

		StoreEntityIndexesFromUris (entityid, entityRegistrationInfo.GetUrisForAvailableInformation(), "available");

		System.out.println ("Storing Entity registration info for entity:"+entityid+" to Entities registration storage (required uris), output:"+output);
		StoreArrayListToEntityRegistry (entityid, entityRegistrationInfo.GetUrisForRequiredInformation(), "required");
		StoreEntityIndexesFromUris (entityid, entityRegistrationInfo.GetUrisForRequiredInformation(), "required");

		System.out.println ("Storing Entity registration info for entity:"+entityid+" to Entities registration storage (knowledge production uris), output:"+output);
		StoreArrayListToEntityRegistry (entityid, entityRegistrationInfo.GetUrisForKnowledge(), "knowledge production");

		CheckIfKnowledgeProductionNeedsTriggering (entityid, entityRegistrationInfo.GetUrisForKnowledge(), entityRegistrationInfo.GetKnowledgeBuildingRequestURLs(), entityRegistrationInfo.GetKnowledgeProductionOnRegistration());

		return output;
	}

	public long RemoveEntityRegistrationInfo (EntityRegistrationInformation entityRegistrationInfo) {

		long output=0;
		int entityid = entityRegistrationInfo.GetEntityId();

		System.out.println ("Removing Entity registration info for entity:"+entityid+" from Entities registration storage (entityid)");
		output = DataStoreManager.IKMSDBDel("EntityInfo://"+entityid+"/");

		System.out.println ("Removing Entity registration info for entity:"+entityid+" from Entities registration storage (provided uris), output:"+output);
		RemoveArrayListFromEntitiesRegistry (entityid,entityRegistrationInfo.GetUrisForAvailableInformation(), "provided");

		RemoveEntityIndexesFromUris (entityid, entityRegistrationInfo.GetUrisForAvailableInformation(), "available");

		System.out.println ("Removing Entity registration info for entity:"+entityid+" from Entities registration storage (required uris), output:"+output);
		RemoveArrayListFromEntitiesRegistry (entityid, entityRegistrationInfo.GetUrisForRequiredInformation(), "required");
		RemoveEntityIndexesFromUris (entityid, entityRegistrationInfo.GetUrisForRequiredInformation(), "required");

		System.out.println ("Removing Entity registration info for entity:"+entityid+" from Entities registration storage (knowledge production uris), output:"+output);
		RemoveArrayListFromEntitiesRegistry (entityid, entityRegistrationInfo.GetUrisForKnowledge(), "knowledge production");

		return output;
	}


	public void CheckIfKnowledgeProductionNeedsTriggering (int entityid, ArrayList<String> uris, ArrayList<String> knowledgebuildingrequesturls, ArrayList<Boolean> knowledgeproductiononregistration) {
		String uri = null;
		String requestURL = null;
		Boolean requiresTriggering = null;

		//get an Iterator object for ArrayList using iterator() method.
		Iterator<String> uriitr = uris.iterator();
		Iterator<String> urlitr = knowledgebuildingrequesturls.iterator();
		Iterator<Boolean> triggeritr = knowledgeproductiononregistration.iterator();
		while(uriitr.hasNext()) {
			uri = (String) uriitr.next();
			requestURL = (String)urlitr.next();
			requiresTriggering = (Boolean)triggeritr.next();
			System.out.println ("Knowledge Production Facility available for uri:"+uri+" at URL:"+requestURL+" requires triggering:"+requiresTriggering);
			if (requiresTriggering) {
				// Triggering knowledge production
				TriggerKnowledgeProduction (entityid, uri, requestURL);
			}
		}
	}

	public void TriggerKnowledgeProduction (int entityid, String uri, String requestURL) {
		// Logging for internal IKMS functions workflow diagram
		@SuppressWarnings("unused")
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IKMSName, "Triggering knowledge production", "ikmsfunctions");	

		System.out.println ("Triggering knowledge production for uri:"+uri+" and URL:"+requestURL);		
	}

	public String StoreEntityIndexesFromUris (int entityid, ArrayList<String> uris, String typeofuri) {
		String output = null;
		String uri = null;

		//get an Iterator object for ArrayList using iterator() method.
		Iterator<String> itr = uris.iterator();

		//use hasNext() and next() methods of Iterator to iterate through the elements
		while(itr.hasNext()) {
			if (typeofuri.equals("available"))
				uri = "EntityProvidingInfo://"+typeofuri+"/"+itr.next();
			if (typeofuri.equals("required"))
				uri = "EntityRequiringInfo://"+typeofuri+"/"+itr.next();
			if (typeofuri.equals("knowledge production"))
				uri = "EntityProducingKnowledge://"+typeofuri+"/"+itr.next();

			output = DataStoreManager.IKMSDBSet(uri, String.valueOf(entityid));

			System.out.println ("Indexing:"+entityid+" by uri:"+uri+" output:"+output);
		}

		return output;
	}

	@SuppressWarnings("rawtypes")
	public long RemoveEntityIndexesFromUris (int entityid, ArrayList uris, String typeofuri) {
		long output = 0;
		String uri = null;

		//get an Iterator object for ArrayList using iterator() method.
		Iterator itr = uris.iterator();

		//use hasNext() and next() methods of Iterator to iterate through the elements
		while(itr.hasNext()) {
			if (typeofuri.equals("available"))
				uri = "EntityProvidingInfo://"+typeofuri+"/"+itr.next();
			if (typeofuri.equals("required"))
				uri = "EntityRequiringInfo://"+typeofuri+"/"+itr.next();
			if (typeofuri.equals("knowledge production"))
				uri = "EntityProducingKnowledge://"+typeofuri+"/"+itr.next();

			output = DataStoreManager.IKMSDBDel(uri);

			System.out.println ("Removing indexes from entity:"+entityid+" with uri:"+uri+" output:"+output);
		}

		return output;
	}


	public String StoreArrayListToEntityRegistry (int entityid, ArrayList<String> uris, String typeofuri) {
		String output = null;
		String uri = null;

		//get an Iterator object for ArrayList using iterator() method.
		Iterator<String> itr = uris.iterator();

		//use hasNext() and next() methods of Iterator to iterate through the elements
		while(itr.hasNext()) {
			uri = "EntityInfo://"+entityid+"/"+typeofuri+"/"+itr.next();
			output = DataStoreManager.IKMSDBSet(uri, "");
			//System.out.println ("Storing Entity registration info for entity:"+entityid+" to Entity registration storage, uri:"+uri+" output:"+output);
		}

		return output;
	}

	@SuppressWarnings("rawtypes")
	public long RemoveArrayListFromEntitiesRegistry (int entityid, ArrayList uris, String typeofuri) {
		long output=0;
		String uri = null;

		//get an Iterator object for ArrayList using iterator() method.
		Iterator itr = uris.iterator();

		//use hasNext() and next() methods of Iterator to iterate through the elements
		ArrayList<String> urisToRemove = new ArrayList<String>();
		while(itr.hasNext()) {
			uri = "EntityInfo://"+entityid+"/"+typeofuri+"/"+itr.next();
			urisToRemove.add(uri);
			//System.out.println ("Storing entity registration info for entity:"+entityid+" to Entities registration storage, uri:"+uri+" output:"+output);
		}
		output += DataStoreManager.IKMSDBDel(urisToRemove);

		return output;
	}		

	public HashMap<Integer, ArrayList<String>> GetEntitiesFromUris (@SuppressWarnings("rawtypes") ArrayList uris, String typeofuri) {
		HashMap<Integer, ArrayList<String>> result = new HashMap<Integer, ArrayList<String>>();
		String output = "";
		String uri = null;
		int tempId;

		//get an Iterator object for ArrayList using iterator() method.
		Iterator<?> itr = uris.iterator();
		System.out.println ("Checking URIs:"+uris.toString()+" typeofuri:"+typeofuri);
		//use hasNext() and next() methods of Iterator to iterate through the elements
		while(itr.hasNext()) {

			if (typeofuri.equals("available")) {
				uri = "EntityProvidingInfo://"+typeofuri+"/"+itr.next();
			} else if (typeofuri.equals("required")) {
				uri = "EntityRequiringInfo://"+typeofuri+"/"+itr.next();
			} else if (typeofuri.equals("knowledge production")) {
				uri = "EntityProducingKnowledge://"+typeofuri+"/"+itr.next();
			} else { return null;}

			output = DataStoreManager.IKMSDBGet(uri);

			System.out.println ("Looking up Entity by uri:"+uri+" output:"+output+" type of uri:"+typeofuri);

			ArrayList<String> tempUris=null;
			if (output!=null&&(!output.equals(""))) {
				tempId = Integer.valueOf(output);
			} else {
				// flow with IKMS block
				tempId = -1;
			}

			// keep it only if it is not there
			tempUris = result.get(tempId);
			if (tempUris!=null) {

				// update record
				// add uri, if it is not there
				if (!tempUris.contains(uri)) {
					tempUris.add(uri);
					result.put(tempId, tempUris);
				}
			} else {

				// add new record
				tempUris = new ArrayList<String>();
				tempUris.add(uri);
				result.put(tempId, tempUris);
			}
		}

		return result;
	}

	public boolean CheckIfEntityIsRegistered (int entityid) {
		String uri = "EntityInfo://"+entityid+"/";
		String value = DataStoreManager.IKMSDBGet(uri);

		if (value==null) {
			System.out.println ("Entity is not registered.");
			return false;
		} else {
			System.out.println ("Entity is registered.");
			return true;			
		}
	}

	public boolean CheckIfEntityAndUriAreRegistered (int entityid, String uri, String typeofuri) {
		String fulluri = "EntityInfo://"+entityid+"/"+typeofuri+"/"+uri;

		System.out.println("EntityRegistrationInfo fulluri = " + fulluri);

		//wildcard matching or not
		String value=null;
		Set<String> values=null;
		if (fulluri.endsWith("All")||fulluri.endsWith("*")) {
			String searchuri = fulluri.replace("All", "*");
			System.out.println(searchuri);

			values = DataStoreManager.IKMSDBKeys(searchuri);
			System.out.println("found:"+values.size()+" keys");
		} else {
			value = DataStoreManager.IKMSDBGet(fulluri);
		} 

		if (value==null&&values==null&&AuthorizeBreakingDownUri (fulluri)==false) {
			System.out.println ("Entity and URI are not registered.");
			return false;
		} else {
			System.out.println ("Entity and URI are registered.");
			return true;			
		}
	}

	Boolean AuthorizeBreakingDownUri (String uri) {
		String[] uris = uri.split("/");
		String result="";
		String value=null;

		for( int i = 0; i <= uris.length - 1; i++)
		{
			if (result=="EntityInfo:")
				result+="/";

			if (!result.equals(""))
				result+="/";

			result+=uris[i];
			//System.out.println("Checking URI:"+result+"/All");
			value = DataStoreManager.IKMSDBGet(result+"/All");
			if (value!=null) {
				return true;
			}
		}
		return false;
	}
}
