/** Interface for Script Engine reads an event Script and 
executes events at given times
*/
package usr.engine;

import usr.globalcontroller.*;
import usr.logging.*;
import usr.common.Pair;
import java.util.*;
import java.io.*;

public class ScriptEngine implements EventEngine {
    int timeToEnd_;
    ArrayList<SimEvent> events_= null;

    /** Contructor from Parameter string */
    public ScriptEngine(int time, String parms) 
    {
        timeToEnd_= time*1000;
        readScript(parms);
    }
    
    /** Initial events to add to schedule */
    public void initialEvents(EventScheduler s, GlobalController g)
    {
                // simulation start
        SimEvent e0 = new SimEvent(SimEvent.EVENT_START_SIMULATION, 0, null);
        s.addEvent(e0);
        for (SimEvent e: events_) {
            s.addEvent(e);
        }
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

    private void readScript(String fname)
    {
          events_= new ArrayList<SimEvent>();     
          Scanner scanner= null;
          
          try {
              StringBuilder text = new StringBuilder();
              String NL = System.getProperty("line.separator");
              scanner = new Scanner(new File(fname));
              while (scanner.hasNextLine()){
                  SimEvent e= parseEventLine(scanner.nextLine() + NL);
                  if (e != null)
                      events_.add(e);
              }
          }
          catch (Exception e) {
              Logger.getLogger("log").logln(USR.ERROR, "Cannot parse event list "+fname);
              System.exit(-1);
          }
          finally{
              scanner.close();
          }

    }

    private SimEvent parseEventLine(String s)
    {
        String []args= s.split(" ");
        if (args.length < 2)
            return null;
        try {
            int time= Integer.parseInt(args[0]);
            String type= args[1].trim();
            if (type.equals("START_ROUTER")) {
               return new SimEvent(SimEvent.EVENT_START_ROUTER, time,null);
            }
            if (type.equals("START_LINK")) {
               if (args.length < 4) {
                   throw new Exception ("START_LINK requires two router ids "+
                     s);
               }
               Integer l1= Integer.parseInt(args[2].trim());
               Integer l2= Integer.parseInt(args[3].trim());
               Pair<Integer,Integer> pair= new Pair
                   <Integer,Integer>(l1,l2);
               return new SimEvent(SimEvent.EVENT_START_LINK,time,pair);
            }
            if (type.equals("END_ROUTER")) {
                if (args.length != 3) 
                   throw new Exception("END_ROUTER requires router id "+s);
                Integer r= Integer.parseInt(args[2].trim());
                return new SimEvent(SimEvent.EVENT_END_ROUTER,time,r);
            }
            if (type.equals("END_LINK")) {
                if (args.length != 4) 
                    throw new Exception ("END_LINK requires 2 router ids "+
                      s);
                Integer r1= Integer.parseInt(args[2].trim());
                Integer r2= Integer.parseInt(args[3].trim());
    
                Pair <Integer,Integer> link= new Pair<Integer,Integer>(r1,r2);
                return new SimEvent(SimEvent.EVENT_END_LINK,time,link);
            }
            throw new Exception("Unrecognised event in script line "+s);
             
        } catch (Exception ex) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot read simulation script line "+s);
            Logger.getLogger("log").logln(USR.ERROR, ex.getMessage());
            System.exit(-1);
        }
        return null;
    }

}

