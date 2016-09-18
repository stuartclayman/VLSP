/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import rgc.probdistributions.ProbDistribution;
import rgc.probdistributions.ProbException;
import rgc.xmlparse.ReadXMLUtils;
import rgc.xmlparse.XMLNoTagException;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.events.EndSimulationEvent;
import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.events.ExecutableEvent;
import usr.events.StartSimulationEvent;
import usr.events.vim.EndRouterEvent;
import usr.events.vim.StartLinkEvent;
import usr.events.vim.StartRouter;
import usr.events.vim.StartRouterEvent;
import usr.logging.Logger;
import usr.logging.USR;
import usr.vim.VimFunctions;

/**
 * This engine uses probability distributions to add events into the event
 * library
 */
public class IKMSEventEngine implements EventEngine {
	int timeToEnd_; // Time to end of simulation (ms)
	ProbDistribution nodeCreateDist_ = null; // Distribution for creating nodes
	ProbDistribution nodeDeathDist_ = null; // Distribution of node lifetimes
	ProbDistribution linkCreateDist_ = null; // Distribution for number of links
												// created
	ProbDistribution linkDeathDist_ = null; // Distribution for link lifetimes
	ProbDistribution nodeCPULoadDist_ = null; // Distribution of cpu load
												// (stress parameter c)
	ProbDistribution nodeMemoryLoadDist_ = null; // Distribution of
													// memory-related load
													// (stress parameter m)
	ProbDistribution nodeNetworkLoadDist_ = null; // Distribution of
												// network load (value in
												// bits/sec)
												// (iperf parameter -b)

	private boolean preferentialAttachment_ = false; // If true links are chosen
														// using P.A.

	private boolean staticTopology_ = false; // whether the topology is static
												// or not

	int initialNumberOfRouters_ = 1; // Initial number of routers

	List<Integer> routerIDs = null;

	/** Constructor from Parameter string */
	public IKMSEventEngine(int time, String parms) throws EventEngineException {
		timeToEnd_ = time * 1000;
		parseXMLFile(parms);

		routerIDs = new ArrayList<Integer>();
	}

	/** Start up and shut down events */
	@Override
	public void startStopEvents(EventScheduler s, EventDelegate obj) {
		// simulation start
		StartSimulationEvent e0 = new StartSimulationEvent(0);

		s.addEvent(e0);

		// simulation end
		EndSimulationEvent e = new EndSimulationEvent(timeToEnd_);
		s.addEvent(e);
	}

