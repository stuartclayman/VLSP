/** This class contains the options used for a router
 */
package usr.router;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import rgc.xmlparse.ReadXMLUtils;
import rgc.xmlparse.XMLNoTagException;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.protocol.Protocol;


public class RouterOptions {

    Router router_;

    // Parameters set in RoutingParameters Tag
    // how many millis to wait between checks of routing table
    int maxCheckTime_ = 60000;    // Interface wakes up this often anyway
    int minNetIFUpdateTime_ = 1000;  // Shortest interval between routing updates down given NetIF
    int maxNetIFUpdateTime_ = 30000;  // Longest interval between routing updates down given NetIF


    int maxDist_ = 20; // If a router is at a distance more than this it is assumed unroutable
    // Parameters set in APManager Tag

    String APManagerName_ = null;    // Name of  APManager
    String APOutputPath_ = null;   // Path to which infosource and aggpoint should write
    int maxAPs_ = 0;   // max APs
    int minAPs_ = 0;   // min APs
    int routerConsiderTime_ = 10000;   // Time router reconsiders APs

    int controllerConsiderTime_ = 10000;   // Time controller reconsiders APs
    int controllerRemoveTime_ = 0;           // Time to consider removing weakest AP --
    // if non-zero then weakest AP removed
    // and appropriate new APs added
    int maxAPWeight_ = 0;  // Maximum link weight an AP can be away
    String APFilter_ = null;  // AP filtering percentage
    String monType_ = "rt";      // What to monitor
    int trafficStatTime_ = 10000;  // Time to send traffic stats
    double apLifeBias_ = 0.0;  // Weight to give to AP lifetime predictions  -- 0 means ignore
    double minPropAP_ = 0.0;     // Minimum proportion of AP
    double maxPropAP_ = 1.0;     // Maximum proportion of AP
    String [] APParms_ = {}; // Parameters for AP Options
    

     // The name of the class to use to start a Router
    String routerClass = null;
    String routerArgs = null;       // some  optional args for a Router

     // The name of the class to use to start an Agg Point on a Router
    String apClass = null;
    String apArgs = null;       // some  optional args for an Agg Point

    String outputFileName_ = ""; // output file name
    boolean outputFileAddName_ = false; // Add suffix to output file
    String errorFileName_ = ""; // output file name for error stream
    boolean errorFileAddName_ = false; // Add suffix to output file
    boolean latticeMonitoring = false;  // If true, turn on Lattice Monitoring
    HashMap<String, Integer> probeInfoMap = null; // A Class Name -> datarate mapping

    RouteSelectionPolicy routePolicy_ = null;     // Policy for choosing route from merging tables
    String extendedOutputFile_ = null;            // File name for more output
    boolean gracefulExit_ = true;                 // Routers send a message before leaving


    // Link type - one of UDP or TCP
    enum LinkType { UDP, TCP };

    LinkType linkType = null;

    
    /** Constructor for router Options */

    public RouterOptions (Router router) {
        router_ = router;
        init();
    }

    public RouterOptions () {
        router_ = null;
        init();
    }

    /** init function sets up defaults and basic information */
    void init () {
        probeInfoMap = new HashMap<String, Integer>();
    }

    public void setOptionsFromFile(String fName) throws java.io.FileNotFoundException,
    SAXParseException, SAXException, javax.xml.parsers.ParserConfigurationException,
    java.io.IOException {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse (new File(fName));

        // normalize text representation
        doc.getDocumentElement ().normalize ();

        parseXML(doc);
        //Logger.getLogger("log").logln(USR.ERROR, "Read options from string");
    }

    public void setOptionsFromString(String XMLString) throws java.io.FileNotFoundException,
    SAXParseException, SAXException, javax.xml.parsers.ParserConfigurationException,
    java.io.IOException {
        //Logger.getLogger("log").logln(USR.ERROR, "Options string "+XMLString);

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        StringReader sr = new StringReader(XMLString);
        InputSource is = new InputSource(sr);
        Document doc = docBuilder.parse (is);

        // normalize text representation
        doc.getDocumentElement ().normalize ();

        parseXML(doc);

    }

