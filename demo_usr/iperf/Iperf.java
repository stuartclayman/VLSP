package demo_usr.iperf;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;

import demo_usr.paths.ManagementListener;
import demo_usr.paths.ManagementPort;
import demo_usr.paths.Reconfigure;
import demo_usr.paths.ReconfigureHandler;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.net.DatagramSocket;
import usr.common.ProcessWrapper;

/**
 * An application for starting a network resource stressing application
 */
public class Iperf implements Application, Reconfigure {

	List<String> iperfArgs;

	Process child;

	ProcessWrapper wrapper;

	ExecutorService executer;

	// Management interface
	LinkedBlockingDeque<usr.net.Datagram> mgmtQueue;

	// ManagementPort
	ManagementPort mPort;
	Future<?> mPortFuture;
	int udpPort = 5000;

	// ManagementListener
	ManagementListener mListener;
	Future<?> mListenerFuture;

	int verbose = 2; // verbose level: 1 = normal 2=extra verboseness

	CountDownLatch latch = null;

    DatagramSocket socket = null;

	boolean running = false;

	/**
	 * Constructor for Iperf
	 */
	public Iperf() {
		iperfArgs = new ArrayList<String>();
	}

	/**
	 * Initialisation for Recv. Recv port
	 */
	@Override
	public ApplicationResponse init(String[] args) {
		if (args.length == 0) {
			return new ApplicationResponse(false, "Usage: iperf args");

		} else {
			// iperfArgs[0] = "iperf"
			iperfArgs.add("iperf");

			// copy args
			for (String arg : args) {
				iperfArgs.add(arg);
			}

			return new ApplicationResponse(true, "");

		}
	}

	/** Start application with argument */
	@Override
	public ApplicationResponse start() {

		// Management interface
		mgmtQueue = new LinkedBlockingDeque<usr.net.Datagram>();
		int mgmtPort = (udpPort + 20000) % 32768;

		try {
			// allocate ManagementPort
			mPort = new ManagementPort(mgmtPort, mgmtQueue, verbose);
		} catch (Exception e) {
			return startError("Cannot open reader socket " + mgmtPort + ": "
					+ e.getMessage());
		}

		try {
			// allocate ManagementListener
			mListener = new ManagementListener(new ReconfigureHandler(this),
					mgmtQueue, verbose);
		} catch (Exception e) {
			return startError("ManagementListener error " + ": "
					+ e.getMessage());
		}

		// process builder that runs the iperf command
		/*try {
			// start the ProcessBuilder
			ProcessBuilder pb = new ProcessBuilder(iperfArgs);

			child = pb.start();

			wrapper = new ProcessWrapper(child, "iperf");

		} catch (Exception e) {
			Logger.getLogger("log").logln(USR.ERROR,
					"Cannot start process " + e.getMessage());
			return new ApplicationResponse(false, "Cannot start process "
					+ e.getMessage());
		}*/

		running = true;

		return new ApplicationResponse(true, "");
	}

	/** Implement graceful shut down */
	@Override
	public ApplicationResponse stop() {
		try {
			running = false;

			// process builder command, enable again
			//wrapper.stop();

			mPortFuture.cancel(true);
			mListenerFuture.cancel(true);

			Logger.getLogger("log").logln(USR.STDOUT, "stop() Stress stop");

			return new ApplicationResponse(true, "");
		} catch (Exception e) {
			return new ApplicationResponse(false, "Process waitFor Exception: "
					+ e.getMessage());
		}
	}

	/**
	 * error
	 */
	private ApplicationResponse startError(String msg) {
		Logger.getLogger("log").logln(USR.ERROR, msg);
		return new ApplicationResponse(false, msg);
	}

	private void PassIperfConfiguration() {

		Datagram datagram = null;

		// embed usrAddr into JSONObject
		JSONObject jsobj = new JSONObject();

		try {
			jsobj.put("address", "lefteris");
		} catch (JSONException jse) {
			Logger.getLogger("log").logln(USR.ERROR, "JSONException " + jse);
			return;
		}

		String jsString = jsobj.toString();

		if (verbose > 0) {
			Logger.getLogger("log")
					.logln(USR.ERROR, "JSONObject = " + jsString);
		}

		byte[] buffer = jsString.getBytes();

		datagram = DatagramFactory.newDatagram(buffer);

		// now send it

		try {
			socket.send(datagram);

		} catch (Exception e) {
			if (socket.isClosed()) {
				Logger.getLogger("log").logln(USR.ERROR,
						"Cant send: socket closed with " + jsString);
			} else {
				Logger.getLogger("log").logln(USR.ERROR,
						"Cant send: " + e + " with " + jsString);
			}
		}

		// cannot be null, if we get here
		// if (socket != null) {
		socket.close();

		Logger.getLogger("log").logln(USR.ERROR, "Iperf close socket");
		// }

		Logger.getLogger("log").logln(USR.ERROR, "Iperf: end of run()");

	}

	/** Run the application */
	@Override
	public void run() {

		/*
		 * Start the supporting threads that actually reads from the UDP socket
		 * and forwards to the USR socket
		 */
		executer = Executors.newFixedThreadPool(3);

		try {
			mPortFuture = executer.submit((Callable<?>) mPort);

			mListenerFuture = executer.submit((Callable<?>) mListener);

		} catch (Exception e) {
			Logger.getLogger("log").log(USR.ERROR, e.getMessage());
			e.printStackTrace();
		}

		// wait for the latch to drop before continuing
		try {
			latch.await();
		} catch (InterruptedException ie) {
		}

		mPort.await();
		mListener.await();

		Logger.getLogger("log").logln(USR.ERROR, "Iperf: end of run()");

		// code for iperf command execution
		/*try {
			child.waitFor();

		} catch (InterruptedException ie) {
			System.out.println("Process waitFor interrupted: "
					+ ie.getMessage());
		}

		System.out.println("run() Process waitFor completed");*/
	}

	@Override
	public Object process(JSONObject jsobj) {
		// TODO Auto-generated method stub
		System.out.println ("IPERF APP RECEIVED JSOBJ:"+jsobj.toString());
		return null;
	}

}
