/** Interface for Script Engine reads an event Script and 
executes events at given times
*/

package usr.engine;

import usr.globalcontroller.*;
import usr.common.Pair;
import java.util.*;
import java.io.*;

public class ScriptEngine implements EventEngine {
    int timeToEnd_;
    ArrayList<SimEvent> events_= null;

    /** Contructor from Parameter string */
    public ScriptEngine(int time, String parms) 
    {
        timeToEnd_= time;
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
              System.err.println("Cannot parse event list "+fname);
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
                   throw new Exception ("START_LINK requires two link ids "+
                     s);
               }
               Integer l1= Integer.parseInt(args[2].trim());
               Integer l2= Integer.parseInt(args[3].trim());
               Pair<Integer,Integer> pair= new Pair
                   <Integer,Integer>(1,1);
               return new SimEvent(SimEvent.EVENT_START_LINK,time,pair);
            }
            throw new Exception("Unrecognised event in script line "+s);
             
        } catch (Exception ex) {
            System.err.println("Cannot read simulation script line "+s);
            System.err.println(ex.getMessage());
            System.exit(-1);
        }
        return null;
    }

}

