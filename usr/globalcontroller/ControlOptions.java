/** This class contains the options used for a simulation.  It reads in
 * an
 * XML file or string to generate them
 * The options specify hosts and controls used in simulation
 */
package usr.globalcontroller;

import usr.logging.*;
import usr.globalcontroller.visualization.Visualization;
import usr.localcontroller.LocalControllerInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.io.*;
import usr.engine.*;
import usr.events.*;
import org.w3c.dom.Document;
import org.w3c.dom.*;
import usr.output.OutputType;
import java.lang.reflect.Constructor;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import usr.engine.*;
import rgc.xmlparse.*;
import rgc.probdistributions.*;
import usr.router.RouterOptions;
import java.lang.reflect.InvocationTargetException;

public class ControlOptions
{
private ArrayList<LocalControllerInfo> localControllers_;
private int globalControlPort_ = 8888;                  // Port global
                                                        // controller
                                                        // listens on
private String remoteLoginCommand_ = null;              // Command used
                                                        // to login
                                                        // to start
                                                        // local
                                                        // controller
private String remoteStartController_ = null;           // Command used
                                                        // on
                                                        // local
                                                        // controller to
                                                        // start it
private String remoteLoginFlags_ = null;                //  Flags used
                                                        // for ssh to
                                                        // login to
                                                        // remote
                                                        // machine
private String remoteLoginUser_ = null;                 // User on
                                                        // remote
                                                        // machines to
                                                        // login
                                                        // with.
private boolean startLocalControllers_ = true;          // If true
                                                        // Global
                                                        // Controller
                                                        // starts
                                                        // local
                                                        // controllers
private boolean isSimulation_ = false;                  //  If true
                                                        // simulation in
                                                        // software not
                                                        // emulation
                                                        // in hardware
private boolean allowIsolatedNodes_ = true;             // If true,
                                                        // check for
                                                        // isolated
                                                        // nodes
private boolean connectedNetwork_ = false;              // If true, keep
                                                        // network
                                                        // connected
private boolean latticeMonitoring = false;              // If true, turn
                                                        // on
                                                        // Lattice
                                                        // Monitoring
private HashMap<String, String> consumerInfoMap = null; // An map of
                                                        // class
                                                        // names for
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        //
                                                        // Monitoring
                                                        // consumers
                                                        // and thier
                                                        // label

private int controllerWaitTime_ = 6;
private int lowPort_ = 10000;                           // Default
                                                        // lowest port
                                                        // to be used on
                                                        // local
                                                        // controller
private int highPort_ = 20000;                          // Default
                                                        // highest port
                                                        // to be used on
                                                        // local
                                                        // controller
private int maxLag_ = 10000;                            // Maximum lag
                                                        // tolerable in
                                                        // simulation
                                                        // in millisec
private String routerOptionsString_ = "";               //
private RouterOptions routerOptions_ = null;
private ArrayList <EventEngine> engines_ = null;        // Engines used
                                                        // to
                                                        // create new
                                                        // events for
                                                        // sim

private ArrayList <OutputType> outputs_ = null;

private String visualizationClass = null;

/** init function sets up basic information */
public void init(){
    engines_ = new ArrayList <EventEngine>();
    localControllers_ = new ArrayList<LocalControllerInfo>();
    outputs_ = new ArrayList <OutputType>();
    remoteLoginCommand_ = "/usr/bin/ssh";
    remoteLoginFlags_ = "-n";
    Properties prop = System.getProperties();
    remoteStartController_ =
        "java -cp " +
        prop.getProperty("java.class.path",
            null) + " usr.localcontroller.LocalController";
    routerOptions_ = new RouterOptions(null);
    consumerInfoMap = new HashMap<String, String>();
}

/** Adds information about a new host to the list
 */
private void addNewHost(LocalControllerInfo host){
    localControllers_.add(host);
}

/** Accessor function for router Options */

public RouterOptions getRouterOptions(){
    return routerOptions_;
}

/** Read control options from XML file
 */
public ControlOptions (String fName){
    init();
    readXML(fName);
}

void readXML(String fName){
    try { DocumentBuilderFactory docBuilderFactory =
              DocumentBuilderFactory.newInstance();
          DocumentBuilder docBuilder =
              docBuilderFactory.newDocumentBuilder();
          Document doc = docBuilder.parse(new File(fName));

          // normalize text representation
          doc.getDocumentElement().normalize();
          String basenode = doc.getDocumentElement().getNodeName();
          if (!basenode.equals("SimOptions")) throw new
                    SAXException(
                  "Base tag should be SimOptions");
          NodeList lcs = doc.getElementsByTagName("LocalController");
          processLocalControllers(lcs);
          NodeList gcs = doc.getElementsByTagName("GlobalController");
          processGlobalController(gcs);
          NodeList eng = doc.getElementsByTagName("EventEngine");
          processEventEngines(eng);
          NodeList ro = doc.getElementsByTagName("RouterOptions");
          processRouterOptions(ro);
          NodeList o = doc.getElementsByTagName("Output");
          processRouterOutputs(o);

          // Check all tags are processed
          // Check for other unparsed tags
          Element el = doc.getDocumentElement();
          NodeList rest = el.getChildNodes();
          for (int i = 0; i < rest.getLength(); i++) {
              Node n = rest.item(i);
              if (n.getNodeType() ==
                  Node.ELEMENT_NODE) {throw new SAXException(
                                          "Final tidy unrecognised tag "
                                          +
                                          n.
                                          getNodeName());
              }
          }
    } catch (java.io.FileNotFoundException e)        {
        Logger.getLogger("log").logln(USR.ERROR,
            "Cannot find file " + fName);
        System.exit(-1);
    }catch (SAXParseException err) {
        System.err.println("** Parsing error" + ", line "
            + err.getLineNumber() + ", uri " +
            err.getSystemId());
        Logger.getLogger("log").logln(USR.ERROR,
            " " + err.getMessage());
        System.exit(-1);
    }catch (SAXException e) {
        Logger.getLogger("log").logln(USR.ERROR,
            "Exception in SAX XML parser.");
        Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
        System.exit(-1);
    }catch (Throwable t) {
        Logger.getLogger("log").logln(USR.ERROR,
            "Caught unknown exception.");
        t.printStackTrace();
        System.exit(-1);
    }
}

/** Process tags for global controller
 */
private void processGlobalController(NodeList gc) throws
SAXException {
    if (gc.getLength() > 1) throw new SAXException(
            "Only one GlobalController tag allowed.");
    if (gc.getLength() == 0)
        return;

    Node gcn = gc.item(0);

    try {
        globalControlPort_ = ReadXMLUtils.parseSingleInt(
            gcn, "Port", "GlobalController", true);
        ReadXMLUtils.removeNode(gcn, "Port", "GlobalController");
    } catch (SAXException e) { throw e;
    } catch (XMLNoTagException e) {
    }
    try {
        isSimulation_ =
            ReadXMLUtils.parseSingleBool(gcn, "Simulation",
                "GlobalController",
                true);
        ReadXMLUtils.removeNode(gcn, "Simulation", "GlobalController");
    } catch (SAXException e) { throw e;
    } catch (XMLNoTagException e) {
    }
    try {
        allowIsolatedNodes_ = ReadXMLUtils.parseSingleBool(
            gcn, "AllowIsolatedNodes", "GlobalController", true);
        ReadXMLUtils.removeNode(gcn, "AllowIsolatedNodes",
            "GlobalController");
    } catch (SAXException e) { throw e;
    } catch (XMLNoTagException e) {
    }
    try {
        connectedNetwork_ = ReadXMLUtils.parseSingleBool(
            gcn, "ConnectedNetwork", "GlobalController", true);
        ReadXMLUtils.removeNode(gcn, "ConnectedNetwork",
            "GlobalController");
    } catch (SAXException e) { throw e;
    } catch (XMLNoTagException e) {
    }
    try {
        startLocalControllers_ = ReadXMLUtils.parseSingleBool(
            gcn,
            "StartLocalControllers",
            "GlobalController",
            true);
        ReadXMLUtils.removeNode(gcn, "StartLocalControllers",
            "GlobalController");
    } catch (SAXException e) { throw e;
    } catch (XMLNoTagException e) {
    }
    try {
        String s =
            ReadXMLUtils.parseSingleString(gcn,
                "RemoteLoginUser",
                "GlobalController",
                true);
        if (s != "")
            remoteLoginUser_ = s;
        ReadXMLUtils.removeNode(gcn, "Port", "GlobalController");
    } catch (SAXException e) { throw e;
    } catch (XMLNoTagException e) {
    }
    try {
        String s = ReadXMLUtils.parseSingleString(
            gcn,
            "RemoteStartController",
            "GlobalController",
            true);
        if (s != "")
            remoteStartController_ = s;
        ReadXMLUtils.removeNode(gcn, "RemoteStartController",
            "GlobalController");
    } catch (SAXException e) { throw e;
    } catch (XMLNoTagException e) {
    }
    try {
        int l =
            ReadXMLUtils.parseSingleInt(gcn, "LowPort",
                "GlobalController",
                true);
        lowPort_ = l;
        ReadXMLUtils.removeNode(gcn, "LowPort", "GlobalController");
    } catch (SAXException e) { throw e;
    } catch (XMLNoTagException e) {
    }
    try {
        int h =
            ReadXMLUtils.parseSingleInt(gcn, "HighPort",
                "GlobalController",
                true);
        highPort_ = h;
        ReadXMLUtils.removeNode(gcn, "HighPort", "GlobalController");
    } catch (SAXException e) { throw e;
    } catch (XMLNoTagException e) {
    }

    // What is the name of the class for Visualization
    try {
        visualizationClass = ReadXMLUtils.parseSingleString(
            gcn, "VisualizationClass", "GlobalController", true);
        ReadXMLUtils.removeNode(gcn, "VisualizationClass",
            "GlobalController");

        Logger.getLogger("log").logln(
            USR.STDOUT, "VisualizationClass = " +
            visualizationClass);

        // try and find class
        Class <? extends Visualization> visualizer = Class.forName(
            visualizationClass).asSubclass(Visualization.class );
    } catch (SAXException e) { throw new SAXException(
                                   "Unable to parse class name " +
                                   visualizationClass +
                                   " in GlobalController options" +
                                   e.getMessage());
    }catch (XMLNoTagException e) {
    } catch (ClassNotFoundException e) { throw new Error(
                                             "Class not found for class name "
                                             + visualizationClass);
    } catch (ClassCastException e) { throw new Error(
                                         "Class name " +
                                         visualizationClass +
                                         " must be sub type of Visualization");
    }

    // Check for Monitoring node
    NodeList monitoring = ((Element)gcn).getElementsByTagName(
        "Monitoring");

    // if it exists - parse subelements
    if (monitoring.getLength() != 0) {
        Element el = (Element)monitoring.item(0);

        // Should the GlobalController turn on Lattice Monitoring
        try {
            latticeMonitoring = ReadXMLUtils.parseSingleBool(
                el,
                "LatticeMonitoring",
                "GlobalController",
                true);
            ReadXMLUtils.removeNode(el, "LatticeMonitoring",
                "GlobalController");
        } catch (SAXException e) { throw e;
        } catch (XMLNoTagException e) {
        }

        // get Consumers
        try {
            // First get all nodes called 'Consumer'
            NodeList consumers = ((Element)el).getElementsByTagName(
                "Consumer");

            if (consumers.getLength() != 0) {
                for (int c = 0; c < consumers.getLength(); c++) {
                    Node elC = consumers.item(c);

                    try {
                        String className =
                            ReadXMLUtils.parseSingleString(
                                elC, "Name",
                                "Consumer",
                                true);
                        ReadXMLUtils.removeNodes(elC, "Name",
                            "Consumer");

                        //String label =
                        // ReadXMLUtils.parseSingleString(elC,
                        // "Label",
                        // "Consumer", true);
                        //ReadXMLUtils.removeNodes(elC, "Label",
                        // "Consumer");
                        String label = null;

                        //Logger.getLogger("log").logln(USR.STDOUT,
                        // "Probe: name = " + name + " datarate = "
                        // +
                        // datarate);

                        if (label == null || label == "") {
                            label = className.substring(
                                className.lastIndexOf(
                                    ".") + 1, className.length());
                        }

                        consumerInfoMap.put(label, className);
                    } catch (SAXException e) { throw e;
                    } catch (XMLNoTagException nte) {
                        Logger.getLogger("log").logln(USR.ERROR,
                            nte.getMessage());
                    }
                }

                // Remove  'Consumer' nodes
                ReadXMLUtils.removeNodes(el, "Consumer",
                    "Monitoring");
            }

            if (consumers != null) {
                Logger.getLogger("log").logln(
                    USR.STDOUT,
                    "Consumers = " + consumerInfoMap);
            }
        } catch (SAXException e) { throw e;
        }

        ReadXMLUtils.removeNode(gcn, "Monitoring", "GlobalController");
    } else {
        System.out.println("No GlobalController Monitoring node");
    }

    // Check for other unparsed tags
    NodeList nl = gcn.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
        Node n = nl.item(i);
        if (n.getNodeType() ==
            Node.ELEMENT_NODE) {throw new SAXException(
                                    "Global Controller XML unrecognised tag "
                                    +
                                    n.getNodeName());
        }
    }
    gcn.getParentNode().removeChild(gcn);
}
/**
 *  Process tags which specify local controllers
 */
