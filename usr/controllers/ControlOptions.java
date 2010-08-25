/** This class contains the options used for a simulation.  It reads in an
 * XML file or string to generate them
 * The options specify hosts and controls used in simulation
 */
package usr.controllers;
import usr.common.LocalHostInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 


class ControlOptions {
    private ArrayList<LocalHostInfo> localControllers_;
    private int globalControlPort_ = 8888;
    private int simulationLength_= 20000;
    private String remoteLoginCommand_= null;
    private String remoteStartController_= null;
    private String remoteLoginFlags_ = null;
    private String remoteLoginUser_= null;
    private boolean startLocalControllers_= true;
    private boolean isSimulation_= false;
    private int controllerWaitTime_= 600;


    /** init function sets up basic information */
    public void init () {
      
      localControllers_= new ArrayList<LocalHostInfo>();
      startLocalControllers_= true;
      globalControlPort_= 8888;
      remoteLoginCommand_ = "/usr/bin/ssh";
      remoteLoginFlags_ = "-n";
      remoteLoginUser_="richard";
      remoteStartController_ = 
        "java -cp /home/richard/code/userspacerouter usr.controllers.LocalController";


    }
    
    /** Adds information about a new host to the list
    */
    private void addNewHost(LocalHostInfo host) {
      localControllers_.add(host);
    }

    /** Read control options from XML file 
    */
    public ControlOptions (String fName) {
      init();
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
           globalControlPort_= parseSingleInt(gcn, "Port","GlobalController",true);
        } catch (SAXException e) {
            throw e;
        } catch (XMLNoTagException e) {
           
        }
        try {
           startLocalControllers_= parseSingleBool(gcn, "StartLocalControllers", "GlobalController",true);
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
            try {
                hostName= parseSingleText(lc, "Name", "LocalController",false);
                port= parseSingleInt(lc, "Port","LocalController",false);
                LocalHostInfo lh= new LocalHostInfo(hostName, port);
                localControllers_.add(lh);
            } catch (SAXException e) {
                throw e;
            } catch (XMLNoTagException e) {
            }
        }

    }
    
    // Parse tags to get boolean
    private boolean parseSingleBool(Node node, String tag, String parent, boolean optional) 
        throws SAXException, XMLNoTagException
    { 
        String str= parseSingleText(node,tag,parent,optional);
        if (str.equals("true"))
          return true;
        if (str.equals("false"))
          return false;
        throw new SAXException ("Tag "+tag+" parent "+parent+" is not boolean "+str);
    }
    
    // Parse tags to get int
    private int parseSingleInt(Node node, String tag, String parent, boolean optional) 
        throws SAXException, XMLNoTagException
    { 
        String str= parseSingleText(node,tag,parent,optional);
        int i= Integer.parseInt(str);
        return i;
    }
    
    // Parse tags to get text
    private String parseSingleText(Node node, String tag, String parent, 
        boolean optional) 
        throws SAXException, XMLNoTagException
    {
    
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new SAXException("Expecting node element with tag "+tag);
        }
        Element el = (Element)node;
        NodeList hNameList = el.getElementsByTagName(tag);
        if (hNameList.getLength() !=1 && optional == false) {
            throw new SAXException(parent+" element requires exactly one tag "+
              tag);
        }
        if (hNameList.getLength() > 1 ) {
            throw new SAXException(parent + " element can only have one tag "+
              tag);
        }
        if (hNameList.getLength() == 0) {
            throw new XMLNoTagException();
        }
        Element hElement = (Element)hNameList.item(0);
        NodeList textFNList = hElement.getChildNodes();
        return ((Node)textFNList.item(0)).getNodeValue().trim();
    }
    
    /** Return string to launch local controller on remote
    machine given machine name 
    */
    public String [] localControllerStartCommand(LocalHostInfo lh) {
        String [] cmd= new String[5];
        cmd[0] = remoteLoginCommand_;
        cmd[1] = remoteLoginFlags_;
        if (remoteLoginUser_ == null) {
            cmd[2]=lh.getName();
        } else {
            cmd[2] = remoteLoginUser_+"@"+lh.getName();
        }
        cmd[3] = remoteStartController_;
        cmd[4] = String.valueOf(lh.getPort());
        return cmd;
    }
    
    /** Accessor function returns the number of controllers 
    */
    public int noControllers() {
      return localControllers_.size();
    }
    
    /** Accessor function returns the i th controller 
    */
    public LocalHostInfo getController(int i) {
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
   
    /** Return length of simulation
    */
    
    public int getSimulationLength() {
        return simulationLength_;
    }
}

