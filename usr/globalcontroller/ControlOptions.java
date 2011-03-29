/** This class contains the options used for a simulation.  It reads in an
 * XML file or string to generate them
 * The options specify hosts and controls used in simulation
 */
package usr.globalcontroller;

import usr.logging.*;
import usr.localcontroller.LocalControllerInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.io.*;
import usr.engine.*;
import org.w3c.dom.Document;
import org.w3c.dom.*;
import usr.output.OutputType;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 
import usr.engine.*;
import usr.common.*;
import usr.router.RouterOptions;


public class ControlOptions {
    private ArrayList<LocalControllerInfo> localControllers_;
    private int globalControlPort_ = 8888;  // Port global controller listens on
    private String remoteLoginCommand_= null;  // Command used to login to start local controller
    private String remoteStartController_= null;  // Command used on local controller to start it
    private String remoteLoginFlags_ = null;   //  Flags used for ssh to login to remote machine
    private String remoteLoginUser_= null;     // User on remote machines to login with.
    private boolean startLocalControllers_= true;   // If true Global Controller starts local controllers
    private boolean isSimulation_= false;    //  If true simulation in software not emulation in hardware
    private boolean allowIsolatedNodes_= true;   // If true, check for isolated nodes
    private boolean connectedNetwork_= false;  // If true, keep network connected
    private boolean latticeMonitoring = false;  // If true, turn on Lattice Monitoring


    private int controllerWaitTime_= 6;    
    private int lowPort_= 10000;   // Default lowest port to be used on local controller 
    private int highPort_= 20000;  // Default highest port to be used on local controller
    private int maxLag_= 1000000;  // Maximum lag tolerable in simulation in millisec
    private String routerOptionsString_= ""; //
    private RouterOptions routerOptions_= null;
    EventEngine engine_;   // Engine used to create new events for sim

    private ArrayList <OutputType> outputs_= null;

    /** init function sets up basic information */
    public void init () {
      
      localControllers_= new ArrayList<LocalControllerInfo>();
      outputs_= new ArrayList <OutputType>();
      remoteLoginCommand_ = "/usr/bin/ssh";
      remoteLoginFlags_ = "-n";
      Properties prop = System.getProperties();
      
      remoteStartController_ = 
        "java -cp "+prop.getProperty("java.class.path", null)+" usr.localcontroller.LocalController";
      routerOptions_= new RouterOptions(null);
    }
    
    /** Adds information about a new host to the list
    */
    private void addNewHost(LocalControllerInfo host) {
      localControllers_.add(host);
    }


    /** Accessor function for router Options */
    
    public RouterOptions getRouterOptions() 
    {
        return routerOptions_;
    }

    /** Read control options from XML file 
    */
    public ControlOptions (String fName) {
      init();
      readXML(fName); 
    }
    