private void processLocalControllers(NodeList lcs) throws
SAXException {
    String hostName;
    int port;

    for (int i = 0; i < lcs.getLength(); i++) {
        Node lc = lcs.item(i);
        LocalControllerInfo lh = null;
        try {
            hostName =
                ReadXMLUtils.parseSingleString(lc, "Name",
                    "LocalController",
                    false);
            port =
                ReadXMLUtils.parseSingleInt(lc, "Port",
                    "LocalController",
                    false);
            ReadXMLUtils.removeNode(lc, "Name", "LocalController");
            ReadXMLUtils.removeNode(lc, "Port", "LocalController");
            lh = new LocalControllerInfo(hostName, port);
            lh.setHighPort(highPort_);
            lh.setLowPort(lowPort_);
            localControllers_.add(lh);
        } catch (java.net.UnknownHostException e) { throw new
                                                          SAXException(
                                                        "Unable to recognise hostname in XML"
                                                        +
                                                        e.
                                                        getMessage());
        }catch (SAXException e) { throw e;
        } catch (XMLNoTagException e) {
            Logger.getLogger("log").logln(
                USR.ERROR,
                "Unexpected exception in processLocalControllers");
            throw new SAXException();
        }
        try {
            int mr =
                ReadXMLUtils.parseSingleInt(lc, "MaxRouters",
                    "LocalController",
                    true);
            ReadXMLUtils.removeNode(lc, "MaxRouters",
                "LocalController");
            lh.setMaxRouters(mr);
        } catch (SAXException e) { throw e;
        } catch (XMLNoTagException e) {
        }
        try {
            String s =
                ReadXMLUtils.parseSingleString(lc,
                    "RemoteLoginUser",
                    "LocalController",
                    true);
            ReadXMLUtils.removeNode(lc, "RemoteLoginUser",
                "LocalController");
            lh.setRemoteLoginUser(s);
        } catch (SAXException e) { throw e;
        } catch (XMLNoTagException e) {
        }
        try {
            String s = ReadXMLUtils.parseSingleString(
                lc,
                "RemoteStartController",
                "LocalController",
                true);
            ReadXMLUtils.removeNode(lc, "RemoteStartController",
                "LocalController");
            lh.setRemoteStartController(s);
        } catch (SAXException e) { throw e;
        } catch (XMLNoTagException e) {
        }
        try {
            int l =
                ReadXMLUtils.parseSingleInt(lc, "LowPort",
                    "LocalController",
                    true);
            ReadXMLUtils.removeNode(lc, "LowPort", "LocalController");
            lh.setLowPort(l);
        } catch (SAXException e) { throw e;
        } catch (XMLNoTagException e) {
        }
        try {
            int h =
                ReadXMLUtils.parseSingleInt(lc, "HighPort",
                    "LocalController",
                    true);
            ReadXMLUtils.removeNode(lc, "HighPort", "LocalController");
            lh.setHighPort(h);
        } catch (SAXException e) { throw e;
        } catch (XMLNoTagException e) {
        }

        NodeList nl = lc.getChildNodes();
        for (int j = 0; j < nl.getLength(); j++) {
            Node n = nl.item(j);
            if (n.getNodeType() ==
                Node.ELEMENT_NODE) {throw new SAXException(
                                        "Local Controller unrecognised tag "
                                        +
                                        n.
                                        getNodeName());
            }
        }
    }
    for (int i = lcs.getLength() - 1; i >= 0; i--) {
        Node n = lcs.item(i);
        n.getParentNode().removeChild(n);
    }
}

