/** This class contains the options used for a router
 */
package usr.router;

import java.io.*;
import usr.engine.*;
import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException; 
import usr.common.*;


public class RouterOptions {
   
    Router router_;
        
    // Parameters set in RoutingParameters Tag
        // how many millis to wait between checks of routing table
    int maxCheckTime_ = 60000;    // Interface wakes up this often anyway
    int minNetIFUpdateTime_= 1000;  // Shortest interval between routing updates down given NetIF
    int maxNetIFUpdateTime_= 30000;  // Longest interval between routing updates down given NetIF
    
    // Parameters set in APManager Tag
    
    String APManagerName_= null;    // Name of  APManager
    int maxAPs_= 0;   // max APs
    int minAPs_= 0;   // min APs
    int routerConsiderTime_= 10000;   // Time router reconsiders
    int controllerConsiderTime_= 10000;   // Time controller reconsiders
    int maxAPHops_= 5;   // Maximum number of hops an AP can be away
    int maxAPWeight_= 0;  // Maximum link weight an AP can be aways
    String []APParms_= {}; // Parameters for AP Options
    
    /** Constructor for router Options */
    
    public RouterOptions (Router router) {
        router_= router;
        init();
    }   
   
    /** init function sets up defaults and basic information */
    void init () {
    }
    
    public void setOptionsFromFile(String fName) throws java.io.FileNotFoundException,
        SAXParseException, SAXException, javax.xml.parsers.ParserConfigurationException,
        java.io.IOException
    {

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse (new File(fName));

            // normalize text representation
            doc.getDocumentElement ().normalize ();
            
            parseXML(doc);
            //System.err.println("Read options from string");
    }
    
    public void setOptionsFromString(String XMLString) throws java.io.FileNotFoundException,
        SAXParseException, SAXException, javax.xml.parsers.ParserConfigurationException,
        java.io.IOException
    {
           //System.err.println("Options string "+XMLString);
           
          DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            StringReader sr= new StringReader(XMLString);
            InputSource is= new InputSource(sr);
            Document doc = docBuilder.parse (is);

            // normalize text representation
            doc.getDocumentElement ().normalize ();
            
            parseXML(doc);

    }
    