    void readXML(String fName) {
      try { DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse (new File(fName));

            // normalize text representation
            doc.getDocumentElement ().normalize ();
            String basenode= doc.getDocumentElement().getNodeName();
            if (!basenode.equals("SimOptions")) {
                throw new SAXException("Base tag should be SimOptions");
            }
            NodeList lcs= doc.getElementsByTagName("LocalController");
            processLocalControllers(lcs);
            NodeList gcs= doc.getElementsByTagName("GlobalController");
            processGlobalController(gcs);
            NodeList eng= doc.getElementsByTagName("EventEngine");
            processEventEngine(eng);
            NodeList ro= doc.getElementsByTagName("RouterOptions");
            processRouterOptions(ro);
            NodeList o= doc.getElementsByTagName("Output");

            for (int i= o.getLength()-1; i>= 0; i--) {
                Node oNode= o.item(i);
                outputs_.add(processOutput(oNode));
                oNode.getParentNode().removeChild(oNode);
            }

            
            // Check all tags are processed
            // Check for other unparsed tags
            Element el= doc.getDocumentElement();
            NodeList rest= el.getChildNodes();
            for (int i= 0; i < rest.getLength(); i++) { 
                Node n= rest.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                     throw new SAXException("Final tidy unrecognised tag "+n.getNodeName());
                }
             
            }

      } catch (java.io.FileNotFoundException e) {
          Logger.getLogger("log").logln(USR.ERROR, "Cannot find file "+fName);
          System.exit(-1);
      }catch (SAXParseException err) {
          System.err.println ("** Parsing error" + ", line " 
             + err.getLineNumber () + ", uri " + err.getSystemId ());
          Logger.getLogger("log").logln(USR.ERROR, " " + err.getMessage ());
          System.exit(-1);

      }catch (SAXException e) {
          Logger.getLogger("log").logln(USR.ERROR, "Exception in SAX XML parser.");
          Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
          System.exit(-1);
          
      }catch (Throwable t) {
          
          Logger.getLogger("log").logln(USR.ERROR, "Caught unknown exception.");
          t.printStackTrace ();
          System.exit(-1);
      }
     
    }
    
    
    /** Process tags for global controller
    */
    
    private void processGlobalController(NodeList gc) throws SAXException {
        if (gc.getLength() > 1) {
            throw new SAXException ("Only one GlobalController tag allowed.");
        }
        if (gc.getLength() == 0) 
            return;
        Node gcn= gc.item(0);
       
        try {
           globalControlPort_= ReadXMLUtils.parseSingleInt(gcn, "Port","GlobalController",true);
           ReadXMLUtils.removeNode(gcn,"Port","GlobalController");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
         try {
           isSimulation_= ReadXMLUtils.parseSingleBool(gcn, "Simulation","GlobalController",true);
           ReadXMLUtils.removeNode(gcn,"Simulation","GlobalController");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
           allowIsolatedNodes_= ReadXMLUtils.parseSingleBool(gcn, "AllowIsolatedNodes","GlobalController",true);
           ReadXMLUtils.removeNode(gcn,"AllowIsolatedNodes","GlobalController");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
           connectedNetwork_= ReadXMLUtils.parseSingleBool(gcn, "ConnectedNetwork","GlobalController",true);
           ReadXMLUtils.removeNode(gcn,"ConnectedNetwork","GlobalController");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
           startLocalControllers_= ReadXMLUtils.parseSingleBool(gcn, "StartLocalControllers", "GlobalController",true);
           ReadXMLUtils.removeNode(gcn,"StartLocalControllers","GlobalController");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {  
        }
        try {
            String s= ReadXMLUtils.parseSingleString(gcn, "RemoteLoginUser","GlobalController",true);
            if (s != "")
                remoteLoginUser_= s;
            ReadXMLUtils.removeNode(gcn,"Port","GlobalController");
        } catch (SAXException e) {
                throw e;
        } catch (XMLNoTagException e) {
        }
        try {
            String s= ReadXMLUtils.parseSingleString(gcn, "RemoteStartController","GlobalController",true);
            if (s != "") 
               remoteStartController_= s;
            ReadXMLUtils.removeNode(gcn,"RemoteStartController","GlobalController");
        } catch (SAXException e) {
             throw e;
        } catch (XMLNoTagException e) {
        }
        try {
            int l= ReadXMLUtils.parseSingleInt(gcn, "LowPort","GlobalController",true);
            lowPort_= l;
            ReadXMLUtils.removeNode(gcn,"LowPort","GlobalController");
        } catch (SAXException e) {
             throw e;
        } catch (XMLNoTagException e) {
        }
        try {
            int h= ReadXMLUtils.parseSingleInt(gcn, "HighPort","GlobalController",true);
            highPort_= h;
            ReadXMLUtils.removeNode(gcn,"HighPort","GlobalController");
        } catch (SAXException e) {
             throw e;
        } catch (XMLNoTagException e) {
        }
        
         try {
           latticeMonitoring = ReadXMLUtils.parseSingleBool(gcn, "LatticeMonitoring","GlobalController",true);
           ReadXMLUtils.removeNode(gcn,"LatticeMonitoring","GlobalController");
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }

        NodeList nl= gcn.getChildNodes();
        for (int i= 0; i < nl.getLength(); i++) {         
            Node n= nl.item(i); 
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                throw new SAXException("Global Controller XML unrecognised tag "+n.getNodeName());
            }
                
        } 
        gcn.getParentNode().removeChild(gcn);
        
    }
    /**
        Process tags which specify local controllers
    */
    private void processLocalControllers(NodeList lcs) throws SAXException {
        
        String hostName;
        int port;
        for (int i= 0; i < lcs.getLength(); i++) {
            Node lc= lcs.item(i);
            LocalControllerInfo lh= null;
            try {
                hostName= ReadXMLUtils.parseSingleString(lc, "Name", "LocalController",false);
                port= ReadXMLUtils.parseSingleInt(lc, "Port","LocalController",false);
                ReadXMLUtils.removeNode(lc,"Name","LocalController");
                ReadXMLUtils.removeNode(lc,"Port","LocalController");
                lh= new LocalControllerInfo(hostName, port);
                lh.setHighPort(highPort_);
                lh.setLowPort(lowPort_);
                localControllers_.add(lh);
            } catch (java.net.UnknownHostException e) {
                throw new SAXException ("Unable to recognise hostname in XML"+e.getMessage());
                 
            }catch (SAXException e) {
                throw e;
            } catch (XMLNoTagException e) {
                Logger.getLogger("log").logln(USR.ERROR, "Unexpected exception in processLocalControllers");
                throw new SAXException();
            }
            try {
                int mr= ReadXMLUtils.parseSingleInt(lc, "MaxRouters","LocalController",true);
                ReadXMLUtils.removeNode(lc,"MaxRouters","LocalController");
                lh.setMaxRouters(mr);
            } catch (SAXException e) {
                throw e;
            } catch (XMLNoTagException e) {
            }
            try {
                String s= ReadXMLUtils.parseSingleString(lc, "RemoteLoginUser","LocalController",true);
                ReadXMLUtils.removeNode(lc,"RemoteLoginUser","LocalController");
                lh.setRemoteLoginUser(s);
            } catch (SAXException e) {
                throw e;
            } catch (XMLNoTagException e) {
            }
            try {
                String s= ReadXMLUtils.parseSingleString(lc, "RemoteStartController","LocalController",true);
                ReadXMLUtils.removeNode(lc,"RemoteStartController","LocalController");
                lh.setRemoteStartController(s);
            } catch (SAXException e) {
                throw e;
            } catch (XMLNoTagException e) {
            }
            try {
                int l= ReadXMLUtils.parseSingleInt(lc, "LowPort","LocalController",true);
                ReadXMLUtils.removeNode(lc,"LowPort","LocalController");
                lh.setLowPort(l);
            } catch (SAXException e) {
               throw e;
            } catch (XMLNoTagException e) {
            }
            try {
                int h= ReadXMLUtils.parseSingleInt(lc, "HighPort","LocalController",true);
                ReadXMLUtils.removeNode(lc,"HighPort","LocalController");
                lh.setHighPort(h);
            } catch (SAXException e) {
                 throw e;
            } catch (XMLNoTagException e) {
            }
             NodeList nl= lc.getChildNodes();
             for (int j= 0; j < nl.getLength();j++) {         
                  Node n= nl.item(j); 
                  if (n.getNodeType() == Node.ELEMENT_NODE) {
                     throw new SAXException("Local Controller unrecognised tag "+n.getNodeName());
                 
                   }
                
             } 
        
        }
        for (int i= lcs.getLength()-1; i >= 0; i--) {
            Node n= lcs.item(i);
            n.getParentNode().removeChild(n);
        }
    }


    /**
        Process tags which specify local controllers
    */
    private void processEventEngine(NodeList eng) throws SAXException {
      if (eng.getLength() != 1) {
          throw new SAXException
            ("Must be exactly one EventEngine tag in control file");
      }
      Node n= eng.item(0);
      String engine="";
      int endtime= 0;
      String parms="";
      
      try {
          engine= ReadXMLUtils.parseSingleString(n,"Name","EventEngine",false);
          ReadXMLUtils.removeNode(n,"Name","EventEngine");
          endtime= ReadXMLUtils.parseSingleInt(n,"EndTime","EventEngine",false);
          ReadXMLUtils.removeNode(n,"EndTime","EventEngine");
          parms= ReadXMLUtils.parseSingleString(n,"Parameters","EventEngine",true);
          ReadXMLUtils.removeNode(n,"Parameters","EventEngine");
      } catch (SAXException e) {
          throw e;
      } catch (XMLNoTagException e) {
      }
      
      NodeList nl= n.getChildNodes();
        for (int i= 0; i < nl.getLength(); i++) {         
            Node n0= nl.item(i); 
            if (n0.getNodeType() == Node.ELEMENT_NODE) {
                throw new SAXException("Event Engine unrecognised XML tag "+n0.getNodeName());
            }
            
                
        } 
        n.getParentNode().removeChild(n);
      if (engine.equals("Empty")) {
          engine_= new EmptyEventEngine(endtime,parms);
          return;
      }
      if (engine.equals("Test")) {
          engine_= new TestEventEngine(endtime,parms);
          return;
      }
      if (engine.equals("Probabilistic")) {
          engine_= new ProbabilisticEventEngine(endtime,parms);
          return;
      }
      if (engine.equals("Script")) {
          engine_= new ScriptEngine(endtime,parms);
          return;
      }
      throw new SAXException("Could not find engine type "+engine);
      
    
    }
    
    private void processRouterOptions(NodeList n) throws SAXException, 
       IOException, 
      javax.xml.parsers.ParserConfigurationException {
        if (n.getLength() == 0) {
            return;
        }
        if (n.getLength() > 1) {
            throw new SAXException ("Cannot have more than one RouterOptions section");
        }
        Element hElement = (Element)n.item(0);
        NodeList textFNList = hElement.getChildNodes();
        String fName= ((Node)textFNList.item(0)).getNodeValue().trim();
        //Logger.getLogger("log").logln(USR.ERROR, "Read router file "+fName); 
         BufferedReader reader;
        try {   
             reader = new BufferedReader( new FileReader (fName));
        } catch (FileNotFoundException e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot find router file "+fName); 
            throw new SAXException();
        }
        String line  = null;
        StringBuilder stringBuilder = new StringBuilder();
          
        String ls = System.getProperty("line.separator");
        while( ( line = reader.readLine() ) != null ) {
           stringBuilder.append( line );
           stringBuilder.append( " ");
        }
        routerOptionsString_= stringBuilder.toString();
        //Logger.getLogger("log").logln(USR.ERROR, "User Options String "+routerOptionsString_);
        routerOptions_.setOptionsFromString(routerOptionsString_);
        
        Node n0=n.item(0);
        NodeList nl= n0.getChildNodes();
        for (int i= 0; i < nl.getLength(); i++) {         
            Node n1= nl.item(i); 
            if (n1.getNodeType() == Node.ELEMENT_NODE) {
                throw new SAXException("Unrecognised tag "+n1.getNodeName());
            }
                
        } 
        n0.getParentNode().removeChild(n0);
    }

    /** Process tags related to a particular type of output */
    private OutputType processOutput(Node n) throws SAXException
    {
        OutputType ot= new OutputType();
        
        try {
          String fName= ReadXMLUtils.parseSingleString(n,"File","Output",false);
          ReadXMLUtils.removeNode(n,"File","Output");
          ot.setFileName(fName);
          String when= ReadXMLUtils.parseSingleString(n,"When","Output",false);
          ReadXMLUtils.removeNode(n,"When","Output");
          ot.setTimeType(when);
          String type= ReadXMLUtils.parseSingleString(n,"Type","Output",false);
          ReadXMLUtils.removeNode(n,"Type","Output");
          ot.setType(type);
          String parm= ReadXMLUtils.parseSingleString(n,"Parameter","Output",true);
          ReadXMLUtils.removeNode(n,"Parameter","Output");
          ot.setParameter(parm);
      } catch (NumberFormatException e) {
          throw new SAXException ("Cannot parse integer in Output Tag "+e.getMessage());
      } catch (java.lang.IllegalArgumentException e) {
          throw new SAXException ("Cannot parse tag "+e.getMessage());
      }  catch (SAXException e) {
          throw e;
      } catch (XMLNoTagException e) {
      }
      try {
          int time= ReadXMLUtils.parseSingleInt(n,"Time","Output",true);
          ReadXMLUtils.removeNode(n,"Time","Output");
          ot.setTime(time);
      } catch (NumberFormatException e) {
          throw new SAXException ("Cannot parse integer in Output Tag "+e.getMessage());
      } catch (java.lang.IllegalArgumentException e) {
          throw new SAXException ("Cannot parse tag "+e.getMessage());
      }  catch (SAXException e) {
          throw e;
      } catch (XMLNoTagException e) {
      }
        return ot;
    }
    
    /** Return string to launch local controller on remote
    machine given machine name 
    */
    public String [] localControllerStartCommand(LocalControllerInfo lh) {
        if (lh.getName().equals("localhost")) {
            // no need to do remote command
            String [] cmd= new String[3];
            cmd[0] = "/usr/bin/java";
            cmd[1] = "usr.localcontroller.LocalController";
            cmd[2] = String.valueOf(lh.getPort());
            return cmd;

        } else {
            // its a remote command
            String [] cmd= new String[5];
            cmd[0] = remoteLoginCommand_;
            cmd[1] = remoteLoginFlags_;
            // For user name in turn try info from remote, or info
            // from global or fall back to no username
            String user= lh.getRemoteLoginUser();  
            if (user == null)
                user= remoteLoginUser_;
            if (user == null) {
                cmd[2]=lh.getName();
            } else {
                cmd[2] = user+"@"+lh.getName();
            }
            String remote= lh.getRemoteStartController();
            if (remote == null) {
                remote= remoteStartController_;
            }
            cmd[3] = remote;
            cmd[4] = String.valueOf(lh.getPort());
            return cmd;
        }
    }

    

    /** Accessor function returns the number of controllers 
    */
    public int noControllers() {
      return localControllers_.size();
    }
    
    /** Accessor function returns the i th controller 
    */
    public LocalControllerInfo getController(int i) {
      return localControllers_.get(i);
    }
    
    public Iterator getControllersIterator() {
        return localControllers_.iterator();
    }
 
    /** Should global controller attempt to remotely start local
      controllers using ssh or assume it has been done.
    */
    public boolean startLocalControllers() {
        return startLocalControllers_;
    }
    
    /** Are we simulating nodes or executing them with virtual
    routers 
    */
    public boolean isSimulation() {
        return isSimulation_;
    }
    
    /** Do we allow isolated nodes in simulation */
    public boolean allowIsolatedNodes() {
        return allowIsolatedNodes_;
    }
    
    /** Do we force the network to be connected */
    public boolean connectedNetwork() {
        return connectedNetwork_;
    }
    
    /** 
     * Should we turn on Lattice Monitoring
     */
    public boolean latticeMonitoring() {
        return latticeMonitoring;
    }
    
    /** Return port number for global controller 
    */
    public int getGlobalPort() {
      return globalControlPort_;
    }  
    
    /** Accessor function -- number of times to try to start local controller*/
    public int getControllerWaitTime() {
        return controllerWaitTime_;
    } 
   
    /** Accessor function for outputs requested from simulation*/
    ArrayList <OutputType> getOutputs() {
        return outputs_;
    }
    
    /** Initialise event list */
    public void initialEvents(EventScheduler s, GlobalController g)
    {
        engine_.initialEvents(s,g);
        SimEvent e= new SimEvent(SimEvent.EVENT_AP_CONTROLLER, 
            routerOptions_.getControllerConsiderTime(),null);
        s.addEvent(e);
        for (OutputType o: outputs_) {
            if (o.getTimeType() == OutputType.AT_TIME || o.getTimeType() == 
              OutputType.AT_INTERVAL) {
                e= new SimEvent(SimEvent.EVENT_OUTPUT,o.getTime(), o);
                s.addEvent(e); 
            }
        }
    }
    
    /** Add or remove events following a simulation event */
    public void preceedEvent(SimEvent e, EventScheduler s, GlobalController g)
     {
        engine_.preceedEvent(e,s,g);
     }
    /** Add or remove events following a simulation event -- object allows
    global controller to pass extra parameters related to event if necessary*/
    public void followEvent(SimEvent e, EventScheduler s, GlobalController g,
      Object o)
    {
        engine_.followEvent(e,s,g,o);
        long time= e.getTime();
        if (connectedNetwork_) {
            if (e.getType() == SimEvent.EVENT_END_ROUTER) {
                g.connectNetwork(time);
            } else if (e.getType() == SimEvent.EVENT_END_LINK) {
                int router1= 0, router2= 0;
                Pair<?,?> pair= (Pair<?,?>)e.getData();
                router1= (Integer)pair.getFirst();
                router2= (Integer)pair.getSecond();
                g.connectNetwork(time,router1, router2);
            }
        } else if (!allowIsolatedNodes_) {
            if (e.getType() == SimEvent.EVENT_END_ROUTER) {
                g.checkIsolated(time);
            } else if (e.getType() == SimEvent.EVENT_END_LINK) {
                int router1= 0, router2= 0;
                Pair<?,?> pair= (Pair<?,?>)e.getData();
                router1= (Integer)pair.getFirst();
                router2= (Integer)pair.getSecond();
                g.checkIsolated(time,router1);
                g.checkIsolated(time,router2);
            }
        }
    }
    
    public String getRouterOptionsString() {
        return routerOptionsString_; 
    }
    
    /** Accessor function for max lag -- maximum time delay for simulation */
    public int getMaxLag() {
        return maxLag_;
    }
}


