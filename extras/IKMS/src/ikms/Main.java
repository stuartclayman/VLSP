package ikms;

import ikms.ui.IKMSVisualisation;
import ikms.util.LoggerFrame;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {

	// IKMS configuration parameters
	static int port;
	static String optimizationGoal;
	static String dbHost;
	static String dbPassword;
	static String gcHost;
	static String gcPort;
	static boolean showResponseTimeGraph;
	static boolean showInformationFreshnessGraph;
	static boolean showResponseTimeGraphForMonitoredEntities;
	static boolean showInformationFreshnessGraphForMonitoredEntities;
	static boolean showProcessingCostGraph;
	static boolean showMemoryStateGraph;
	static boolean showFlowsStatistics;
	static int windowX;
	static int windowY;
	static int windowWidth;
	static int windowHeight;
	static boolean textMode;
	
	static IKMS ikms = null;
	
	private static IKMSVisualisation visualise=null;
	
	public static void main(String[] args) {
		// Get initial IKMS configuration
		Properties prop = new Properties();

		try {
			//load a properties file
			prop.load(new FileInputStream("scripts/ikms.cfg"));

			// gets the properties value
			port = GetIntegerProperty (prop, "port", 9900);
			optimizationGoal = GetStringProperty (prop, "optimizationGoal", "{\"optGoalId\":0, \"optGoalName\":\"Pull from Entity\", \"optGoalParameters\":\"\", \"optGoalLevelofEnforcement\":\"Medium\"}");
			dbHost = GetStringProperty (prop, "dbhost", "localhost");
			dbPassword = GetStringProperty (prop, "dbpassword", "");
			showResponseTimeGraph = GetBooleanProperty (prop, "showResponseTimeGraph", false);
			showInformationFreshnessGraph = GetBooleanProperty (prop, "showInformationFreshnessGraph", false);
			showResponseTimeGraphForMonitoredEntities = GetBooleanProperty (prop, "showResponseTimeGraphForMonitoredEntities", false);
			showInformationFreshnessGraphForMonitoredEntities = GetBooleanProperty (prop, "showInformationFreshnessGraphForMonitoredEntities", false);
			showProcessingCostGraph = GetBooleanProperty (prop, "showProcessingCostGraph", true);
			showMemoryStateGraph = GetBooleanProperty (prop, "showMemoryStateGraph", true);
			showFlowsStatistics = GetBooleanProperty (prop, "showFlowsStatistics", true);
			windowX = GetIntegerProperty (prop, "windowX", 100);
			windowY = GetIntegerProperty (prop, "windowY", 100);
			windowWidth = GetIntegerProperty (prop, "windowWidth", 1105);
			windowHeight = GetIntegerProperty (prop, "windowHeight", 845);
			textMode = GetBooleanProperty (prop, "textMode", false);
			gcHost = GetStringProperty (prop, "gchost", "localhost");
			gcPort = GetStringProperty (prop, "gcport", "8888");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		// disable workflow visualization in case of textmode
		if (textMode)
			LoggerFrame.setTextMode();

		// Create a new IKMS object
		ikms = new IKMS ();
		
		if (! ikms.config()) return;

		if (! ikms.init(port, optimizationGoal, dbHost, dbPassword, gcHost, gcPort)) return;

		if (! ikms.start()) return;

		// do not show graphical user interface in textmode
		visualise = new IKMSVisualisation(ikms, showResponseTimeGraph, showInformationFreshnessGraph, showResponseTimeGraphForMonitoredEntities, showInformationFreshnessGraphForMonitoredEntities, showProcessingCostGraph, showMemoryStateGraph, showFlowsStatistics, windowX, windowY, windowWidth, windowHeight, textMode);
		
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
		    @Override
		    public void run()
		    {
		    		System.out.println ("Shutting down IKMS...");
		    		ShutDownIKMS();
		    }
		});
		
	}
	
	private static final void ShutDownIKMS () {
		visualise.TerminateIKMSUI();
		ikms.stop();
	}
	
	private static boolean GetBooleanProperty (Properties prop, String parameter, boolean defaultValue) {
		if (prop.getProperty(parameter)==null) {
			return defaultValue;
		} else {
			if (prop.getProperty(parameter).equals("true"))
				return true;
			else
				if (prop.getProperty(parameter).equals("false"))
					return false;
				else
					return defaultValue;
		}
	}

	private static int GetIntegerProperty (Properties prop, String parameter, int defaultValue) {
		Integer output = defaultValue;

		if (prop.getProperty(parameter)==null) {
			return output;
		} else {
			try {
				output = Integer.valueOf(prop.getProperty(parameter));
				return output;
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
	}

	private static String GetStringProperty (Properties prop, String parameter, String defaultValue) {
		String output = defaultValue;

		if (prop.getProperty(parameter)==null) {
			return output;
		} else {
			output = prop.getProperty(parameter);
			return output;
		}
	}

}
