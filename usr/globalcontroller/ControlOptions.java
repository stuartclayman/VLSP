/** This class contains the options used for a simulation.  It reads in an
 * XML file or string to generate them
 * The options specify hosts and controls used in simulation
 */
package usr.globalcontroller;

import usr.localcontroller.LocalControllerInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import usr.engine.*;
import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 
import usr.engine.*;
import usr.common.*;


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
    private int maxLag_= 10000;
    EventEngine engine_;


    /** init function sets up basic information */
    public void init () {
      
      localControllers_= new ArrayList<LocalControllerInfo>();
      remoteLoginCommand_ = "/usr/bin/ssh";
      remoteLoginFlags_ = "-n";
      remoteStartController_ = 
        "java -cp $(HOME)/code/userspacerouter usr.localcontroller.LocalController";

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
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
           startLocalControllers_= ReadXMLUtils.parseSingleBool(gcn, "StartLocalControllers", "GlobalController",true);
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {  
        }
        try {
            String s= ReadXMLUtils.parseSingleString(gcn, "RemoteLoginUser","GlobalController",true);
            if (s != "")
                remoteLoginUser_= s;
        } catch (SAXException e) {
                throw e;
        } catch (XMLNoTagException e) {
        }
        try {
            String s= ReadXMLUtils.parseSingleString(gcn, "RemoteStartController","GlobalController",true);
            if (s != "") 
               remoteStartController_= s;
        } catch (SAXException e) {
             throw e;
        } catch (XMLNoTagException e) {
        }
        try {
            int l= ReadXMLUtils.parseSingleInt(gcn, "LowPort","GlobalController",true);
            lowPort_= l;
        } catch (SAXException e) {
             throw e;
        } catch (XMLNoTagException e) {
        }
        try {
            int h= ReadXMLUtils.parseSingleInt(gcn, "HighPort","GlobalController",true);
            highPort_= h;
        } catch (SAXException e) {
             throw e;
        } catch (XMLNoTagException e) {
        }
        
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
                lh.setMaxRouters(mr);
            } catch (SAXException e) {
                throw e;
            } catch (XMLNoTagException e) {
            }
            try {
                String s= ReadXMLUtils.parseSingleString(lc, "RemoteLoginUser","LocalController",true);
                lh.setRemoteLoginUser(s);
            } catch (SAXException e) {
                throw e;
            } catch (XMLNoTagException e) {
            }
            try {
                String s= ReadXMLUtils.parseSingleString(lc, "RemoteStartController","LocalController",true);
                lh.setRemoteStartController(s);
            } catch (SAXException e) {
                throw e;
            } catch (XMLNoTagException e) {
            }
            try {
                int l= ReadXMLUtils.parseSingleInt(lc, "LowPort","GlobalController",true);
                lh.setLowPort(l);
            } catch (SAXException e) {
               throw e;
            } catch (XMLNoTagException e) {
            }
            try {
                int h= ReadXMLUtils.parseSingleInt(lc, "HighPort","GlobalController",true);
                lh.setHighPort(h);
            } catch (SAXException e) {
                 throw e;
            } catch (XMLNoTagException e) {
            }
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
          endtime= ReadXMLUtils.parseSingleInt(n,"EndTime","EventEngine",false);
          parms= ReadXMLUtils.parseSingleString(n,"Parameters","EventEngine",true);
      } catch (SAXException e) {
          throw e;
      } catch (XMLNoTagException e) {
      }
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
    
    /** Accessor function for max lag -- maximum time delay for simulation */
    public int getMaxLag() {
        return maxLag_;
    }
}


