package ikms.operations;

import java.util.ArrayList;

// Checks if an information source or sink is authorized to communicate information with the IKMS.

public class InformationAuthorizationOperation {

	// Defining communicating operations
	EntityRegistrationOperation entityRegistrationOperation = null;
	
	// Information Authorization Constructor
	public InformationAuthorizationOperation (EntityRegistrationOperation entityRegistrationOperation_) {
		// Initializing communicating operations
		entityRegistrationOperation = entityRegistrationOperation_;
	}

	// Checks if the Entity is authorized to retrieve a uri
	public boolean CheckAuthorization (int entityid, String uri, String typeofuri) {
		boolean result = false;
		
		System.out.println ("Checking authorization.");
		System.out.println ("Checking if Entity and URI are registered.");
		result = CheckIfEntityAndUriAreRegistered (entityid,uri,typeofuri);
		
		return result;
	}
	
	// Checks if the Entity is authorized to retrieve a set of uris
	public boolean CheckAuthorization (int entityid, ArrayList<String> uris) {
		boolean result = false;
		
		System.out.println ("Checking authorization.");
		System.out.println ("Checking if Entity is registered.");
		result = CheckIfEntityIsRegistered (entityid);
		
		return result;
	}

	// Checks if the Entity is authorized to request IKMS to retrieve a set of uris and to apply an Aggregation Function
	public boolean CheckAggregationAuthorization (int entityid, ArrayList<String> uris, String aggregationFunction) {
		boolean result = false;
		
		System.out.println ("Checking authorization.");
		System.out.println ("Checking if Entity is registered.");
		result = CheckIfEntityIsRegistered (entityid);
		
		return result;
	}

	// Checks if the Entity is authorized to request IKMS to retrieve a set of uris and to apply a Knowledge Production Algorithm
	public boolean CheckKnowledgeProductionAuthorization (int entityid, ArrayList<String> uris, String knowledgeProductionAlgorithm) {
		boolean result = false;
		
		System.out.println ("Checking authorization.");
		System.out.println ("Checking if Entity is registered.");
		result = CheckIfEntityIsRegistered (entityid);
		
		return result;
	}
	
	public boolean CheckIfEntityAndUriAreRegistered (int entityid, String uri, String typeofuri) {
		return entityRegistrationOperation.CheckIfEntityAndUriAreRegistered(entityid, uri, typeofuri);
	}

	public boolean CheckIfEntityIsRegistered (int entityid) {
		return entityRegistrationOperation.CheckIfEntityIsRegistered(entityid);
	}

}
