package demo_usr.ikms;


import usr.applications.Application;
import usr.applications.ApplicationResponse;

public class LocalInformationFlowsExperiment implements Application {
	int flowsNumber = 50;

	GenericSourceMA[] sources;
	GenericSinkMA[] sinks;


	/**
	 * Initialize with some args
	 */
	public ApplicationResponse init(String[] args) {
		flowsNumber = 50;

		sources = new GenericSourceMA[flowsNumber];
		sinks = new GenericSinkMA[flowsNumber];


		return new ApplicationResponse(true, "");

	}

	/**
	 * Start an application.
	 * This is called before run().
	 */
	public ApplicationResponse start() {

		// initialize and run sources + sinks
		for (int i=0;i<flowsNumber;i++) {
			sources[i].start();
			sinks[i].start();
		}


		return new ApplicationResponse(true, "");
	}


	/**
	 * Stop an application.
	 * This is called to implement graceful shut down
	 * and cause run() to end.
	 */
	public ApplicationResponse stop() {

		return new ApplicationResponse(true, "");

	}


	/**
	 * Main loop
	 */
	public void run() {
		// initialize and run sources + sinks
		for (int i=0;i<flowsNumber;i++) {
			sources[i].run();
			sinks[i].run();
		}

	}

}