/**
 *  Process tags which specify Event engines */
private void processEventEngines(NodeList eng) throws SAXException {
    if (eng.getLength() == 0) throw new SAXException
              (
            "Must be at least one EventEngine tag in control file");
    while (eng.getLength() != 0)
        engines_.add(processEventEngine(eng.item(0)));
}

/** process tags for a single event engine*/
private EventEngine processEventEngine(Node n) throws SAXException {
    EventEngine eng;
    String engine = "";
    int endtime = 0;
    String parms = "";

    try {
        engine =
            ReadXMLUtils.parseSingleString(n,
                "Name",
                "EventEngine",
                false);
        ReadXMLUtils.removeNode(n, "Name", "EventEngine");
        endtime =
            ReadXMLUtils.parseSingleInt(n,
                "EndTime",
                "EventEngine",
                false);
        ReadXMLUtils.removeNode(n, "EndTime", "EventEngine");
        parms =
            ReadXMLUtils.parseSingleString(n, "Parameters",
                "EventEngine",
                true);
        ReadXMLUtils.removeNode(n, "Parameters", "EventEngine");
    } catch (SAXException e) { throw e;
    } catch (XMLNoTagException e) {
    }

    NodeList nl = n.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
        Node n0 = nl.item(i);
        if (n0.getNodeType() ==
            Node.ELEMENT_NODE) {throw new SAXException(
                                    "Event Engine unrecognised XML tag "
                                    +
                                    n0.getNodeName());
        }
    }
    n.getParentNode().removeChild(n);

    //Legacy for old scripts
    try {
        if (engine.equals("Empty")) {
            eng = new EmptyEventEngine(endtime, parms);
            return eng;
        }
        if (engine.equals("Test")) {
            eng = new TestEventEngine(endtime, parms);
            return eng;
        }
        if (engine.equals("Probabilistic")) {
            eng = new ProbabilisticEventEngine(endtime, parms);
            return eng;
        }
        if (engine.equals("Script")) {
            eng = new ScriptEngine(endtime, parms);
            return eng;
        }
    } catch (EventEngineException e) { throw new SAXException(
                                           "Cannot construct event engine "
                                           + engine + " " +
                                           e.getMessage());
    }
    Class <?> engClass = null;
    try {
        engClass = Class.forName(engine);
        boolean impEng = false;
        for (Class e : engClass.getInterfaces()) {
            if (e.equals(Class.forName("usr.engine.EventEngine"))) {
                impEng = true;
                break;
            }
        }
        if (impEng == false) throw new Exception(
                "Class name not instance of EventEngine");
    } catch (Exception e) { throw new SAXException(
                                "Could not find engine type " +
                                engine);
    }
    try {
        Class[] args = new Class[2];
        args[0] = int.class;
        args[1] = String.class;
        Constructor <?> c = engClass.getConstructor(args);
        Object[] arglist = new Object[2];
        arglist[0] = endtime;
        arglist[1] = parms;
        eng = (EventEngine)c.newInstance(arglist);
        return eng;
    } catch (InvocationTargetException e) {
        Throwable t = e.getTargetException(); throw new SAXException(
            "Could not construct engine " + engine +
            "\n Error message:" +
            t.getMessage());
    } catch (Exception e) { throw new SAXException(
                                "Could not construct engine " +
                                engine +
                                "\n Error message:" +
                                e.getMessage());
    }
}

