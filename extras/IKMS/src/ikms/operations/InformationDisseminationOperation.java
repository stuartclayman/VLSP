package ikms.operations;

// The ICD function performs information dissemination to a number of Entities that build knowledge or that act upon this 
// information, e.g., performing configuration changes. The information/knowledge is disseminated using one of the following methods:
// - Push method: The IKMS responds to a single information push request coming from an Entity using the Push method. The Information 
// & Knowledge Dissemination block periodically pushes updated information to the interested Entities (i.e., whenever it changes). 
// The Entities maintain the information in a local storage, from which they service either knowledge production or act upon the new information; 
// - Pull method: An entity may request information/knowledge using the Pull method. The Entities must explicitly request a particular type 
// of information and/or knowledge. They can either make these requests on a periodic basis (polling) or when a certain demand arises. 
// - Pub/sub method:  The Entities can be subscribed to receive a certain type of information and/or knowledge. They are automatically 
// informed when this information appears or changes (e.g., a change higher than a particular threshold). 

public class InformationDisseminationOperation {

	// Communicating operations
	InformationAuthorizationOperation informationAuthorizationOperation = null;

	// InformationDisseminationOperation constructor
	public InformationDisseminationOperation (InformationAuthorizationOperation informationAuthorizationOperation_) {
		// Initializing communicating operations
		informationAuthorizationOperation = informationAuthorizationOperation_;
	}
}
