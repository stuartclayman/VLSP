package ikms;

// The Information and Knowledge Management System. 
// The IKMS handles information from management applications level, produces higher information abstractions 
// and organizes communication of information and knowledge. 

//List of IKMS functions: 
//-	Information Collection & Dissemination ICD
//-	Information Storage and Indexing - ISI
//-	Information Processing and Knowledge Production - IPKP
//-	Information Flow Establishment and Optimization - IFEO

import ikms.operations.InformationFlowConfigurationAndStatisticsOperation;
import ikms.console.IKMSManagementConsole;
import ikms.core.Response;
import ikms.data.DataStoreManager;
import ikms.data.IKMSOptimizationGoal;
import ikms.functions.InformationCollectionAndDisseminationFunction;
import ikms.functions.InformationFlowEstablishmentAndOptimizationFunction;
import ikms.functions.InformationProcessingAndKnowledgeProductionFunction;
import ikms.functions.InformationStorageAndIndexingFunction;
import ikms.info.SelectedOptimizationGoal;
import ikms.interfaces.InformationExchangeInterface;
import ikms.interfaces.InformationManagementInterface;

import java.util.HashMap;
import java.util.Map;

public class IKMS {

	InformationCollectionAndDisseminationFunction informationCollectionAndDissemination = null;
	InformationFlowEstablishmentAndOptimizationFunction informationFlowEstablishmentAndOptimizationFunction = null;
	InformationProcessingAndKnowledgeProductionFunction informationProcessingAndKnowledgeProductionFunction = null;
	InformationStorageAndIndexingFunction informationStorageAndIndexingFunction = null;

	InformationFlowConfigurationAndStatisticsOperation informationFlowConfigurationAndStatisticsOperation = null;
	
	InformationExchangeInterface informationExchangeInterface = null;
	InformationManagementInterface informationManagementInterface = null;

	// The REST console
	IKMSManagementConsole console;

	// the class names Processors that do work
	// Label -> String
	HashMap<String, String> processorNameMap;

	// empty args
	String[] EMPTY = {};

	// whether to take measurements or not (for textmode version only)
	boolean measurementsActive = false;

	// measurements warm-up time (textmode version)
	double warmup = 0;
	
	// measurements total time (textmode version)
	double totaltime = 0;
	
	public IKMS () {
		processorNameMap = new HashMap<String, String>();
	}

	// Check if taking measurements process is active or not (for textmode version only)
	public boolean areMeasurementsActive () {
		return measurementsActive && InformationFlowConfigurationAndStatisticsOperation.IsFirstMessageReceived();
	}
	
	// Get measurements warmup period (for textmode version only)
	public double getMeasurementsWarmupPeriod () {
		return warmup;
	}
	
	// Get total measurement time  (for textmode version only)
	public double getTotalMeasurementsTime () {
		return totaltime;
	}
	
	protected boolean config() {
		processorNameMap.put("RouterMeasurementProcessor", "ikms.processor.RouterMeasurementProcessor");

		return true;
	}

	public boolean init(int port, String goalJSON, String dbHost, String dbPassword, String gcHost, String gcPort) {
		try {
			console = new IKMSManagementConsole(this, port);

			// Initialize functions
			informationStorageAndIndexingFunction = new InformationStorageAndIndexingFunction(this, dbHost, dbPassword);
			informationCollectionAndDissemination = new InformationCollectionAndDisseminationFunction(this);
			informationFlowEstablishmentAndOptimizationFunction = new InformationFlowEstablishmentAndOptimizationFunction(this, gcHost, gcPort);
			informationProcessingAndKnowledgeProductionFunction = new InformationProcessingAndKnowledgeProductionFunction(this);

			// Initialize interfaces
			informationExchangeInterface = new InformationExchangeInterface(this);
			informationManagementInterface = new InformationManagementInterface(this);

			IKMSOptimizationGoal goal = new IKMSOptimizationGoal (goalJSON);
			
			// Initialize default optimization goal
			informationFlowEstablishmentAndOptimizationFunction.GetInformationQualityControllerOperation().SetGlobalPerformanceOptimizationGoal(goal);

			System.out.println("Default global optimization goal set:"+SelectedOptimizationGoal.GetOptimizationGoal().toJSONString());

			return true;        
		} catch (Exception e) {
			System.err.println("IKMS init error: " + e.getMessage());
			return false;
		}

	}


	public boolean start() {
		console.start();

		for (Map.Entry<String, String> entry : processorNameMap.entrySet()) {
			String processorClassName = entry.getValue();
			String label = entry.getKey();

			System.err.println("About to start " + processorClassName);

			// Get the informationCollectionAndDissemination function
			// to start a processor
			Response response = informationCollectionAndDissemination.startProcessor(processorClassName, EMPTY);

			if (response.isSuccess()) {
			} else {
				System.err.println(label + " did not execute() " + response.getMessage());
			}

		}

		return true;        
	}


	public boolean stop() {
		// stop redis communication
		DataStoreManager.destroyKeyValueStorePool();
		// shut down pub/sub threads
		informationStorageAndIndexingFunction.informationStorage.ShutdownPubSubThreads();
		// stop processors
		console.stop();
		return true;        
	}

	// signal received to start taking measurements (in textmode version only)
	public void startMeasurements(double warmup_, double totaltime_) {		
		warmup = warmup_;
		totaltime = totaltime_;
		measurementsActive = true;
		System.out.println ("Enabling taking measurements. Warmup period:"+warmup+" total time:"+totaltime);
	}

	// signal received to stop taking measurements (in textmode version only)
	public void stopMeasurements() {
		System.out.println ("Disabling taking measurements.");
		measurementsActive = false;
		InformationFlowConfigurationAndStatisticsOperation.ResetFirstMeasurementReceived();
	}
	
	
	public InformationExchangeInterface getInformationExchangeInterface() {
		return informationExchangeInterface;
	}

	public InformationManagementInterface getInformationManagementInterface() {
		return informationManagementInterface;
	}

	public InformationCollectionAndDisseminationFunction getInformationCollectionAndDisseminationFunction() {
		return informationCollectionAndDissemination;
	}

	public InformationStorageAndIndexingFunction getInformationStorageAndIndexingFunction() {
		return informationStorageAndIndexingFunction;
	}

	public InformationFlowEstablishmentAndOptimizationFunction getInformationFlowEstablishmentAndOptimizationFunction () {
		return informationFlowEstablishmentAndOptimizationFunction;
	}
	public InformationProcessingAndKnowledgeProductionFunction getInformationProcessingAndKnowledgeProductionFunction () {
		return informationProcessingAndKnowledgeProductionFunction;
	}

}