private void processRouterOptions(NodeList n) throws SAXException,
IOException,
javax.xml.parsers.ParserConfigurationException {
    if (n.getLength() == 0)
        return;
    if (n.getLength() > 1) throw new SAXException(
            "Cannot have more than one RouterOptions section");
    Element hElement = (Element)n.item(0);
    NodeList textFNList = hElement.getChildNodes();
    String fName = ((Node)textFNList.item(0)).getNodeValue().trim();
    //Logger.getLogger("log").logln(USR.ERROR, "Read router file
    // "+fName);
    BufferedReader reader;
    try {
        reader = new BufferedReader(new FileReader(fName));
    } catch (FileNotFoundException e) {
        Logger.getLogger("log").logln(USR.ERROR,
            "Cannot find router file " + fName); throw new SAXException();
    }
    String line = null;
    StringBuilder stringBuilder = new StringBuilder();

    String ls = System.getProperty("line.separator");
    while ((line = reader.readLine()) != null) {
        stringBuilder.append(line);
        stringBuilder.append(" ");
    }
    routerOptionsString_ = stringBuilder.toString();
    //Logger.getLogger("log").logln(USR.ERROR, "User Options String
    // "+routerOptionsString_);
    routerOptions_.setOptionsFromString(routerOptionsString_);

    Node n0 = n.item(0);
    NodeList nl = n0.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
        Node n1 = nl.item(i);
        if (n1.getNodeType() ==
            Node.ELEMENT_NODE) throw new SAXException(
                "Unrecognised tag "
                + n1.getNodeName());
    }
    n0.getParentNode().removeChild(n0);
}

