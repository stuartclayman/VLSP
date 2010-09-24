/** This class contains the options used for a simulation.  It reads in an
 * XML file or string to generate them
 * The options specify hosts and controls used in simulation
 */
package usr.globalcontroller;

import usr.localcontroller.LocalControllerInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;
import usr.engine.*;
import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 
import usr.engine.*;
import usr.common.*;
import usr.router.RouterOptions;


class ControlOptions {
    private ArrayList<LocalControllerInfo> localControllers_;
    private int globalControlPort_ = 8888;  // Port global controller listens on
    private String remoteLoginCommand_= null;  // Command used to login to start local controller
    private String remoteStartController_= null;  // Command used on local controller to start it
    private String remoteLoginFlags_ = null;   //  Flags used for ssh to login to remote machine
    private String remoteLoginUser_= null;     // User on remote machines to login with.
    private boolean startLocalControllers_= true;   // If true Global Controller starts local controllers
    private boolean isSimulation_= false;    //  If true simulation in software not emulation in hardware
    private int controllerWaitTime_= 6;    
    private int lowPort_= 10000;   // Default lowest port to be used on local controller 
    private int highPort_= 20000;  // Default highest port to be used on local controller
    private int maxLag_= 10000;  // Maximum lag tolerable in simulation in millisec
    private String routerOptionsString_= ""; //
    private RouterOptions routerOptions_= null;
    EventEngine engine_;   // Engine used to create new events for sim


    /** init function sets up basic information */
    public void init () {
      
      localControllers_= new ArrayList<LocalControllerInfo>();
      remoteLoginCommand_ = "/usr/bin/ssh";
      remoteLoginFlags_ = "-n";
      remoteStartController_ = 
        "java -cp $(HOME)/code/userspacerouter usr.localcontroller.LocalController";
      routerOptions_= new RouterOptions(null);
    }
    
    /** Adds information about a new host to the list
    */
    private void addNewHost(LocalControllerInfo host) {
      localControllers_.add(host);
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
            } catch (SAXException e) {
                throw e;
            } catch (XMLNoTagException e) {
                System.err.println("Unexpected exception in processLocalControllers");
                System.exit(-1);
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
      FileNotFoundException, IOException, 
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
        //System.err.println("Read router file "+fName);    
        BufferedReader reader = new BufferedReader( new FileReader (fName));
        String line  = null;
        StringBuilder stringBuilder = new StringBuilder();
          
        String ls = System.getProperty("line.separator");
        while( ( line = reader.readLine() ) != null ) {
           stringBuilder.append( line );
           stringBuilder.append( ls );
        }
        routerOptionsString_= stringBuilder.toString();
        //System.err.println("User Options String "+routerOptionsString_);
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
    
    /** Return port number for global controller 
    */
    public int getGlobalPort() {
      return globalControlPort_;
    }  
    
    public int getControllerWaitTime() {
        return controllerWaitTime_;
    } 
   
    
    /** Initialise event list */
    public void initialEvents(EventScheduler s, GlobalController g)
    {
        engine_.initialEvents(s,g);
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
    }
    
    public String getRouterOptionsString() {
        return routerOptionsString_; 
    }
    
    /** Accessor function for max lag -- maximum time delay for simulation */
    public int getMaxLag() {
        return maxLag_;
    }
}


