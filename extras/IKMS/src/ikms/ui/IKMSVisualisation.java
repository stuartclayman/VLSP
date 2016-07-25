package ikms.ui;

import ikms.IKMS;
import ikms.info.SelectedOptimizationGoal;
import ikms.operations.InformationFlowConfigurationAndStatisticsOperation;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class IKMSVisualisation {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param args
	 */

	private JPanel contentPane;

	private JFrame ikmsFrame;
	
	// measurement figures for all entities
	private PerformanceMeasurementGraph responseTimeGraph;
	private PerformanceMeasurementGraph informationFreshnessGraph;
	private PerformanceMeasurementGraph processingCostGraph;
	private PerformanceMeasurementGraph memoryStateGraph;
	// measurement figures for monitored entities
	private PerformanceMeasurementGraph responseTimeGraphForMonitoredEntities;
	private PerformanceMeasurementGraph informationFreshnessGraphForMonitoredEntities;

	Thread figuresUpdatingThread;

	double currentTime=0;

	// show figures or not (maximum three should be true)
	boolean showResponseTimeGraph;
	boolean showInformationFreshnessGraph;
	boolean showResponseTimeGraphForMonitoredEntities;
	boolean showInformationFreshnessGraphForMonitoredEntities;
	boolean showProcessingCostGraph;
	boolean showMemoryStateGraph;
	boolean showFlowsStatistics;
	boolean textMode;

	private InformationFlowConfigurationAndStatisticsOperation informationFlowConfigurationAndStatisticsOperation = null;

	private MeasurementsLogFile logOutput = new MeasurementsLogFile ("output.txt");

	private JLabel totalFlowsLabel;
	private JLabel totalPushPullFlowsLabel;
	private JLabel totalDirectFlowsLabel;
	private JLabel totalPubSubFlowsLabel;

	private IKMS ikms = null;

	// variables for periodic measurements
	private double responseTime;
	private double informationFreshness;
	private double responseTimeForMonitoredEntities;
	private double informationFreshnessForMonitoredEntities;
	private double cpu;
	private double lastcpu;
	private double laststorage;
	private double storage;
	private double numberOfFlows;
	private double numberOfPushPullFlows;
	private double numberOfDirectFlows;
	private double numberOfPubSubFlows;
		
	public IKMSVisualisation(IKMS ikms_, boolean showResponseTimeGraph_, boolean showInformationFreshnessGraph_, boolean showResponseTimeGraphForMonitoredEntities_, boolean showInformationFreshnessGraphForMonitoredEntities_, boolean showProcessingCostGraph_, boolean showMemoryStateGraph_, boolean showFlowsStatistics_, int windowX_, int windowY_, int windowWidth_, int windowHeight_, boolean textMode_) {

		// initialize variables
		ikms = ikms_;
		informationFlowConfigurationAndStatisticsOperation = ikms.getInformationFlowEstablishmentAndOptimizationFunction().GetInformationFlowConfigurationAndStatisticsOperation();
		textMode = textMode_;
		lastcpu = 0.0;
		laststorage = 0.0;
		
		// show graphical user interface is textMode=false

		if (textMode==false) {
			ikmsFrame = new JFrame();
			
			showResponseTimeGraph = showResponseTimeGraph_;
			showInformationFreshnessGraph = showInformationFreshnessGraph_;
			showResponseTimeGraphForMonitoredEntities = showResponseTimeGraphForMonitoredEntities_;
			showInformationFreshnessGraphForMonitoredEntities = showInformationFreshnessGraphForMonitoredEntities_;
			showProcessingCostGraph = showProcessingCostGraph_;
			showMemoryStateGraph = showMemoryStateGraph_;		
			showFlowsStatistics = showFlowsStatistics_;

			// add window lister for terminating UI
			ikmsFrame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					TerminateIKMSUI ();
				}
			});

			ikmsFrame.setTitle("DOLFIN Information Database");
			ikmsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			ikmsFrame.setBounds (windowX_, windowY_, windowWidth_, windowHeight_);
			//setBounds(100, 100, 1105, 845);
			contentPane = new JPanel();
			ikmsFrame.setContentPane(contentPane);
			
			// set white background
			contentPane.setBackground(Color.white);

			contentPane.setLayout(new GridLayout(0, 1, 0, 0));
			// current path: System.out.println (this.getClass().getResource("").getPath());
			JLabel label = new JLabel(new ImageIcon(getClass().getResource("resources/ikms.png")));
			label.setBorder(BorderFactory.createLineBorder(Color.black));
			contentPane.add(label);

			JPanel performanceGraphPanel = new JPanel();
			FlowLayout flowLayout_1 = (FlowLayout) performanceGraphPanel.getLayout();
			flowLayout_1.setVgap(50);
			flowLayout_1.setHgap(50);

			// set white background
			performanceGraphPanel.setBackground(Color.white);
			
			contentPane.add(performanceGraphPanel);

			if (showResponseTimeGraph) {
				responseTimeGraph = new PerformanceMeasurementGraph ("Avg Response Time");
				performanceGraphPanel.add(responseTimeGraph.GetChartPanel(),"Response Time");
			}

			if (showResponseTimeGraphForMonitoredEntities) {
				responseTimeGraphForMonitoredEntities = new PerformanceMeasurementGraph ("Sel.Flows ARP");
				performanceGraphPanel.add(responseTimeGraphForMonitoredEntities.GetChartPanel(),"Sel.Flows ARP");
			}

			if (showInformationFreshnessGraph) {
				informationFreshnessGraph = new PerformanceMeasurementGraph ("Inf. Freshness");
				performanceGraphPanel.add(informationFreshnessGraph.GetChartPanel(),"Inf. Freshness");
			}

			if (showInformationFreshnessGraphForMonitoredEntities) {
				informationFreshnessGraphForMonitoredEntities = new PerformanceMeasurementGraph ("Sel.Flows IF");
				performanceGraphPanel.add(informationFreshnessGraphForMonitoredEntities.GetChartPanel(),"Sel.Flows IF");
			}

			if (showProcessingCostGraph) {
				processingCostGraph = new PerformanceMeasurementGraph ("CPU Load");
				performanceGraphPanel.add(processingCostGraph.GetChartPanel(),"CPU Load");
			}

			if (showMemoryStateGraph) {
				memoryStateGraph = new PerformanceMeasurementGraph ("Memory State (KB)");
				performanceGraphPanel.add(memoryStateGraph.GetChartPanel(),"State");
			}

			if (showFlowsStatistics) {
				totalFlowsLabel = new JLabel("");
				totalFlowsLabel.setFont(new Font("Arial", Font.BOLD, 20));

				totalPushPullFlowsLabel = new JLabel("");
				totalPushPullFlowsLabel.setFont(new Font("Arial", Font.BOLD, 20));

				totalDirectFlowsLabel = new JLabel("");
				totalDirectFlowsLabel.setFont(new Font("Arial", Font.BOLD, 20));

				totalPubSubFlowsLabel = new JLabel("");
				totalPubSubFlowsLabel.setFont(new Font("Arial", Font.BOLD, 20));

				performanceGraphPanel.add(totalFlowsLabel);
				performanceGraphPanel.add(totalPushPullFlowsLabel);
				performanceGraphPanel.add(totalDirectFlowsLabel);
				performanceGraphPanel.add(totalPubSubFlowsLabel);

				UpdateFlowsCountLabels (0, 0, 0, 0);

			}
			ikmsFrame.setVisible(true);   
		}
		// Start updating the performance measurement figures
		StartUpdatingPerformanceMeasurements ();

	}  

	private void UpdateFlowsCountLabels (double numberOfFlows, double numberOfPushPullFlows, double numberOfDirectFlows, double numberOfPubSubFlows) {
		totalFlowsLabel.setText("Active Flows: " + (int) numberOfFlows);
		totalPushPullFlowsLabel.setText("Push/Pull Flows: " + (int) numberOfPushPullFlows);
		totalDirectFlowsLabel.setText("Direct Flows: " + (int) numberOfDirectFlows);
		totalPubSubFlowsLabel.setText("Pub/Sub Flows: " + (int) numberOfPubSubFlows);
	}

	public void StartUpdatingPerformanceMeasurements () {
		final int timePeriod=1000;
		
		Runnable figuresUpdatingRun = new Runnable() {
			public void run() {
				while (figuresUpdatingThread!=null) {
					try{
						// update logfile, in case of textmode
						if (textMode)
							UpdatePerformanceMeasurementsLogFile ();
						else
							UpdatePerformanceMeasurementsFigures ();

						Thread.sleep((int) timePeriod);

						// increase time for active measurements only & textmode
						if (textMode==false||ikms.areMeasurementsActive()) {
							currentTime+=timePeriod/1000;
						}
					} catch (java.lang.InterruptedException ex)
					{
						System.out.println("Stopped!");
					}
				}
			}
		};

		figuresUpdatingThread = new Thread(figuresUpdatingRun);
		figuresUpdatingThread.start();
	}

	public void TerminateIKMSUI () {
		// stop figures updating thread
		System.out.println ("Terminating measurements thread.");
		if (figuresUpdatingThread!=null)
			figuresUpdatingThread = null;
	}

	@SuppressWarnings("static-access")
	public void UpdatePerformanceMeasurementsLogFile () {
		// for active measurements only 
		if (ikms.areMeasurementsActive()) {

			//System.out.println (currentTime+";"+responseTime+";"+freshness+";"+cpu+";"+storage);
			
			// get updated warmup and total measurement times (textmode)
			double warmup = ikms.getMeasurementsWarmupPeriod() / 1000;
			double totaltime = ikms.getTotalMeasurementsTime() / 1000;

			// measurement step
			double step = currentTime-warmup;
			
			// show required measurements only
			//System.out.println ("warmup:"+warmup+" currenttime:"+currentTime+" totaltime:"+totaltime);
			if (currentTime>=warmup&&currentTime<=totaltime) {
				responseTime = informationFlowConfigurationAndStatisticsOperation.GetAverageResponseTime();
				informationFreshness = informationFlowConfigurationAndStatisticsOperation.GetAverageFreshness();
				responseTimeForMonitoredEntities = informationFlowConfigurationAndStatisticsOperation.GetAverageResponseTimeForMonitoredEntities();
				informationFreshnessForMonitoredEntities = informationFlowConfigurationAndStatisticsOperation.GetAverageFreshnessForMonitoredEntities();			
				cpu = informationFlowConfigurationAndStatisticsOperation.GetIKMSSystemCPUUsed();
				// in case of failure in cpu reading, return the last valid value
				if (cpu==0.0) {
					cpu = lastcpu;
				} else {
					lastcpu = cpu;
				}
				storage = informationFlowConfigurationAndStatisticsOperation.GetStorageMemoryUsed() / 1000;
				if (storage==0.0) {
					storage = laststorage;
				} else {
					laststorage = storage;
				}
				numberOfFlows = informationFlowConfigurationAndStatisticsOperation.GetNumberOfFlows();
				numberOfPushPullFlows = informationFlowConfigurationAndStatisticsOperation.GetNumberOfPushPullFlows();
				numberOfDirectFlows = informationFlowConfigurationAndStatisticsOperation.GetNumberOfDirectFlows();
				numberOfPubSubFlows = informationFlowConfigurationAndStatisticsOperation.GetNumberOfPubSubFlows();
				
				// Retrieve energy efficiency related measurements from global controller
				informationFlowConfigurationAndStatisticsOperation.RetrieveLocalControllerInformation();
				
				logOutput.Log(step+"\t"+responseTime+"\t"+informationFreshness+"\t"+responseTimeForMonitoredEntities+"\t"+informationFreshnessForMonitoredEntities+"\t"+cpu+"\t"+storage+"\t"+SelectedOptimizationGoal.GetOptimizationGoal().getOptGoalName()+"\t"+numberOfFlows+"\t"+numberOfPushPullFlows+"\t"+numberOfDirectFlows+"\t"+numberOfPubSubFlows+"\t"+informationFlowConfigurationAndStatisticsOperation.GetMinEnergyValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetMaxEnergyValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetAverageEnergyValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetTotalEnergyValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetMinHostCPULoadValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetMaxHostCPULoadValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetAverageHostCPULoadValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetMinHostMemoryAllocationValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetMaxHostMemoryAllocationValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetAverageHostMemoryAllocationValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetMinHostNetworkUtilizationValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetMaxHostNetworkUtilizationValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetAverageHostNetworkUtilizationValue()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetMinHostIncomingThroughput()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetMaxHostIncomingThroughput()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetAverageHostIncomingThroughput()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetMinHostOutgoingThroughput()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetMaxHostOutgoingThroughput()+"\t"+informationFlowConfigurationAndStatisticsOperation.GetAverageHostOutgoingThroughput());
			}
		}
	}

	@SuppressWarnings("static-access")
	public void UpdatePerformanceMeasurementsFigures () {
		// updating graph figures
		if (showResponseTimeGraph) {
			responseTime = informationFlowConfigurationAndStatisticsOperation.GetAverageResponseTime();
			responseTimeGraph.AddData(currentTime, responseTime);
		}
		if (showResponseTimeGraphForMonitoredEntities) {
			responseTimeForMonitoredEntities = informationFlowConfigurationAndStatisticsOperation.GetAverageResponseTimeForMonitoredEntities();
			responseTimeGraphForMonitoredEntities.AddData(currentTime, responseTimeForMonitoredEntities);
		}
		if (showInformationFreshnessGraph) {
			informationFreshness = informationFlowConfigurationAndStatisticsOperation.GetAverageFreshness();
			informationFreshnessGraph.AddData(currentTime, informationFreshness);
		}
		if (showInformationFreshnessGraphForMonitoredEntities) {
			informationFreshnessForMonitoredEntities = informationFlowConfigurationAndStatisticsOperation.GetAverageFreshnessForMonitoredEntities();
			informationFreshnessGraphForMonitoredEntities.AddData(currentTime, informationFreshnessForMonitoredEntities);
		}
		if (showProcessingCostGraph) {
			cpu = informationFlowConfigurationAndStatisticsOperation.GetIKMSSystemCPUUsed();
			processingCostGraph.AddData(currentTime, cpu);
		}
		if (showMemoryStateGraph) {
			storage = informationFlowConfigurationAndStatisticsOperation.GetStorageMemoryUsed() / 1000;
			memoryStateGraph.AddData(currentTime, storage);
		}

		// updating flow counts
		if (showFlowsStatistics) {
			numberOfFlows = informationFlowConfigurationAndStatisticsOperation.GetNumberOfFlows();
			numberOfPushPullFlows = informationFlowConfigurationAndStatisticsOperation.GetNumberOfPushPullFlows();
			numberOfDirectFlows = informationFlowConfigurationAndStatisticsOperation.GetNumberOfDirectFlows();
			numberOfPubSubFlows = informationFlowConfigurationAndStatisticsOperation.GetNumberOfPubSubFlows();

			UpdateFlowsCountLabels (numberOfFlows, numberOfPushPullFlows, numberOfDirectFlows, numberOfPubSubFlows);
		}

	}
}