/**
 * Process all router outputs
 * There maybe multiple Output blocks.
 */
private void processRouterOutputs(NodeList o) throws SAXException {
    for (int i = o.getLength() - 1; i >= 0; i--) {
        Node oNode = o.item(i);
        outputs_.add(processOutput(oNode));
        oNode.getParentNode().removeChild(oNode);
    }
}

/** Process tags related to a particular type of output */
private OutputType processOutput(Node n) throws SAXException {
    OutputType ot = new OutputType();

    try {
        String fName =
            ReadXMLUtils.parseSingleString(n, "File", "Output",
                false);
        ReadXMLUtils.removeNode(n, "File", "Output");
        ot.setFileName(fName);
        String when =
            ReadXMLUtils.parseSingleString(n, "When", "Output",
                false);
        ReadXMLUtils.removeNode(n, "When", "Output");
        ot.setTimeType(when);
        String type =
            ReadXMLUtils.parseSingleString(n, "Type", "Output",
                false);
        ReadXMLUtils.removeNode(n, "Type", "Output");
        ot.setType(type);
        String parm =
            ReadXMLUtils.parseSingleString(n,
                "Parameter",
                "Output",
                true);
        ReadXMLUtils.removeNode(n, "Parameter", "Output");
        ot.setParameter(parm);
    } catch (NumberFormatException e) { throw new SAXException(
                                            "Cannot parse integer in Output Tag "
                                            + e.getMessage());
    } catch (java.lang.IllegalArgumentException e) { throw new
                                                           SAXException(
                                                         "Cannot parse tag "
                                                         + e.
                                                         getMessage());
    }  catch (SAXException e) {  throw e;
    } catch (XMLNoTagException e) {
    }
    try {
        int time = ReadXMLUtils.parseSingleInt(n, "Time", "Output",
            true);
        ReadXMLUtils.removeNode(n, "Time", "Output");
        ot.setTime(time);
    } catch (NumberFormatException e) { throw new SAXException(
                                            "Cannot parse integer in Output Tag "
                                            + e.getMessage());
    } catch (java.lang.IllegalArgumentException e) { throw new
                                                           SAXException(
                                                         "Cannot parse tag "
                                                         + e.
                                                         getMessage());
    }  catch (SAXException e) {  throw e;
    } catch (XMLNoTagException e) {
    }
    return ot;
}

