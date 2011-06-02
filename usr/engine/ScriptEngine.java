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
    // the time of the last event seen during parsing events
    int lastEventTime = 0;

    // all the events
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

        // first we remove the comments
        String noComments = s.replaceFirst("//.*", "").trim();

        if (noComments.equals("")) {
            return null;
        }

        // now process
        String []args= noComments.split(" ");
        if (args.length < 2)
            return null;
        try {
            // get the time
            // eith absolute as 12000 or relative +2000
            int time = 0;

            if (args[0].startsWith("+")) {
                // relative
                String noPlus = args[0].substring(1);
                int relTime = Integer.parseInt(noPlus);
                time = lastEventTime + relTime;
                lastEventTime = time;
            } else {
                // absolute
                time = Integer.parseInt(args[0]);
                lastEventTime = time;
            }

            String type= args[1].trim();

            if (type.equals("START_ROUTER")) {
                if (args.length == 2) {
                    return new SimEvent(SimEvent.EVENT_START_ROUTER, time,null);
                } else if (args.length == 3) {
                    String name = args[2].trim();
                    return new SimEvent(SimEvent.EVENT_START_ROUTER, time, name);
                } else {
                    throw new Exception("START_ROUTER requires router id or no arg "+ s + " => " + noComments + " args.length = " + args.length);
                }
            }
            else if (type.equals("START_LINK")) {
               if (args.length < 4 || args.length > 6) {
                   throw new Exception ("START_LINK requires two router ids "+ s);
               } else if (args.length == 4) {
                   // time START_LINK r1 r2
                   Integer l1= Integer.parseInt(args[2].trim());
                   Integer l2= Integer.parseInt(args[3].trim());
                   Pair<Integer,Integer> pair= new Pair <Integer,Integer>(l1,l2);

                   Object[] result = new Object[1];
                   result[0] = pair;

                   // return an Object[] of size 1
                   return new SimEvent(SimEvent.EVENT_START_LINK,time,result);

               } else if (args.length == 5) {
                   // time START_LINK r1 r2 weight

                   Integer l1= Integer.parseInt(args[2].trim());
                   Integer l2= Integer.parseInt(args[3].trim());
                   Pair<Integer,Integer> pair= new Pair <Integer,Integer>(l1,l2);

                   Integer weight = Integer.parseInt(args[4].trim());

                   Object[] result = new Object[2];
                   result[0] = pair;
                   result[1] = weight;

                   // return an Object[] of size 1
                   return new SimEvent(SimEvent.EVENT_START_LINK,time,result);

               } else  if (args.length == 6) {
                   // time START_LINK r1 r2 weight name
                   Integer l1= Integer.parseInt(args[2].trim());
                   Integer l2= Integer.parseInt(args[3].trim());
                   Pair<Integer,Integer> pair= new Pair <Integer,Integer>(l1,l2);

                   Integer weight = Integer.parseInt(args[4].trim());

                   // Construct return result
                   Object[] result = new Object[3];
                   result[0] = pair;
                   result[1] = weight;
                   result[2] = args[5].trim();

                   // return an Object[] of size 1
                   return new SimEvent(SimEvent.EVENT_START_LINK,time,result);

               }

            }
            else if (type.equals("END_ROUTER")) {
                if (args.length != 3) 
                   throw new Exception("END_ROUTER requires router id "+s);
                Integer r= Integer.parseInt(args[2].trim());
                return new SimEvent(SimEvent.EVENT_END_ROUTER,time,r);
            }
            else if (type.equals("END_LINK")) {
                if (args.length != 4) 
                    throw new Exception ("END_LINK requires 2 router ids "+ s);
                Integer r1= Integer.parseInt(args[2].trim());
                Integer r2= Integer.parseInt(args[3].trim());
    
                Pair <Integer,Integer> link= new Pair<Integer,Integer>(r1,r2);
                return new SimEvent(SimEvent.EVENT_END_LINK,time,link);
            }
            else if (type.equals("END_SIMULATION")) {
                return new SimEvent(SimEvent.EVENT_END_SIMULATION, time, null);
            }
            else if (type.equals("ON_ROUTER")) {
                if (args.length < 4) {
                    throw new Exception ("ON_ROUTER requires router_id AND className, plus optional args");
                } else {
                    // check router ID
                    Integer r1= Integer.parseInt(args[2].trim());

                    // eliminate time and ON_ROUTER
                    String[] cmdArgs = new String[args.length-2];

                    for (int a=2; a < args.length; a++) {
                        cmdArgs[a-2] = args[a];
                    }

                    return new SimEvent(SimEvent.EVENT_ON_ROUTER, time, cmdArgs);
                }
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