    /** Parse the XML which represents router options */
    public void parseXML(Document doc) throws java.io.FileNotFoundException,
    SAXParseException, SAXException {
        Node basenode = doc.getDocumentElement();
        String basenodeName = basenode.getNodeName();

        if (!basenodeName.equals("RouterOptions")) {
            throw new SAXException("Base tag should be RouterOptions");
        }

        NodeList rt = doc.getElementsByTagName("Router");

        if (rt != null) {
            processRouter(rt);
        }

        NodeList rps = doc.getElementsByTagName("RoutingParameters");

        if (rps != null) {
            processRoutingParameters(rps);
        }

        NodeList out = doc.getElementsByTagName("Output");

        if (out != null) {
            processOutputParameters(out);
        }

        NodeList apm = doc.getElementsByTagName("APManager");

        if (apm != null) {
            processAPM(apm);
        }

        NodeList ap = doc.getElementsByTagName("AP");

        if (ap != null) {
            processAP(ap);
        }

        NodeList mon = ((Element)basenode).getElementsByTagName("Monitoring");

        if (mon != null) {
            processMonitoring(mon);
        }


        // Check for other unparsed tags
        Element el = doc.getDocumentElement();
        NodeList rest = el.getChildNodes();

        for (int i = 0; i < rest.getLength(); i++) {
            Node n = rest.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                throw new SAXException("Unrecognised tag "+n.getNodeName());
            }

        }

    }

    /** Process the part of the XML related to routing parameters */
    void processOutputParameters(NodeList out) throws SAXException {

        if (out.getLength() > 1) {
            throw new SAXException ("Only one Output tag allowed.");
        }

        if (out.getLength() == 0) {
            return;
        }
        Node o = out.item(0);


        try {
            outputFileName_ = ReadXMLUtils.parseSingleString(o,
                                                             "FileName", "Output", true);
            ReadXMLUtils.removeNode(o, "FileName", "Output");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            outputFileAddName_ = ReadXMLUtils.parseSingleBool(o,
                                                              "ExtendedName", "Output", true);
            ReadXMLUtils.removeNode(o, "ExtendedName", "Output");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            errorFileName_ = ReadXMLUtils.parseSingleString(o,
                                                            "ErrorFileName", "Output", true);
            ReadXMLUtils.removeNode(o, "ErrorFileName", "Output");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            errorFileAddName_ = ReadXMLUtils.parseSingleBool(o,
                                                             "ErrorExtendedName", "Output", true);
            ReadXMLUtils.removeNode(o, "ErrorExtendedName", "Output");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        NodeList nl = o.getChildNodes();

        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                throw new SAXException("Unrecognised tag "+n.getNodeName());
            }

        }
        o.getParentNode().removeChild(o);
    }

    /** Process the part of the XML related to Router setup */
    void processRouter(NodeList router) throws SAXException {
        if (router.getLength() > 1) {
            throw new SAXException ("Only one Router tag allowed.");
        }

        if (router.getLength() == 0) {
            return;
        }
        Node a = router.item(0);

        // What is the name of the class for PlacementEngine
        try {
            routerClass = ReadXMLUtils.parseSingleString(a, "RouterClass", "Router", true);

            ReadXMLUtils.removeNode(a, "RouterClass", "Router");

            Logger.getLogger("log").logln(USR.STDOUT, "RouterClass = " + routerClass);

            Class.forName(routerClass).asSubclass(Router.class);
        } catch (SAXException e) {
            throw new SAXException("Unable to parse class name " + routerClass + " in Router options" + e.getMessage());
        } catch (XMLNoTagException e) {
        } catch (ClassNotFoundException e) {
            throw new Error("Class not found for class name " + routerClass); 
        } catch (ClassCastException e) {
            throw new Error("Class name " + routerClass + " must be sub type of Router");
        }


        try {
            routerArgs = ReadXMLUtils.parseSingleString(a, "RouterArgs", "Router", true);
            ReadXMLUtils.removeNode(a, "RouterArgs", "Router");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }


        NodeList nl = a.getChildNodes();

        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                throw new SAXException("Unrecognised tag "+n.getNodeName());
            }

        }
        a.getParentNode().removeChild(a);

    }

    /** Process the part of the XML related to routing parameters */
    void processRoutingParameters(NodeList rps) throws SAXException {

        if (rps.getLength() > 1) {
            throw new SAXException ("Only one RoutingParameters tag allowed.");
        }

        if (rps.getLength() == 0) {
            return;
        }
        Node rp = rps.item(0);
        try {
            int n = ReadXMLUtils.parseSingleInt(rp, "TrafficStatTime", "RoutingParameters", true);
            trafficStatTime_ = n;
            ReadXMLUtils.removeNode(rp, "TrafficStatTime", "RoutingParameters");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            gracefulExit_ = ReadXMLUtils.parseSingleBool(rp,
                                                         "GracefulExit", "RoutingParameters", true);
            ReadXMLUtils.removeNode(rp, "GracefulExit",
                                    "RoutingParameters");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
        }

        String pol = "";
        try {
            pol = ReadXMLUtils.parseSingleString(rp, "RoutingPolicy",
                                                 "RoutingParameters", true);
            Class<?> routepol = Class.forName(pol);

            if (!RouteSelectionPolicy.class.isAssignableFrom(routepol)) {
                throw new SAXException("In Routing policy tag " + pol
                                       + " does not implement RouteSelectionPolicy interface");
            }

            Constructor<?> c = routepol.getConstructor(new Class[0]);
            routePolicy_ = (RouteSelectionPolicy)c.newInstance(
                    new Object[0]);

            ReadXMLUtils.removeNode(rp, "RoutingPolicy",
                                    "RoutingParameters");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
        } catch (InstantiationException e) {
            throw new SAXException("Trying to create RoutingPolicy: "
                                   + "Cannot create object of class " + pol + "\n" +
                                   e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new SAXException("Trying to create RoutingPolicy: "
                                   + "Cannot create object of class " + pol + "\n" +
                                   e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new SAXException("Trying to create RoutingPolicy: "
                                   + "Cannot create object of class " + pol + "\n" +
                                   e.getMessage());
        } catch (IllegalAccessException e) {
            throw new SAXException("Trying to create RoutingPolicy: "
                                   + "Cannot create object of class " + pol + "\n" +
                                   e.getMessage());
        } catch (InvocationTargetException e) {
            throw new SAXException("Trying to create RoutingPolicy: "
                                   + "Cannot create object of class " + pol + "\n" +
                                   e.getMessage());
        }

        // Process DatagramType
        String dgtype = "";
        try {
            dgtype = ReadXMLUtils.parseSingleString(rp, "DatagramType", "RoutingParameters", true);
            ReadXMLUtils.removeNode(rp, "DatagramType", "RoutingParameters");
            Class.forName(dgtype).asSubclass(Datagram.class );
            DatagramFactory.setClassForProtocol(dgtype, Protocol.DATA);
            DatagramFactory.setClassForProtocol(dgtype, Protocol.CONTROL);
        } catch (ClassNotFoundException e) {
            throw new SAXException("Class not found for class name "+dgtype+" in Routing options");
        } catch (SAXException e) {
            throw new SAXException("Unable to parse class name "+dgtype+" in Routing options"+e.getMessage());
        } catch (ClassCastException e) {
            throw new SAXException("Class name "+dgtype+" must be sub type of Datagram in Routing options");
        } catch (XMLNoTagException e) {

        }

        // Process AddressType
        String addrtype = "";
        try {
            String existing = AddressFactory.getClassForAddress();

            addrtype = ReadXMLUtils.parseSingleString(rp, "AddressType", "RoutingParameters", true);
            ReadXMLUtils.removeNode(rp, "AddressType", "RoutingParameters");
            Class.forName(addrtype).asSubclass(Address.class );

            if (existing == null ||  !existing.equals(addrtype)) {
                AddressFactory.setClassForAddress(addrtype);
            }

        } catch (ClassNotFoundException e) {
            throw new SAXException("Class not found for class name "+addrtype+" in Routing options");
        } catch (SAXException e) {
            throw new SAXException("Unable to parse class name "+addrtype+" in Routing options"+e.getMessage());
        } catch (ClassCastException e) {
            throw new SAXException("Class name "+addrtype+" must be sub type of Address in Routing options");
        } catch (XMLNoTagException e) {

        }

        // Process LinkType - either UDP or TCP
        String linktype = "";
        try {
            linktype = ReadXMLUtils.parseSingleString(rp, "LinkType", "RoutingParameters", true);
            ReadXMLUtils.removeNode(rp, "LinkType", "RoutingParameters");

            if (linktype.equals("UDP")) {
                linkType = LinkType.UDP;
            } else if (linktype.equals("TCP")) {
                linkType = LinkType.TCP;
            } else {
                throw new Exception("Unknown LinkType " + linktype);
            }

        } catch (ClassNotFoundException e) {
            throw new SAXException("Class not found for class name "+addrtype+" in Routing options");
        } catch (SAXException e) {
            throw new SAXException("Unable to parse class name "+addrtype+" in Routing options"+e.getMessage());
        } catch (ClassCastException e) {
            throw new SAXException("Class name "+addrtype+" must be sub type of Address in Routing options");
        } catch (XMLNoTagException e) {
        } catch (Exception e) {
            throw new SAXException(e.getMessage());
        }

        try {
            int n = ReadXMLUtils.parseSingleInt(rp, "MaxCheckTime", "RoutingParameters", true);
            maxCheckTime_ = n;
            ReadXMLUtils.removeNode(rp, "MaxCheckTime", "RoutingParameters");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            int n = ReadXMLUtils.parseSingleInt(rp, "MinNetIFUpdateTime", "RoutingParameters", true);
            minNetIFUpdateTime_ = n;
            ReadXMLUtils.removeNode(rp, "MinNetIFUpdateTime", "RoutingParameters");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            int n = ReadXMLUtils.parseSingleInt(rp, "MaxNetIFUpdateTime", "RoutingParameters", true);
            maxNetIFUpdateTime_ = n;
            ReadXMLUtils.removeNode(rp, "MaxNetIFUpdateTime", "RoutingParameters");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            int n = ReadXMLUtils.parseSingleInt(rp, "MaxDist", "RoutingParameters", true);
            maxDist_ = n;

            if (n != 0) {
                DatagramFactory.setInitialTTL(n);
            }
            ReadXMLUtils.removeNode(rp, "MaxDist", "RoutingParameters");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        NodeList nl = rp.getChildNodes();

        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                throw new SAXException("Unrecognised tag "+n.getNodeName());
            }

        }
        rp.getParentNode().removeChild(rp);
    }

    /** Process the part of the XML related to Access Point management */
    void processAPM(NodeList apm) throws SAXException {

        if (apm.getLength() > 1) {
            throw new SAXException ("Only one APManager tag allowed.");
        }

        if (apm.getLength() == 0) {
            return;
        }
        Node n = apm.item(0);

        try {
            APManagerName_ = ReadXMLUtils.parseSingleString
                    (n, "Name", "APManager", true);
            ReadXMLUtils.removeNode(n, "Name", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            APOutputPath_ = ReadXMLUtils.parseSingleString
                    (n, "OutputPath", "APManager", true);
            // Now set up path
            File fname = new File(APOutputPath_);
            fname.mkdir();
            ReadXMLUtils.removeNode(n, "OutputPath", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            maxAPs_ = ReadXMLUtils.parseSingleInt
                    (n, "MaxAPs", "APManager", true);
            ReadXMLUtils.removeNode(n, "MaxAPs", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            minAPs_ = ReadXMLUtils.parseSingleInt
                    (n, "MinAPs", "APManager", true);
            ReadXMLUtils.removeNode(n, "MinAPs", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            routerConsiderTime_ = ReadXMLUtils.parseSingleInt
                    (n, "RouterConsiderTime", "APManager", true);
            ReadXMLUtils.removeNode(n, "RouterConsiderTime", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            controllerConsiderTime_ = ReadXMLUtils.parseSingleInt
                    (n, "ControllerConsiderTime", "APManager", true);
            ReadXMLUtils.removeNode(n, "ControllerConsiderTime", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            controllerRemoveTime_ = ReadXMLUtils.parseSingleInt
                    (n, "ControllerRemoveTime", "APManager", true);
            ReadXMLUtils.removeNode(n, "ControllerRemoveTime", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            maxAPWeight_ = ReadXMLUtils.parseSingleInt
                    (n, "MaxAPWeight", "APManager", true);
            ReadXMLUtils.removeNode(n, "MaxAPWeight", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            APFilter_ = ReadXMLUtils.parseSingleString
                    (n, "APFilter", "APManager", true);
            ReadXMLUtils.removeNode(n, "APFilter", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            monType_ = ReadXMLUtils.parseSingleString
                    (n, "MonitorType", "APManager", true);
            ReadXMLUtils.removeNode(n, "MonitorType", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        try {
            minPropAP_ = ReadXMLUtils.parseSingleDouble
                    (n, "MinPropAP", "APManager", true);
            ReadXMLUtils.removeNode(n, "MinPropAP", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
        }

        try {
            maxPropAP_ = ReadXMLUtils.parseSingleDouble
                    (n, "MaxPropAP", "APManager", true);
            ReadXMLUtils.removeNode(n, "MaxPropAP", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
        }

        try {
            apLifeBias_ = ReadXMLUtils.parseSingleDouble
                    (n, "APLifeBias", "APManager", true);
            ReadXMLUtils.removeNode(n, "APLifeBias", "APManager");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
        }

        try {
            APParms_ = ReadXMLUtils.parseArrayString(n, "Parameter", "APManager");
            ReadXMLUtils.removeNodes(n, "Parameter", "APManager");
        } catch (SAXException e) {
            throw e;
        }

        // for (int i= 0; i < APParms_.length; i++) {
        //     Logger.getLogger("log").logln(USR.ERROR, "READ "+APParms_[i]);
        //}
        NodeList nl = n.getChildNodes();

        for (int i = 0; i < nl.getLength(); i++) {
            Node n0 = nl.item(i);

            if (n0.getNodeType() == Node.ELEMENT_NODE) {
                throw new SAXException("Unrecognised tag in APManager"+n0.getNodeName());
            }

        }

        n.getParentNode().removeChild(n);
    }

    /** Process the part of the XML related to Access Point setup */
    void processAP(NodeList ap) throws SAXException {
        if (ap.getLength() > 1) {
            throw new SAXException ("Only one AP tag allowed.");
        }

        if (ap.getLength() == 0) {
            return;
        }
        Node a = ap.item(0);

        // What is the name of the class for PlacementEngine
        try {
            apClass = ReadXMLUtils.parseSingleString(a, "APClass", "AP", true);

            ReadXMLUtils.removeNode(a, "APClass", "AP");

            Logger.getLogger("log").logln(USR.STDOUT, "APClass = " + apClass);

            Class.forName(apClass).asSubclass(AP.class);
        } catch (SAXException e) {
            throw new SAXException("Unable to parse class name " + apClass + " in AP options" + e.getMessage());
        } catch (XMLNoTagException e) {
        } catch (ClassNotFoundException e) {
            throw new Error("Class not found for class name " + apClass); 
        } catch (ClassCastException e) {
            throw new Error("Class name " + apClass + " must be sub type of AP");
        }


        try {
            apArgs = ReadXMLUtils.parseSingleString(a, "APArgs", "AP", true);
            ReadXMLUtils.removeNode(a, "APArgs", "AP");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        // for (int i= 0; i < APParms_.length; i++) {
        //     Logger.getLogger("log").logln(USR.ERROR, "READ "+APParms_[i]);
        //}
        NodeList nl = a.getChildNodes();

        for (int i = 0; i < nl.getLength(); i++) {
            Node n0 = nl.item(i);

            if (n0.getNodeType() == Node.ELEMENT_NODE) {
                throw new SAXException("Unrecognised tag in AP"+n0.getNodeName());
            }

        }

        a.getParentNode().removeChild(a);
    }

    /**
     * Process Monitoring
     */
    private void processMonitoring(NodeList list) throws SAXException {
        if (list.getLength() > 1) {
            throw new SAXException ("Only one Monitoring tag allowed.");
        }

        if (list.getLength() == 0) {
            return;
        }


        Node mon = list.item(0);


        // Should the Router turn on Lattice Monitoring
        try {
            latticeMonitoring = ReadXMLUtils.parseSingleBool(mon, "LatticeMonitoring", "Monitoring", true);
            ReadXMLUtils.removeNode(mon, "LatticeMonitoring", "Monitoring");

            //Logger.getLogger("log").logln(USR.STDOUT, "LatticeMonitoring = " + latticeMonitoring);
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {

        }

        // get Probes
        try {
            // First get all nodes called 'Probe'
            NodeList probes = ((Element)mon).getElementsByTagName("Probe");

            if (probes.getLength() != 0) {
                for (int p = 0; p < probes.getLength(); p++) {
                    Node el = probes.item(p);

                    try {
                        String name = ReadXMLUtils.parseSingleString(el, "Name", "Probe", true);
                        ReadXMLUtils.removeNodes(el, "Name", "Probe");

                        Integer datarate = ReadXMLUtils.parseSingleInt(el, "Rate", "Probe", true);
                        ReadXMLUtils.removeNodes(el, "Rate", "Probe");

                        //Logger.getLogger("log").logln(USR.STDOUT, "Probe: name = " + name + " datarate = " + datarate);

                        probeInfoMap.put(name, datarate);

                    } catch (SAXException e) {
                        throw e;
                    } catch (XMLNoTagException nte) {
                        Logger.getLogger("log").logln(USR.ERROR, nte.getMessage());
                    }
                }

                // Remove all 'Probe' nodes
                ReadXMLUtils.removeNodes(mon, "Probe", "Monitoring");


            }

        } catch (SAXException e) {
            throw e;
        }

        // Clean up
        NodeList nl = mon.getChildNodes();

        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                throw new SAXException("Monitoring XML unrecognised tag "+n.getNodeName());
            }

        }
        mon.getParentNode().removeChild(mon);


    }

    /** Return the time between sending traffic statistics */
    public int getTrafficStatTime() {
        return trafficStatTime_;
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

    /** Accessor function for name of Router class*/
    public String getRouterClassName() {
        return routerClass;
    }




    /** Accessor function for name of AP controller */
    public String getAPControllerName() {
        return APManagerName_;
    }

    /** Accessor function for output path for AP*/
    public String getAPOutputPath() {
        return APOutputPath_;
    }

    /** Accessor function for max no of APs */
    public int getMaxAPs() {
        return maxAPs_;
    }

    /** Accessor function for min no of APs */
    public int getMinAPs() {
        return minAPs_;
    }

    /** Accessor function for name of AP class*/
    public String getAPClassName() {
        return apClass;
    }


    /** Accessor function for router Consider time */
    public int getRouterConsiderTime() {
        return routerConsiderTime_;
    }

    /** Accessor function for global controller consider time*/
    public int getControllerConsiderTime() {
        return controllerConsiderTime_;
    }

    /** Accessor function for maximum weight to AP*/
    public int getMaxAPWeight() {
        return maxAPWeight_;
    }

    /** Accessor function for monitor type */
    public String getMonType() {
        return monType_;
    }

    /** Accessor function for AP filter type */
    public String getAPFilter() {
        return APFilter_;
    }

    /** Accessor function for maximum dist in network*/
    public int getMaxDist() {
        return maxDist_;
    }

    /** Accessor function for AP parameters */
    public String[] getAPParms() {
        return APParms_;
    }

    /** Accessor function for minimum proportion of access points */
    public double getMinPropAP() {
        return minPropAP_;
    }

    /** Accessor function for maximum proportion of access points */
    public double getMaxPropAP() {
        return maxPropAP_;
    }

    /** Accessor function for AP life bias -- weight given to life
     * estimation in working out which AP to choose */
    public double getAPLifeBias() {
        return apLifeBias_;
    }

    /** Accessor function for output file name */
    public String getOutputFile() {
        return outputFileName_;
    }

    /** Accessor function for output file name addition flag*/
    public boolean getOutputFileAddName() {
        return outputFileAddName_;
    }

    /** Accessor function for output file name */
    public String getErrorFile() {
        return errorFileName_;
    }

    /** Accessor function for output file name addition flag*/
    public boolean getErrorFileAddName() {
        return errorFileAddName_;
    }

    /**
     * Should we turn on Lattice Monitoring
     */
    public boolean latticeMonitoring() {
        return latticeMonitoring;
    }

    /** Does a router send an exit message before leaving
     */
    public boolean gracefulExit() {
        return gracefulExit_;
    }

    /**
     * Get the LinkType
     */
    public LinkType getLinkType() {
        return linkType;
    }

    /**
     * Get the Probe Info Map
     */
    public HashMap<String, Integer> getProbeInfoMap() {
        return probeInfoMap;
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