	/** Initial events to add to schedule */
	@Override
	public void initialEvents(EventScheduler s, EventDelegate obj) {
		// Start initial router
		long time;

		Event[] e = new Event[initialNumberOfRouters_];
		// create a number of initial routers (based on the initial number of
		// nodes parameter)
		try {
			for (int i = 0; i < initialNumberOfRouters_; i++) {

				// Schedule new node
				try {
					time = (long) (nodeCreateDist_.getVariate() * 1000);
				} catch (ProbException x) {
					Logger.getLogger("log")
							.logln(USR.ERROR,
									leadin()
											+ " Error generating trafficArriveDist variate");
					time = 0;
				}

				// calculate stress parameters
				double loadCPU = 0;
				double loadMemory = 0;
				double loadNetwork = 0;

				if (nodeCPULoadDist_ != null) {
					try {
						loadCPU = (double) (nodeCPULoadDist_.getVariate());
					} catch (ProbException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						loadCPU = 0;
					}
				}

				if (nodeMemoryLoadDist_ != null) {
					try {
						loadMemory = (double) (nodeMemoryLoadDist_.getVariate());
					} catch (ProbException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						loadMemory = 0;
					}
				}

				if (nodeNetworkLoadDist_ != null) {
					try {
						loadNetwork = (double) (nodeNetworkLoadDist_.getVariate());
					} catch (ProbException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						loadNetwork = 0;
					}
				}
				String stressParameters = null;
				String iperfParameters = null;

				if (loadCPU == 0 && loadMemory > 0) {
					// deploy stress with parameter m
					stressParameters = "-m " + loadMemory;
				}

				if (loadCPU > 0 && loadMemory == 0) {
					// deploy stress with parameter c
					stressParameters = "-c " + loadCPU;
				}

				if (loadCPU > 0 && loadMemory > 0) {
					// deploy stress with parameter c & m
					stressParameters = "-c " + loadCPU + " -m " + loadMemory;
				}

				// in case network load requested
				if (loadNetwork > 0) {
					iperfParameters = "-b " + loadNetwork;
				}

				// Logger.getLogger("log").logln(USR.ERROR,
				// "Time to next router "+time);
				Logger.getLogger("log").logln(
						USR.STDOUT,
						"Starting router number:" + i + " of routers:"
								+ initialNumberOfRouters_);

				if (stressParameters == null && iperfParameters == null) {
					// start router without stress or iperf
					e[i] = new StartRouterEvent(time, this);
				} else {
					if (iperfParameters == null) {
						// pass stress parameters only
						Logger.getLogger("log").logln(
								USR.STDOUT,
								leadin() + "Stress parameters:"
										+ stressParameters);

						e[i] = new StartRouterEvent(time, stressParameters,
								this);
					} else {
						if (stressParameters == null) {
							// pass iperf parameters only
							Logger.getLogger("log").logln(
									USR.STDOUT,
									leadin() + "Iperf parameters:"
											+ iperfParameters);

							e[i] = new StartRouterEvent(time, iperfParameters,
									this);
						} else {
							// pass stress and iperf parameters in one string,
							// separated by ; character
							Logger.getLogger("log").logln(
									USR.STDOUT,
									leadin() + "Stress parameters:"
											+ stressParameters
											+ " Iperf parameters:"
											+ iperfParameters);

							e[i] = new StartRouterEvent(time, stressParameters
									+ ";" + iperfParameters, this); // pass parameters for both stress & iperf
						}
					}
				}
				s.addEvent(e[i]);

			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/** Add or remove events following a simulation event */
	@Override
	public void preceedEvent(Event e, EventScheduler s, EventDelegate obj) {
		Logger.getLogger("log").logln(USR.STDOUT,
				leadin() + "preceedEvent " + e);

		if (e instanceof ExecutableEvent && e instanceof StartRouter) {
			ExecutableEvent ee = (ExecutableEvent) e;
			preceedRouter(ee, s, (VimFunctions) ee.getContextObject());
			return;
		}

	}

	/** Add or remove events following a simulation event */
	@Override
	public void followEvent(Event e, EventScheduler s, JSONObject response,
			EventDelegate obj) {
		Logger.getLogger("log")
				.logln(USR.STDOUT, leadin() + "followEvent " + e);

		if (e instanceof ExecutableEvent && e instanceof StartRouter) {
			ExecutableEvent ee = (ExecutableEvent) e;
			followRouter(ee, s, response, (VimFunctions) ee.getContextObject());
			return;
		}
	}

	@Override
	public void finalEvents(EventDelegate obj) {
		for (Integer routerID : routerIDs) {
			try {
				EndRouterEvent e2 = new EndRouterEvent(0, this, routerID);

				obj.executeEvent(e2);
			} catch (Exception ex) {
			}
		}
	}

	private void preceedRouter(Event e, EventScheduler s, VimFunctions v) {
		Logger.getLogger("log").logln(USR.STDOUT,
				leadin() + "followRouter " + e);

	}

	private void followRouter(Event e, EventScheduler s, JSONObject response,
			VimFunctions v) {
		Logger.getLogger("log").logln(USR.STDOUT,
				leadin() + "followRouter " + e.getParameters());

		int routerId;
		long now = e.getTime();
		long time;

		try {
			routerId = response.getInt("routerID"); // WAS g.getMaxRouterId();
			// keep track of routerIds for later on when the engine ends
			routerIDs.add(routerId);

			// if there are parameters set, execute stress
			String parameters = e.getParameters();
			if (parameters != null) {
				// in case parameters string variable includes the ; character,
				// execute both stress & iperf
				// otherwise check the -b value, if it exists execute iperf only
				// if not execute stress only
				String stressParameters = null;
				String iperfParameters = null;

				if (parameters.contains(";")) {
					// run both iperf and stress
					stressParameters=parameters.split(";")[0];
					iperfParameters=parameters.split(";")[1];
				} else {
					if (parameters.contains("-b")) {
						// run iperf only
						iperfParameters=parameters;
					} else {
						// run stress only
						stressParameters=parameters;
					}
				}

				// execute iperf if needed
				if (iperfParameters != null) {
					Logger.getLogger("log")
							.logln(USR.STDOUT,
									leadin()
											+ "followRouter starting iperf application with parameters"
											+ iperfParameters);
					v.createApp(routerId, "demo_usr.iperf.Iperf", iperfParameters);
				}

				// execute stress if needed
				if (stressParameters != null) {
					Logger.getLogger("log")
							.logln(USR.STDOUT,
									leadin()
											+ "followRouter starting stress application with parameters"
											+ stressParameters);
					v.createApp(routerId, "demo_usr.stress.Stress", stressParameters);
				}

			}
		} catch (JSONException jse) {
			routerId = -1;
			jse.printStackTrace();
		}

		// Schedule new node, if topology is not static
		if (!staticTopology_) {
			try {
				time = (long) (nodeCreateDist_.getVariate() * 1000);
			} catch (ProbException x) {
				Logger.getLogger("log")
						.logln(USR.ERROR,
								leadin()
										+ " Error generating trafficArriveDist variate");
				time = 0;
			}

			// Logger.getLogger("log").logln(USR.ERROR,
			// "Time to next router "+time);
			StartRouterEvent e1 = new StartRouterEvent(now + time, this);
			s.addEvent(e1);
		}

		System.err.println("Router list" + getRouterList(v) + " Router ID "
				+ routerId);

		if (getRouterList(v).indexOf(routerId) == -1) {
			System.err.println("Router did not start -- adding no links");
			return;
		}

		// Schedule node death if this will happen, in case of a non static
		// topology
		if (!staticTopology_) {
			if (nodeDeathDist_ != null) {
				try {
					time = (long) (nodeDeathDist_.getVariate() * 1000);
				} catch (ProbException x) {
					Logger.getLogger("log")
							.logln(USR.ERROR,
									leadin()
											+ " Error generating nodeDeathDist variate");
					time = 0;
				}

				EndRouterEvent e2 = new EndRouterEvent(now + time, this,
						new Integer(routerId));
				s.addEvent(e2);
			}
		}
		// Schedule links
		int noLinks = 1;
		try {
			noLinks = linkCreateDist_.getIntVariate();
		} catch (ProbException x) {
			Logger.getLogger("log").logln(USR.ERROR,
					leadin() + " Error generating linkCreateDist variate");

		}

		ArrayList<Integer> nodes = getRouterList(v);
		nodes.remove(nodes.indexOf(routerId));
		ArrayList<Integer> outlinks = getOutLinks(routerId, v);

		for (Integer l : outlinks) {
			nodes.remove(nodes.indexOf(l));
		}

		// Logger.getLogger("log").logln(USR.ERROR,
		// "Trying to pick "+noLinks+" links");

		for (int i = 0; i < noLinks; i++) {
			if (nodes.size() <= 0) {
				break;
			}

			if (preferentialAttachment_) { // Choose a node using pref. attach.
				int totLinks = 0;

				for (int l : nodes) {
					totLinks += getOutLinksCount(l, v);
				}

				int index = (int) Math.floor(Math.random() * totLinks);

				for (int j = 0; j < nodes.size(); j++) {
					int l = nodes.get(j);
					index -= getOutLinksCount(l, v);

					if (index < 0 || j == nodes.size() - 1) {
						nodes.remove(j);
						StartLinkEvent e3 = new StartLinkEvent(now, this, l,
								routerId);
						s.addEvent(e3);
						break;
					}
				}
			} else {
				// Logger.getLogger("log").logln(USR.ERROR,
				// "Choice set "+nodes);
				int index = (int) Math.floor(Math.random() * nodes.size());
				int newLink = nodes.get(index);
				// Logger.getLogger("log").logln(USR.ERROR, "Picked "+newLink);
				nodes.remove(index);

				StartLinkEvent e4 = new StartLinkEvent(now, this, newLink,
						routerId);
				s.addEvent(e4);

			}
		}
	}

	/** Parse the XML to get probability distribution information */
	private void parseXMLFile(String fName) throws EventEngineException {
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new File(fName));

			// normalize text representation
			doc.getDocumentElement().normalize();
			String basenode = doc.getDocumentElement().getNodeName();

			if (!basenode.equals("IKMSEngine")) {
				throw new SAXException("Base tag should be IKMSEngine");
			}

			NodeList nbd = doc.getElementsByTagName("NodeBirthDist");
			nodeCreateDist_ = ProbDistribution.parseProbDist(nbd,
					"NodeBirthDist");

			if (nodeCreateDist_ == null) {
				throw new SAXException("Must specific NodeBirthDist");
			}
			NodeList lcd = doc.getElementsByTagName("LinkCreateDist");
			linkCreateDist_ = ProbDistribution.parseProbDist(lcd,
					"LinkCreateDist");

			if (linkCreateDist_ == null) {
				throw new SAXException("Must specific LinkCreateDist");
			}

			NodeList ncld = doc.getElementsByTagName("NodeCPULoadDist");
			nodeCPULoadDist_ = ProbDistribution.parseProbDist(ncld,
					"NodeCPULoadDist");

			NodeList nmld = doc.getElementsByTagName("NodeMemoryLoadDist");
			nodeMemoryLoadDist_ = ProbDistribution.parseProbDist(nmld,
					"NodeMemoryLoadDist");

			NodeList nnld = doc.getElementsByTagName("NodeNetworkLoadDist");
			nodeNetworkLoadDist_ = ProbDistribution.parseProbDist(nnld,
					"NodeNetworkLoadDist");
			
			NodeList ndd = doc.getElementsByTagName("NodeDeathDist");
			nodeDeathDist_ = ProbDistribution.parseProbDist(ndd,
					"NodeDeathDist");
			NodeList ldd = doc.getElementsByTagName("LinkDeathDist");

			linkDeathDist_ = ProbDistribution.parseProbDist(ldd,
					"LinkDeathDist");
			try {

				NodeList misc = doc.getElementsByTagName("Parameters");

				if (misc.getLength() > 1) {
					throw new SAXException(
							"Only one GlobalController tags allowed.");
				}

				if (misc.getLength() == 1) {
					Node miscnode = misc.item(0);
					preferentialAttachment_ = ReadXMLUtils.parseSingleBool(
							miscnode, "PreferentialAttachment", "Parameters",
							true);

					initialNumberOfRouters_ = ReadXMLUtils.parseSingleInt(
							miscnode, "InitialNumberOfNodes", "Parameters",
							true);

					staticTopology_ = ReadXMLUtils.parseSingleBool(miscnode,
							"StaticTopology", "Parameters", true);

					ReadXMLUtils.removeNode(miscnode, "PreferentialAttachment",
							"Parameters");
					ReadXMLUtils.removeNode(miscnode, "InitialNumberOfNodes",
							"Parameters");
					ReadXMLUtils.removeNode(miscnode, "StaticTopology",
							"Parameters");
				}
			} catch (SAXException e) {
				throw e;
			} catch (XMLNoTagException e) {

			}

		} catch (java.io.FileNotFoundException e) {
			throw new EventEngineException(
					"Parsing IKMSEventEngine: Cannot find file " + fName);
		} catch (SAXParseException err) {
			throw new EventEngineException("Parsing IKMSEventEngine: error"
					+ ", line " + err.getLineNumber() + ", uri "
					+ err.getSystemId());

		} catch (SAXException e) {
			throw new EventEngineException(
					"Parsing IKMSEventEngine: Exception in SAX XML parser"
							+ e.getMessage());

		} catch (Throwable t) {
			throw new EventEngineException("Parsing IKMSEventEngine: "
					+ t.getMessage());
		}
	}

	/**
	 * Get a router list
	 */
	private ArrayList<Integer> getRouterList(VimFunctions v) {
		try {
			JSONObject jsobj = v.listRouters();
			JSONArray list = jsobj.getJSONArray("list");

			ArrayList<Integer> result = new ArrayList<Integer>();

			for (int i = 0; i < list.length(); i++) {
				result.add(list.getInt(i));
			}

			return result;
		} catch (JSONException jse) {
			return null;
		}
	}

	/**
	 * Get active applications
	 */
	private ArrayList<Integer> getAppsList(VimFunctions v, int routerId) {
		try {
			JSONObject jsobj = v.listApps(routerId);
			JSONArray list = jsobj.getJSONArray("list");

			ArrayList<Integer> result = new ArrayList<Integer>();

			for (int i = 0; i < list.length(); i++) {
				result.add(list.getInt(i));
			}

			return result;
		} catch (JSONException jse) {
			return null;
		}
	}

	/**
	 * Get router outlinks
	 */
	private ArrayList<Integer> getOutLinks(int routerID, VimFunctions v) {
		try {
			JSONObject jsobj = v.getRouterInfo(routerID);
			JSONArray list = jsobj.getJSONArray("links");

			ArrayList<Integer> result = new ArrayList<Integer>();

			for (int i = 0; i < list.length(); i++) {
				result.add(list.getInt(i));
			}

			return result;
		} catch (JSONException jse) {
			return null;
		}
	}

	/**
	 * Get count of router outlinks
	 */
	private int getOutLinksCount(int routerID, VimFunctions v) {
		try {
			JSONObject jsobj = v.getRouterInfo(routerID);
			JSONArray list = jsobj.getJSONArray("links");

			return list.length();

		} catch (JSONException jse) {
			return 0;
		}
	}

	/**
	 * Header for errors
	 */

	private String leadin() {
		return "IKMSTrafficEngine:";
	}

}