    /** Parse the XML which represents router options */
    public void parseXML(Document doc) throws java.io.FileNotFoundException,
        SAXParseException, SAXException
    {
        String basenode= doc.getDocumentElement().getNodeName();
        if (!basenode.equals("RouterOptions")) {
            throw new SAXException("Base tag should be RouterOptions");
        }
        
        NodeList rps= doc.getElementsByTagName("RoutingParameters");
        if (rps != null) {
            processRoutingParameters(rps);
        }
        NodeList apm= doc.getElementsByTagName("APManager");
        if (apm != null) {
            processAPM(apm);
        }
        // Check for other unparsed tags
        Element el= doc.getDocumentElement();
        NodeList rest= el.getChildNodes();
        for (int i= 0; i < rest.getLength(); i++) { 
            Node n= rest.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                 throw new SAXException("Unrecognised tag "+n.getNodeName());
            }
             
        }
   
    }
    
    /** Process the part of the XML related to routing parameters */
    void processRoutingParameters(NodeList rps) throws SAXException
    {
        
        if (rps.getLength() > 1) {
            throw new SAXException ("Only one RoutingParameters tag allowed.");
        }
        if (rps.getLength() == 0) 
            return;
        Node rp= rps.item(0);
        
      
        try {
           int n= ReadXMLUtils.parseSingleInt(rp, "MaxCheckTime","RoutingParameters",true);
           maxCheckTime_= n;
           ReadXMLUtils.removeNode(rp,"MaxCheckTime","RoutingParameters");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
           int n= ReadXMLUtils.parseSingleInt(rp, "MinNetIFUpdateTime","RoutingParameters",true);
           minNetIFUpdateTime_= n;
           ReadXMLUtils.removeNode(rp,"MinNetIFUpdateTime","RoutingParameters");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
           int n= ReadXMLUtils.parseSingleInt(rp, "MaxNetIFUpdateTime","RoutingParameters",true);
           maxNetIFUpdateTime_= n;
          ReadXMLUtils.removeNode(rp,"MaxNetIFUpdateTime","RoutingParameters");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }   
        NodeList nl= rp.getChildNodes();
        for (int i= 0; i < nl.getLength(); i++) {         
            Node n= nl.item(i); 
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                throw new SAXException("Unrecognised tag "+n.getNodeName());
            }
                
        } 
        rp.getParentNode().removeChild(rp);
    }
    
    /** Process the part of the XML related to Access Point management */
    void processAPM(NodeList apm) throws SAXException
    {
        
        if (apm.getLength() > 1) {
            throw new SAXException ("Only one APManager tag allowed.");
        }
        if (apm.getLength() == 0) 
            return;
        Node n= apm.item(0);

        try {
            APManagerName_= ReadXMLUtils.parseSingleString
              (n, "Name","APManager",true);
            ReadXMLUtils.removeNode(n,"Name","APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
            maxAPs_= ReadXMLUtils.parseSingleInt
              (n, "MaxAPs","APManager",true);
            ReadXMLUtils.removeNode(n,"MaxAPs","APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
            minAPs_= ReadXMLUtils.parseSingleInt
              (n, "MinAPs","APManager",true);
            ReadXMLUtils.removeNode(n,"MinAPs","APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
            routerConsiderTime_= ReadXMLUtils.parseSingleInt
              (n, "RouterConsiderTime","APManager",true);
            ReadXMLUtils.removeNode(n,"RouterConsiderTime","APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
            controllerConsiderTime_= ReadXMLUtils.parseSingleInt
              (n, "ControllerConsiderTime","APManager",true);
            ReadXMLUtils.removeNode(n,"ControllerConsiderTime","APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
            maxAPHops_= ReadXMLUtils.parseSingleInt
              (n, "MaxAPHops","APManager",true);
            ReadXMLUtils.removeNode(n,"MaxAPHops","APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
            maxAPWeight_= ReadXMLUtils.parseSingleInt
              (n, "MaxAPWeight","APManager",true);
            ReadXMLUtils.removeNode(n,"MaxAPWeight","APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
            APParms_= ReadXMLUtils.parseArrayString(n,"Parameter","APManager");
            ReadXMLUtils.removeNodes(n,"Parameter","APManager");
        } catch (SAXException e) {
            throw e;
        } 
       // for (int i= 0; i < APParms_.length; i++) {
       //     System.err.println("READ "+APParms_[i]);
        //}
        NodeList nl= n.getChildNodes();
        for (int i= 0; i < nl.getLength(); i++) {         
            Node n0= nl.item(i); 
            if (n0.getNodeType() == Node.ELEMENT_NODE) {
                throw new SAXException("Unrecognised tag in APManager"+n0.getNodeName());
            }
                
        } 
        
        n.getParentNode().removeChild(n);
    }
    
    /** Return the longest time between router fabric wake ups */
    public int getMaxCheckTime() {
        return maxCheckTime_;
    }
       
    /** Return the shortest time between network interface routing
    table updates */
    public int getMinNetIFUpdateTime() {
         return minNetIFUpdateTime_;
    }
    
    /** Return the longest time between network interface routing table
    updates*/
    public int getMaxNetIFUpdateTime() {
         return maxNetIFUpdateTime_;
    }
    
    /** Accessor function for name of AP controller */
    public String getAPControllerName()
    { 
        return APManagerName_;
    }
  
    /** Accessor function for max no of APs */
    public int getMaxAPs()
    { 
        return maxAPs_;
    }
    
     /** Accessor function for min no of APs */
    public int getMinAPs()
    { 
        return minAPs_;
    }
  
      /** Accessor function for router Consider time */
    public int getRouterConsiderTime()
    { 
        return routerConsiderTime_;
    }
    
     /** Accessor function for global controller consider time*/
    public int getControllerConsiderTime()
    { 
        return controllerConsiderTime_;
    }
    
    /** Accessor function for maximum number of hops to AP*/
    public int getMaxAPHops()
    { 
        return maxAPHops_;
    }
    
    /** Accessor function for maximum weight to AP*/
    public int getMaxAPWeight()
    { 
        return maxAPWeight_;
    }
    
    /** Accessor function for AP parameters */
    public String[] getAPParms() 
    {
        return APParms_;
    }
    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String RO = "RO: ";
        RouterController controller = router_.getRouterController();

        return controller.getName() + " " + RO;
    }
}


