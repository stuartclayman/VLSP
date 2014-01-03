package ikms.functions;

// The Information Processing and Knowledge Production function (IPKP) is responsible for operations related to information processing 
// (e.g., aggregation) and knowledge production. 

// A central operation of information processing is the information aggregation (IA). The IA can receive the collected data from the ICD 
// function and filter them out before they are stored to the ISI function or disseminated. 

// The Knowledge Production (KP) component handles and produces globally-scoped knowledge. This type of knowledge is being produced out 
// of aggregated information or locally-scoped knowledge. Locally-scoped knowledge, on the other hand, is built from the Knowledge 
// Building Entities out of data/information directly collected from the managed entities. In both cases, reasoning and inference mechanisms 
// are required. Thus, similar software components can be used.

// List of Information Processing and Knowledge Production operations: 
// - Information aggregation 
// - Knowledge production

import ikms.IKMS;
import ikms.operations.InformationAggregationOperation;
import ikms.operations.KnowledgeProductionOperation;

public class InformationProcessingAndKnowledgeProductionFunction {
	// The IKMS itself
	IKMS ikms;

	// Defining IPKP operations
	InformationAggregationOperation informationAggregation=null;
	KnowledgeProductionOperation knowledgeProduction=null;

	public InformationProcessingAndKnowledgeProductionFunction (IKMS ikms) {
		// keep a handle on the IKMS
		this.ikms = ikms;

		// Initialize operations
		informationAggregation=new InformationAggregationOperation();
		knowledgeProduction=new KnowledgeProductionOperation();
	}
}
