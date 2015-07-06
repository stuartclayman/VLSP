package ikms.util;

import java.awt.Color;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;

import us.monoid.web.Resty;


/**
 * A class to interact with a LoggerFrame visualizer
 * It implements the equivalnt of <br/>
 * http://localhost:8090/log/?sender=SSSSSSS&receiver=RRRRR&message=MMMMM <br/>
 * by doing LoggerFrame.log("SSSSSSS", "RRRRR", "MMMMM")
 * <p>
 * The LoggerFrame class can log to multiple destinations.
 * There is a "default" destination on localhost:8090, however
 * this can be changed using LoggerFrame.register("default", host, port)
 * or a new destination can be added using: LoggerFrame.register("logframe2", host2, port3).
 */

public class LoggerFrame {
	static HashMap<String, String> registry = new HashMap<String, String>();

	static HashMap<Integer, String> activeEntities = new HashMap<Integer, String>();

	// colours
	static HashMap<String, Color> colours = new HashMap<String, Color>();

	// IKMS Name
	public static String IKMSName="Information & Knowledge Management System";

	// Information Exchange Interface Name
	public static String InformationExchangeName="Information Exchange Interface";

	// Information Management Interface Name
	public static String InformationManagementName="Information Management Interface";

	// ICD Function Name
	public static String ICDFunctionName="ICD Function";

	// ISI Function Name
	public static String ISIName="ISI Function";

	// ISI (Entity Registration Storage) Name
	//public static String ISIEntityStorageName="ISI (Entity Registration Storage)";

	// ISI (Info/Know Storage) Name
	//public static String ISIInfoStorageName="ISI (Info/Know Storage)";

	// ISI (Indexing Storage) Name
	//public static String ISIIndexingStorageName="ISI (Indexing Storage)";

	// IFEO Name
	public static String IFEOName="IFEO Function";

	// IQC Name
	public static String IQCName="Information Quality Controller";

	// IFC Name
	public static String IFCName="Information Flow Configurations";

	/*
	 * Allocation time static startup code
	 */
	static {
		// setup colours - map from String to java.awt.Color
		colours.put("0", Color.BLACK);
		colours.put("1", Color.BLUE);
		colours.put("2", Color.RED);
		colours.put("3", Color.GREEN);
		colours.put("4", Color.DARK_GRAY);
		colours.put("5", Color.ORANGE);
		colours.put("6", Color.CYAN);
		colours.put("7", Color.MAGENTA);
		colours.put("8", Color.PINK);
		colours.put("9", Color.YELLOW);

		// setup logging destinations
		register("default", "localhost", "8090");
		register("entities2ikms", "localhost", "8090");
		register("ikmsfunctions", "localhost", "8091");
		register("ifeofunction", "localhost", "8092");
		register("ipkpfunction", "localhost", "8093");
	}

	private static boolean textMode = false;

	public static void setTextMode () {
		textMode = true;
	}

	/**
	 * Register a new LoggerFrame destination at host:port
	 */
	public static boolean register(String label, String host, String port) {
		registry.put(label, "http://" + host + ":" + port); 
		return true;
	}

	public static void registerActiveEntity (int entityid, String entityname) {
		// register if it is not there
		if (activeEntities.get(entityid)==null)
			activeEntities.put(entityid, entityname);
	}

	public static void unregisterActiveEntity (int entityid) {
		// register if it is not there
		if (activeEntities.get(entityid)==null)
			activeEntities.remove(entityid);
	}

	public static String getActiveEntityName (int entityid) {
		return activeEntities.get(entityid);
	}

	public static boolean workflowvisualisationlog (int entityid, String sender, String receiver, String msg, String loggerframe) {
		// do not visualize in text mode
		if (!textMode) {
			if (sender.equals("")) {
				//workflow from an Entity
				log(loggerframe, getActiveEntityName(entityid), receiver, msg, GetColourFromentityid (entityid));

			} else if (receiver.equals("")) {
				//workflow to an Entity
				log(loggerframe, sender, getActiveEntityName(entityid), msg, GetColourFromentityid (entityid));

			} else {
				//workflow between ikms functions
				log(loggerframe, sender, receiver, msg, GetColourFromentityid (entityid));
			}
		}
		return true;
	}

	private static String GetColourFromentityid (int entityid) {
		return String.valueOf(entityid % 9);
	}

	/**
	 * Log a message to the "default" LoggerFrame
	 */
	public static boolean log(String sender, String receiver, String msg) {
		return log("default", sender, receiver, msg, "black");
	}

	/** 
	 * Log a message to the "default" LoggerFrame
	 */
	public static boolean log(String sender, String receiver, String msg, String colour) {
		return log("default", sender, receiver, msg, colour);
	}


	/** 
	 * Log a message to a LoggerFrame
	 */
	public static boolean log(String label, String sender, String receiver, String msg, String colour) {
		// We  have code here that collects information from Entities
		// using a REST interface on the Entity
		String baseURL = registry.get(label);

		if (baseURL == null) {
			throw new Error("LoggerFrame: destination label " + label + " not defined");

		} else {
			try {
				// lookup Color 
				Color color = colours.get(colour);

				if (color == null) {
					color = Color.BLACK;
				}

				String colorString = String.format("0x%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());

				//System.err.println("Color = " + colorString);

				String locationURL = baseURL + "/log/";

				String callURL = locationURL + "?sender=" + URLEncoder.encode(sender, "UTF-8") + "&receiver=" + URLEncoder.encode(receiver, "UTF-8") + "&message=" + URLEncoder.encode(msg, "UTF-8") + "&color=" + URLEncoder.encode(colorString, "UTF-8");

				// Make a Resty connection
				Resty rest = new Resty();

				// Call the relevant URL
				rest.json(callURL);

				return true;


			}  catch (IOException ioe) {
				//ioe.printStackTrace();
				System.err.println(ANSI.YELLOW + "LoggerFrame " + label + " on " + baseURL + " not running" + ANSI.RESET_COLOUR);
				//} catch (JSONException je) {
				//     je.printStackTrace();
			}

			return false;
		}
	}
}