/** Return string to launch local controller on remote
 * machine given machine name
 */
public String [] localControllerStartCommand(LocalControllerInfo lh){
    if (lh.getName().equals("localhost")) {
        // no need to do remote command
        String [] cmd = new String[3];
        cmd[0] = "/usr/bin/java";
        cmd[1] = "usr.localcontroller.LocalController";
        cmd[2] = String.valueOf(lh.getPort());
        return cmd;
    } else {
        // its a remote command
        String [] cmd = new String[5];
        cmd[0] = remoteLoginCommand_;
        cmd[1] = remoteLoginFlags_;
        // For user name in turn try info from remote, or info
        // from global or fall back to no username
        String user = lh.getRemoteLoginUser();
        if (user == null)
            user = remoteLoginUser_;
        if (user == null)
            cmd[2] = lh.getName();
        else
            cmd[2] = user + "@" + lh.getName();
        String remote = lh.getRemoteStartController();
        if (remote == null)
            remote = remoteStartController_;
        cmd[3] = remote;
        cmd[4] = String.valueOf(lh.getPort());
        return cmd;
    }
}

/** Accessor function returns the number of controllers
 */
public int noControllers(){
    return localControllers_.size();
}

/** Accessor function returns the i th controller
 */
public LocalControllerInfo getController(int i){
    return localControllers_.get(i);
}

