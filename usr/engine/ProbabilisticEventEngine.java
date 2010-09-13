/** Interface for Engine which adds events to the event list
*/

package usr.engine;

import usr.globalcontroller.*;
import usr.common.*;

import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 
import java.io.*;

public class ProbabilisticEventEngine implements EventEngine {
    int timeToEnd_;
    ProbDistribution nodeCreateDist_= null;
    ProbDistribution nodeDeathDist_= null;
//    DiscreteProbDistribution linkCreateDist_= null;
    ProbDistribution linkDeathDist_= null;
    

    /** Contructor from Parameter string */
    public ProbabilisticEventEngine(int time, String parms) 
    {
        timeToEnd_= time;
        parseXMLFile(parms);
    }
    
    /** Initial events to add to schedule */
    public void initialEvents(EventScheduler s, GlobalController g)
    {
        // simulation start
        SimEvent e0 = new SimEvent(SimEvent.EVENT_START_SIMULATION, 0, null);
        s.addEvent(e0);
        
        // simulation end
        SimEvent e= new SimEvent(SimEvent.EVENT_END_SIMULATION, timeToEnd_, null);
        s.addEvent(e);

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
    private void parseXMLFile(String fName) 
    {
        try { DocumentBuilderFactory docBuilderFactory = 
          DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse (new File(fName));

        // normalize text representation
        doc.getDocumentElement ().normalize ();
        String basenode= doc.getDocumentElement().getNodeName();
        if (!basenode.equals("ProbabilisticEngine")) {
            throw new SAXException("Base tag should be ProbabilisticEngine");
        }
        NodeList nbd= doc.getElementsByTagName("NodeBirthDist");
        nodeCreateDist_= parseProbDist(nbd,"NodeBirthDist");
        if (nodeCreateDist_ == null) {
            throw new SAXException ("Must specific NodeBirthDist");
        }
        NodeList ndd= doc.getElementsByTagName("NodeDeathDist");
        nodeDeathDist_= parseProbDist(ndd,"NodeDeathDist");
        NodeList ldd= doc.getElementsByTagName("LinkDeathDist");
        linkDeathDist_= parseProbDist(ldd,"LinkDeathDist");
          
    } catch (java.io.FileNotFoundException e) {
          System.err.println("Cannot find file "+fName);
          System.exit(-1);
      }catch (SAXParseException err) {
          System.err.println ("** Parsing error" + ", line " 
             + err.getLineNumber () + ", uri " + err.getSystemId ());
          System.err.println(" " + err.getMessage ());
          System.exit(-1);

      }catch (SAXException e) {
          System.err.println("Exception in SAX XML parser.");
          System.err.println(e.getMessage());
          System.exit(-1);
          
      }catch (Throwable t) {
          t.printStackTrace ();
          System.exit(-1);
      }
    }
    
    private ProbDistribution parseProbDist(NodeList n, String tagname) 
      throws SAXException, ProbException, XMLNoTagException{
        if (n.getLength() == 0) 
            return null;
        if (n.getLength() > 1) {
            throw new SAXException("Only one tag of type "+tagname+
              " allowed");
        }
        ProbDistribution d= new ProbDistribution();
        
        Element el= (Element)n.item(0);
        
        NodeList els= el.getElementsByTagName("ProbElement");
        if (els.getLength() == 0) {
            throw new SAXException("Must have at least one element of "+
              "probability distribution in "+tagname);
        }  
        for (int i= 0; i < els.getLength(); i++) {
           Node no= els.item(i);
           ProbElement e= parseProbElement(no,tagname);
           d.addPart(e);
        }
        try {
           d.checkParts();
        } catch (ProbException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        return d;
    }
      
    private ProbElement parseProbElement(Node n, String tagname) 
      throws SAXException, ProbException, XMLNoTagException {
        
        String distName= ReadXMLUtils.parseSingleString(n,"Type",tagname,true);
        double weight= ReadXMLUtils.parseSingleDouble(n,"Weight",tagname,true);
        double []parms= ReadXMLUtils.parseArrayDouble(n,"Parameter",tagname);
        ProbElement e= new ProbElement(distName,weight,parms); 
       
        return e;
    }
}
