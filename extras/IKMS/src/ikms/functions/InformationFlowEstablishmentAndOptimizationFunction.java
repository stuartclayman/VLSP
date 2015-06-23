package ikms.functions;

// The Information Flow Establishment & Optimization (IFEO) function regulates the information flow based on 
// the current state and the locations of the participating components (e.g., the management entities producing information). In particular, 
// it controls information collection handled from the ICD function, information aggregation in the IA operation, and aggregation 
// node placement. Furthermore, it guides a filtering system for information collection and aggregation points that can significantly 
// reduce the communication overhead. However, the reduction depends on the nature of the metric to be monitored. 

// List of Information Flow Establishment and Optimization operations: 
// - Information flow establishment 
// - Information flow optimization 
// - Information quality control

import ikms.IKMS;
import ikms.operations.InformationFlowConfigurationAndStatisticsOperation;
import ikms.operations.InformationQualityControllerOperation;

public class InformationFlowEstablishmentAndOptimizationFunction {
	// The IKMS itself
	IKMS ikms;

	// Defining functions it communicates with
	InformationStorageAndIndexingFunction informationStorageAndIndexingFunction = null;

	// Defining IFEO operations
	private InformationQualityControllerOperation informationQualityController=null;
	private InformationFlowConfigurationAndStatisticsOperation informationFlowConfigurationAndStatistics=null;

	public InformationFlowEstablishmentAndOptimizationFunction (IKMS ikms, String gcHost, String gcPort) {
		// keep a handle on the IKMS
		this.ikms = ikms;

		informationStorageAndIndexingFunction = ikms.getInformationStorageAndIndexingFunction();

		// Initialize operations
		informationFlowConfigurationAndStatistics = new InformationFlowConfigurationAndStatisticsOperation(informationStorageAndIndexingFunction, gcHost, gcPort);
		informationQualityController=new InformationQualityControllerOperation(informationFlowConfigurationAndStatistics, informationStorageAndIndexingFunction.entityRegistration);
	}
	
	public InformationQualityControllerOperation GetInformationQualityControllerOperation() {
		return informationQualityController;
	}
	
	public InformationFlowConfigurationAndStatisticsOperation GetInformationFlowConfigurationAndStatisticsOperation() {
		return informationFlowConfigurationAndStatistics;
	}
}
