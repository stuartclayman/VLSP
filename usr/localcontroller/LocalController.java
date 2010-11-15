package usr.localcontroller;

import usr.net.GIDAddress;
import java.lang.*;
import java.io.*;
import usr.logging.*;
import java.util.*;
import java.net.*;
import usr.console.*;
import usr.router.*;
import usr.common.LocalHostInfo;
import usr.common.BasicRouterInfo;
import usr.common.ProcessWrapper;
import usr.common.ThreadTools;
import usr.interactor.*;


/** The local controller is intended to run on every machine.  
 * Its job is to start up router processes as needed.
 * It should be started by being given a port to listen on
 * specified on the command line
 * java LocalController.java 8080
 * In its own start up the Global Controller will contact each
 * Local Controller to give it more state
*/
public class LocalController implements ComponentController {
    private LocalControllerInfo hostInfo_;
    private LocalHostInfo globalController_;
    private GlobalControllerInteractor gcInteractor_= null;
    private LocalControllerManagementConsole console_= null;
    private boolean listening_ = true;
    private ArrayList <BasicRouterInfo> routers_= null;
    private ArrayList <RouterInteractor> routerInteractors_ = null;
    private ArrayList <Router> routerList_= null;
    private HashMap<String, ProcessWrapper> childProcessWrappers_ = null;
    private HashMap<Integer, BasicRouterInfo> routerMap_ = null;
    private String routerConfig_= "";  // String contains config for routers
    private String classPath_= null;

    private RouterOptions routerOptions_= null;
    private String myName = "LocalController";
    
    private String routerConfigString_= "";
    
    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        
        LocalController self_;
        
        if (args.length != 1) {
            Logger.getLogger("log").logln(USR.ERROR, "Command line must specify "+
              "port number to listen on and nothing else.");
            System.exit(-1);
        }
        int port= 0;
        
