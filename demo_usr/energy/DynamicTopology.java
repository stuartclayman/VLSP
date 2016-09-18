package demo_usr.energy;

import usr.events.*;
import usr.engine.*;
import usr.globalcontroller.RemoteEventDelegate;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class DynamicTopology extends RemoteEventDelegate implements EventDelegate {
	int numberOfHosts=3;
	int totalTime=86400;

	/**
	 * Get the maximum lag this delegate will allow.
	 */
	@Override
	public long getMaximumLag() {
		return 600000;   // 10 mins
	}

	/**
	 * Construct a DynamicTopology.
	 */
	public DynamicTopology() throws UnknownHostException, IOException {
		super();
	}

	/**
	 * Construct a DynamicTopology.
	 */
	public DynamicTopology(int numberOfHosts, int totalTime) throws UnknownHostException, IOException {
		super();

		this.numberOfHosts = numberOfHosts;
		this.totalTime = totalTime;
	}

	/**
	 * Construct a DynamicTopology.
	 */
	public DynamicTopology(String addr, int port)  throws UnknownHostException, IOException {
		super(addr, port);
	}

	/**
	 * Construct a DynamicTopology.
	 */
	public DynamicTopology(InetAddress addr, int port)  throws UnknownHostException, IOException {
		super(addr, port);
	}


	/**
	 * Start an EventEngine
	 */
	public EventEngine startEventEngine(EventDelegate ed) {
		try {
			// time to run:  86400 seconds = 1 day
			// startup script

			//return new usr.engine.ScriptEngine(86400, "scripts/AppScript2Ca");

			return new usr.engine.IKMSEventEngine(totalTime, "scripts/energy"+numberOfHosts+".xml");
		} catch (EventEngineException eee) {
			throw new Error("Cant start event engine" + eee.getMessage());
		}
	}


	/**
	 * Get the name of the delegate
	 */
	public String getName() {
		return "RemoteExecution";
	}

	public static void main(String[] args) {
		try {
			DynamicTopology dynamicTopology = new DynamicTopology();

			dynamicTopology.init();

			dynamicTopology.start();

			dynamicTopology.run();

			dynamicTopology.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

