/** Interface for Engine which adds events to the event list
*/

package usr.engine;

import usr.globalcontroller.*;
import usr.common.*;
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
    public BackgroundTrafficEngine(int time, String parms)throws EventEngineException
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
	
    }
    
    /** Add or remove events following a simulation event */
    public void preceedEvent(SimEvent e, EventScheduler s,  GlobalController g) 
    {
    
    }
    
    /** Add or remove events following a simulation event */
    public void followEvent(SimEvent e, EventScheduler s,  GlobalController g,
        Object o)
    {
    
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
        trafficArriveDist_= ReadXMLUtils.parseProbDist(n,"TrafficArriveDist");
        if (trafficArriveDist_ == null) {
            throw new SAXException ("Must specific TrafficArriveDist");
        }
	n= doc.getElementsByTagName("TrafficLengthDist");
        trafficLengthDist_= ReadXMLUtils.parseProbDist(n,"TrafficLengthDist");
        if (trafficLengthDist_ == null) {
            throw new SAXException ("Must specific TrafficLengthDist");
        }
	n= doc.getElementsByTagName("TrafficArriveDist");
        trafficSpeedDist_= ReadXMLUtils.parseProbDist(n,"TrafficSpeedDist");
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

}