        try {
          port= Integer.parseInt(args[0]);
          if (port < 0) 
            throw new NumberFormatException ("Port number must be > 0");
        }
        catch (NumberFormatException e) {
          Logger.getLogger("log").logln(USR.ERROR, "Unable to understand port number."+
            e.getMessage());
          System.exit(-1);
        }
        self_= new LocalController(port);

    }
    
    /** Constructor for local controller starting on port */
    public LocalController (int port) {
        try {
            hostInfo_= new LocalControllerInfo(port);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot find host info for controller");
            shutDown();
        }
        routers_= new ArrayList<BasicRouterInfo>();
        routerList_= new ArrayList<Router>();
        childProcessWrappers_ = new HashMap<String, ProcessWrapper>();
        routerMap_ = new HashMap<Integer, BasicRouterInfo>();
        routerInteractors_ = new ArrayList<RouterInteractor>();
        console_= new LocalControllerManagementConsole(this, port);
        Properties prop = System.getProperties();
        classPath_= prop.getProperty("java.class.path",null);

        init();

        console_.start();
    }
    
    private void init() {
        routerOptions_= new RouterOptions();
        // allocate a new logger
        Logger logger = Logger.getLogger("log");
        // tell it to output to stdout
        // and tell it what to pick up
        // it will actually output things where the log has bit 
        // USR.STDOUT set
        logger.addOutput(System.out, new BitMask(USR.STDOUT));
        // tell it to output to stderr
        // and tell it what to pick up
        // it will actually output things where the log has bit
        // USR.ERROR set
        logger.addOutput(System.err, new BitMask(USR.ERROR));

    }

    /**
     * Get the name of this LocalController.
     */
    public String getName() {
        return myName + ":" + hostInfo_.getPort();
    }
    
    /** Received shut Down data gram from global */
    public void shutDown() {

        Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Local controller got shutdown message from global controller.");

        //ThreadTools.findAllThreads("LC top of shutDown:");

        Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Stopping all running routers"); 
        for (int i= 0; i < routers_.size(); i++) {

            RouterInteractor interactor = routerInteractors_.get(i);
            try {
                interactor.shutDown();
            } catch (java.io.IOException e) {
                Logger.getLogger("log").logln(USR.ERROR,leadin() + "Cannot send shut down to Router");
                Logger.getLogger("log").logln(USR.ERROR,e.getMessage()); 
            } catch (usr.interactor.MCRPException e) {
                Logger.getLogger("log").logln(USR.ERROR,
                  leadin() + "Cannot send shut down to Router");
                Logger.getLogger("log").logln(USR.ERROR,e.getMessage());          
            }

            //ThreadTools.findAllThreads("LC after router shutDown:");



        }

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopping process wrappers");
        Collection <ProcessWrapper> pws= (Collection<ProcessWrapper>)childProcessWrappers_.values();
        for (ProcessWrapper pw: pws) { 
            pw.stop();
        }

        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopping global controller interactor");
        try {        
          gcInteractor_.quit();
        } catch (Exception e) {
            
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot exit from global interactor");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
        }


        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Stopping console");
        console_.stop();

        Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "Pausing.");

        try {
            Thread.sleep(10);
        } catch (Exception e) {
           // Logger.getLogger("log").logln(USR.ERROR, leadin()+ e.getMessage());
            
        }


        //ThreadTools.findAllThreads("LC end of shutDown:");

        
    }
    
    /**
     * Get the host info the the host this is a LocalController for.
     */
    public LocalControllerInfo getHostInfo() {
        return hostInfo_;
    }
    
    /** 
     * Received alive message from GlobalController. 
     */
    public void aliveMessage(LocalHostInfo gc) {
        globalController_= gc;
        Logger.getLogger("log").logln(USR.STDOUT, "Got alive message from global controller.");
        try {
            Logger.getLogger("log").logln(USR.STDOUT, "Sending to "+gc.getName()+":"+gc.getPort());
            gcInteractor_= new GlobalControllerInteractor(gc);
            gcInteractor_.respondToGlobalController(hostInfo_);
        } catch (java.net.UnknownHostException e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot contact global controller");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            System.exit(-1);
        } catch (java.io.IOException e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot contact global controller");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            System.exit(-1);
        }catch (usr.interactor.MCRPException e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot contact global controller");
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            System.exit(-1);
        }
       
    }
    
    /** 
     * Received start new router command
     * @return the name of the router, on success, or null, on failure.     
     */
    public String requestNewRouter (int routerId, int port1, int port2) 
    
    {

        String routerName = "Router-" + routerId;

        Process child= null;
        ProcessWrapper pw= null;

        String [] cmd= new String[7];
        cmd[0] = "/usr/bin/java";
        cmd[1] = "-cp";
        cmd[2] = classPath_;
        cmd[3] = "usr.router.Router";
        cmd[4] = String.valueOf(port1);
        cmd[5] = String.valueOf(port2);
        cmd[6] = routerName;

        try {
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Starting Router on ports "+port1+" "+port2);

            child = new ProcessBuilder(cmd).start();
            pw = new ProcessWrapper(child, routerName);
            

        } catch (java.io.IOException e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unable to execute command "+ Arrays.asList(cmd));
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            if (child != null) {
                child= null;
            }
            pw.stop();
            //System.exit(-1);
            return null;
        }
        childProcessWrappers_.put(routerName, pw);
        /// In JVM Router
        ///Router router= new Router(maxPort_, "Router-" + routerId);
        ///router.start();
        ///routerList_.add(router);

        // Separate JVM Router
        // create a RouterInteractor
        RouterInteractor interactor = null;

        // try 20 times, with 100 millisecond gap
        int MAX_TRIES = 20;
        int tries = 0;
        int millis = 100;
        boolean isOK = false;
        for (tries = 0; tries < MAX_TRIES; tries++) {
            // sleep a bit
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ie) {
            }

            // try and connect
            try {
                // connect to ManagementConsole of the router on port port1
                interactor = new RouterInteractor("localhost", port1);
                routerInteractors_.add(interactor);
                isOK = true;
                break;
            } catch (UnknownHostException uhe) { // Try again
            } catch (IOException e) {
            }
        }

        if (! isOK) {
            // we didnt connect
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Unable to connect to Router on port " + port1);
            // stop process
            pw.stop();
            child= null;
            try {
                interactor.shutDown();
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + 
                    "IOException connecting to Router on port " + port1);
                Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());
            }catch (MCRPException e2) {
            }
            
            return null;
        } 
        BasicRouterInfo br= new BasicRouterInfo(routerId,0,hostInfo_,port1);
            br.setName(routerName);
            routers_.add(br);

            // tell the router its new name and config if available
        try {
            interactor.setName(routerName);
            interactor.setRouterAddress(new GIDAddress(routerId));

            if (routerConfigString_ != "") {
                interactor.setConfigString(routerConfigString_);
            }
        } catch (IOException ioexc) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + 
                    "IOException setting interactor details for Router on port " + port1);
            pw.stop();
            child= null;
            try {
                interactor.shutDown();
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + 
                    "IOException connecting to Router on port " + port1);
                Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());
            }catch (MCRPException e2) {
            }
        
           return null;
        } catch (MCRPException mcrpe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + 
                    "MCRP Exception setting interactor details for Router on port " + port1);
            pw.stop();
            child= null;
            try {
                interactor.shutDown();
            } catch (IOException e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + 
                    "IOException connecting to Router on port " + port1);
                Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());
            }catch (MCRPException e2) {
            }
            return null;
        }
        routerMap_.put(routerId,br);
        return routerName;
       

    }
    
    

    /**
     * Connect two Routers on two specified hosts.
     * @return the name of the connection, on success, or null, on failure.
     */
    public String connectRouters(LocalHostInfo r1, LocalHostInfo r2) {
       Logger.getLogger("log").logln(USR.STDOUT,leadin() + "Got connect request for routers");

       RouterInteractor ri= findRouterInteractor(r1.getPort());

       if (ri == null) {
           return null;
       } else {
           try {
               String address= r2.getName()+":"+r2.getPort();

               // Create a connection
               String connectionName = ri.createConnection(address,1);

               Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Connection from Router: " + r1 + " to Router: " + r2 + " is " + connectionName);


               return connectionName;
           }
           catch (Exception e) {
               Logger.getLogger("log").logln(USR.ERROR, "Cannot connect routers");
               Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
               return null;
           }
       }       
    }

    /** Send router stats to global controller*/
    public boolean sendRouterStats(List<String> list) 
    {
        StringBuilder sb = new StringBuilder();

        
        for (String s: list) {
            sb.append(s);
            sb.append("***");
        }
        String allStats = sb.toString();
        try {
            gcInteractor_.sendRouterStats(allStats);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+
                "Cannot send stats to global controller "+e.getMessage());
            return false;
        }
        return true; 
    }

    /** Local controller receives request to end a router */
    public boolean endRouter(LocalHostInfo r1) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + 
          "Got terminate request for router "+r1);
        RouterInteractor ri= findRouterInteractor(r1.getPort());
        if (ri == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Cannot find router interactor");
            return false;
        }    
        
        try {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Sending terminate request via interactor");
            ri.shutDown();
        } 
        catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Error shutting down router");
            Logger.getLogger("log").logln(USR.ERROR, leadin()+e.getMessage());
            return false;
        }
        int index= routerInteractors_.indexOf(ri);
        routerInteractors_.remove(index);
        int i;
        for (i= 0; i < routers_.size(); i++) {
            if (routers_.get(i).getManagementPort() == r1.getPort())
                break;
        }
        if (i == routers_.size()) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Router not registered with localcontroller");
            return false;
        }
        BasicRouterInfo br= routers_.get(i);
        String name=br.getName();
        routerMap_.remove(br.getId());
        routers_.remove(i);
        ProcessWrapper p= childProcessWrappers_.get(name);
        //System.err.println("PRocess wrapper "+p+" name "+name);
        p.stop();
        //System.err.println(p.getName());
        childProcessWrappers_.remove(name);
        return true;    
    }
    
    /** Local controller receives request to end a router */
    public boolean endLink(LocalHostInfo r1, int r2) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + 
          "Got terminate request for link from"+r1+" to Id "+r2);
        RouterInteractor ri= findRouterInteractor(r1.getPort());
        if (ri == null)
            return false;
        try {
            ri.endLink("Router-"+r2);
        } 
        catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Error shutting down router");
            Logger.getLogger("log").logln(USR.ERROR, leadin()+e.getMessage());
            return false;
        }
        return true;    
    }

    /** Set string which configures routers */
    public void setRouterOptions(String str) 
    { 
      Logger logger = Logger.getLogger("log");
      routerConfigString_= str;
      try {
          routerOptions_.setOptionsFromString(str);
      } catch (Exception e) {
          Logger.getLogger("log").logln(USR.ERROR,leadin()+"Cannot read options string");
          return;
      }
      
      String fileName= routerOptions_.getOutputFile();
      if (!fileName.equals("")) { 
         if (routerOptions_.getOutputFileAddName()) {
            fileName+= "_"+leadinFname();
         }
         File output= new File(fileName);
         try {
          FileOutputStream fos = new FileOutputStream(output,true);
          PrintWriter pw = new PrintWriter(fos,true);
          logger.removeOutput(System.out);
          logger.addOutput(pw, new BitMask(USR.STDOUT));
        } catch (Exception e) {
          System.err.println("Cannot output to file");
            System.exit(-1);
        }
      }
      String errorName= routerOptions_.getErrorFile();
      if (!errorName.equals("")) { 
         if (routerOptions_.getOutputFileAddName()) {
            errorName+= "_"+leadinFname();
         }
         File output= new File(fileName);
         try {
          FileOutputStream fos = new FileOutputStream(output,true);
          PrintWriter pw = new PrintWriter(fos,true);
          logger.removeOutput(System.err);
          logger.addOutput(pw, new BitMask(USR.ERROR));
        } catch (Exception e) {
          System.err.println("Cannot output to file");
            System.exit(-1);
        }
      }
        
    } 
    
    /** Set the Aggregation point for a given router */
    public boolean setAP(int GID, int AP) 
    {
        BasicRouterInfo br= routerMap_.get(GID);
        if (br == null) {
            Logger.getLogger("log").logln(USR.ERROR,
              leadin()+" cannot find information for router "+GID+
            " to set AP");
            return false;
        }
        int port= br.getManagementPort();
        RouterInteractor ri= findRouterInteractor(port);
        if (ri == null)
            return false;
        try {
            ri.setAP(GID,AP);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR,
              leadin()+"cannot set aggregation point for router "+GID);
            return false;
        }
        return true;    
    }

    /**
     * Get router stats for all routers managed by this LocalController
     */
    public synchronized List<String> getRouterStats() {
        Set<Integer> routerIDs = routerMap_.keySet();

        List<String> result = new ArrayList<String>();
        
        // now add the stats for each router to the list
        for (int routerID : routerIDs) {
            List<String> routerStats = getRouterStats(routerID);

            if (routerStats != null) {
                result.addAll(routerStats);
            }
        }
        
        return result;
    }
    
    /**
     * Get some router stats
     */
    public synchronized List<String> getRouterStats(int routerID) {
        BasicRouterInfo br = routerMap_.get(routerID);

        if (br == null) {
            return null;
        }
        int port = br.getManagementPort();
        RouterInteractor ri = findRouterInteractor(port);

        if (ri == null) {
            return null;
        } else {
            try {
                // original data
                List<String> list = ri.getNetIFStats();

                // now add the router id to each element of the list
                List<String> newList = new ArrayList<String>();

                for (String stat : list) {
                    newList.add(routerID + " " + stat);
                }

                return newList;

            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+"");
                return null;
            }

        }
    }

    /**
     * Run something on a Router.
     */
    public String onRouter(int routerID, String className, String[] args) {
        BasicRouterInfo br = routerMap_.get(routerID);

        int port = br.getManagementPort();
        RouterInteractor ri = findRouterInteractor(port);

        if (ri == null) {
            return null;
        } else {
            try {
                String appName = ri.appStart(className, args);
                return appName;
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin()+"");
                return null;
            }

        }

    }

    /** Report the Aggregation point for a given router */
    public boolean reportAP(int GID, int AP) 
    {   
        try {
            gcInteractor_.reportAP(GID,AP);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR,
              leadin()+"cannot set aggregation point for router "+GID);
            return false;
        }
        return true;    
    }
    /**
     * Get the ManagementConsole this ComponentController interacts with.
     */
    public ManagementConsole getManagementConsole() {
        return console_;
    }

    private RouterInteractor findRouterInteractor(int port) 
    {
        for (RouterInteractor r: routerInteractors_) {
          if (port == r.getPort())
              return r;
        }
        Logger.getLogger("log").logln(USR.ERROR,
          leadin()+"Unable to find router interactor listening on port "+port);
        return null;
    }
    
    public GlobalControllerInteractor getGlobalControllerInteractor()
    {
        return gcInteractor_;
    }

    /**
     * Create the String to append to file names
     */
    String leadinFname() {
        return "LC_"+ hostInfo_.getName() + "_" + hostInfo_.getPort();
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String LC = "LC: ";

        return getName() + " " + LC;
    }



    
}



