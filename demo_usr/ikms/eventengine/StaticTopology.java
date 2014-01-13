package demo_usr.ikms.eventengine;

import usr.events.*;
import usr.engine.*;
import usr.globalcontroller.RemoteEventDelegate;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class StaticTopology extends RemoteEventDelegate implements EventDelegate {
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
	 * Construct a StaticTopology.
	 */
	public StaticTopology() throws UnknownHostException, IOException {
		super();
	}

	/**
	 * Construct a StaticTopology.
	 */
	public StaticTopology(int numberOfHosts, int totalTime) throws UnknownHostException, IOException {
		super();

		this.numberOfHosts = numberOfHosts;
		this.totalTime = totalTime;
	}

	/**
	 * Construct a StaticTopology.
	 */
	public StaticTopology(String addr, int port)  throws UnknownHostException, IOException {
		super(addr, port);
	}

	/**
	 * Construct a StaticTopology.
	 */
	public StaticTopology(InetAddress addr, int port)  throws UnknownHostException, IOException {
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

			return new usr.engine.IKMSEventEngine(totalTime, "scripts/ikms"+numberOfHosts+".xml");
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
			StaticTopology staticTopology = new StaticTopology();

			staticTopology.init();

			staticTopology.start();

			staticTopology.run();

			staticTopology.stop();
		} catch (Exception e) {
		}
	}

}

