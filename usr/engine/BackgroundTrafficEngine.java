/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import usr.globalcontroller.*;
import rgc.xmlparse.*;
import rgc.probdistributions.*;
import usr.logging.*;

import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import java.io.*;
import java.util.*;

public class BackgroundTrafficEngine implements EventEngine  {
    long timeToEnd_;  // Time before end of simulation
    ProbDistribution trafficArriveDist_= null;
    ProbDistribution trafficLengthDist_= null;
    ProbDistribution trafficSpeedDist_= null;
    boolean scaleWithNetwork_= true;
    boolean preferEmptyNodes_= true;

    /** Contructor from Parameter string */
    public BackgroundTrafficEngine(int time, String parms) throws EventEngineException
    {
	timeToEnd_= time*1000;
	parseXMLFile(parms);

	if (trafficArriveDist_ == null) {
	    throw new EventEngineException("Background traffic engine must specify arrival distribution.");
	}
	if (trafficLengthDist_ == null) {
	    throw new EventEngineException("Background traffic engine must specify length distribution.");
	}
	if (trafficSpeedDist_ == null) {
	    throw new EventEngineException("Background traffic engine must specify speed distribution.");
	}
    }

    /** Initial events to add to schedule */
    public void startStopEvents(EventScheduler s, GlobalController g)
    {
	// simulation start
	SimEvent e0 = new SimEvent(SimEvent.EVENT_START_SIMULATION, 0, null,this);
	s.addEvent(e0);

	// simulation end
	SimEvent e= new SimEvent(SimEvent.EVENT_END_SIMULATION, timeToEnd_, null,this);
	s.addEvent(e);

    }

    /** Initial events to add to schedule */
    public void initialEvents(EventScheduler s, GlobalController g)
    {
	startNewConnection(0, s, g);
    }

    /** Add or remove events following a simulation event */
    public void preceedEvent(SimEvent e, EventScheduler s,  GlobalController g)
    {

    }

    /** Add or remove events following a simulation event */
    public void followEvent(SimEvent e, EventScheduler s,  GlobalController g,
                            Object o)
    {
	startNewConnection(e.getTime(), s,g);
    }

    /** Start new connection between nodes at a given time */
    private void startNewConnection(long currTime, EventScheduler s,
                                    GlobalController g)
    {
	double scale= 1.0;

	if (scaleWithNetwork_) {
	    int noRouters= g.getNoRouters();
	    if (noRouters > 1)
		scale= 1.0/noRouters;
	}
	long time= 0;
	try {
	    time= (long)(trafficArriveDist_.getVariate()*1000*scale);
	} catch (ProbException e) {
	     Logger.getLogger("log").logln(USR.ERROR, 
		    leadin()+" Error generating trafficArriveDist variate");
	     time= 0;
	}
	if (currTime+time > timeToEnd_)
	    return;
	SimEvent e= new SimEvent(SimEvent.EVENT_NEW_TRAFFIC_CONNECTION,
	                         time, null, this);
	s.addEvent(e);
    }

    /** Will empty nodes be connected to more frequently*/
    public boolean preferEmptyNodes()
    {
	return preferEmptyNodes_;
    }

    /** rate at which to transfer bytes/sec*/
    public double getRate()
    {
	return 1.5;
    }


    /** Return a free port on which to listen for a given router Id*/
    public int getReceivePort(int rId)
    {
	return 80;
    }

    /** number of bytes to transfer*/
    public int getBytes()
    {
	return 10000;
    }

    /** Parse the XML to get probability distribution information*/
    private void parseXMLFile(String fName) throws EventEngineException
    {

	try {
	    DocumentBuilderFactory docBuilderFactory =
	        DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	    Document doc = docBuilder.parse (new File(fName));

	    // normalize text representation
	    doc.getDocumentElement ().normalize ();
	    String basenode= doc.getDocumentElement().getNodeName();
	    if (basenode == null) {
		throw new SAXException("Document requires basenode");
	    }
	    if (!basenode.equals("BackgroundTrafficEngine")) {
		throw new SAXException("Base tag should be BackgroundTrafficEngine");
	    }
	    NodeList n= doc.getElementsByTagName("TrafficArriveDist");
	    trafficArriveDist_= ProbDistribution.parseProbDist(n,"TrafficArriveDist");
	    if (trafficArriveDist_ == null) {
		throw new SAXException ("Must specific TrafficArriveDist");
	    }
	    n= doc.getElementsByTagName("TrafficLengthDist");
	    trafficLengthDist_= ProbDistribution.parseProbDist(n,"TrafficLengthDist");
	    if (trafficLengthDist_ == null) {
		throw new SAXException ("Must specific TrafficLengthDist");
	    }
	    n= doc.getElementsByTagName("TrafficArriveDist");
	    trafficSpeedDist_= ProbDistribution.parseProbDist(n,"TrafficSpeedDist");
	    if (trafficSpeedDist_ == null) {
		throw new SAXException ("Must specific TrafficSpeedDist");
	    }
	} catch (java.io.FileNotFoundException e) {
	    throw new EventEngineException("Parsing BackgroundTrafficEngine: Cannot find file "+fName);
	} catch (SAXParseException err) {
	    throw new EventEngineException ("Parsing BackgroundTrafficEngine: error" + ", line "
	                                    + err.getLineNumber () + ", uri " + err.getSystemId ());

	}catch (SAXException e) {
	    throw new EventEngineException(
	        "Parsing BackgroundTrafficEngine: Exception in SAX XML parser"+ e.getMessage());
	}catch (Throwable t) {
	    throw new EventEngineException("Parsing ProbabilisticEventEngine: "+t.getMessage());
	}
    }
    
    /**
     * Header for errors
     */
     
    private String leadin() {
	return new String ("BackgroundTrafficEngine:");
    }

}