public Iterator getControllersIterator(){
    return localControllers_.iterator();
}

/** Should global controller attempt to remotely start local
 * controllers using ssh or assume it has been done.
 */
public boolean startLocalControllers(){
    return startLocalControllers_;
}

/** Are we simulating nodes or executing them with virtual
 * routers
 */
public boolean isSimulation(){
    return isSimulation_;
}

/** Do we allow isolated nodes in simulation */
public boolean allowIsolatedNodes(){
    return allowIsolatedNodes_;
}

/** Do we force the network to be connected */
public boolean connectedNetwork(){
    return connectedNetwork_;
}

/**
 * Get the class name for Visualization
 */
public String getVisualizationClassName(){
    return visualizationClass;
}

/**
 * Should we turn on Lattice Monitoring
 */
public boolean latticeMonitoring(){
    return latticeMonitoring;
}

/**
 * Get the map of class names and labels for consumers.
 */
public HashMap<String, String>getConsumerInfo(){
    return consumerInfoMap;
}

/** Return port number for global controller
 */
public int getGlobalPort(){
    return globalControlPort_;
}

/** Accessor function -- number of times to try to start local
 * controller*/
public int getControllerWaitTime(){
    return controllerWaitTime_;
}

/** Accessor function for outputs requested from simulation*/
ArrayList <OutputType> getOutputs(){
    return outputs_;
}

/** Initialise event list */
public void initialEvents(EventScheduler s,
    GlobalController g)                        {
    engines_.get(0).startStopEvents(s, g);
    for (EventEngine eng : engines_)
        eng.initialEvents(s, g);

    if (routerOptions_.getControllerConsiderTime() > 0) {
        QueryAPEvent ae = new QueryAPEvent(
            routerOptions_.getControllerConsiderTime(),
            null, g.getAPController());
        s.addEvent(ae);
    }

    for (OutputType o : outputs_) {
        if (o.getTimeType() == OutputType.AT_TIME ||
            o.getTimeType() ==
            OutputType.AT_INTERVAL) {
            OutputEvent oe = new OutputEvent(o.getTime(), null, o);
            s.addEvent(oe);
        }
    }
}

public String getRouterOptionsString(){
    return routerOptionsString_;
}

/** Accessor function for max lag -- maximum time delay for simulation
 */
public int getMaxLag(){
    return maxLag_;
}
}