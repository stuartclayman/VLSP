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

        for (SimEvent e: events_) {
            s.addEvent(e);
        }

        
    }
   
    public void startStopEvents(EventScheduler s, GlobalController g)
    {
        // simulation start
        SimEvent e;
        e = new SimEvent(SimEvent.EVENT_START_SIMULATION, 0, null,this);
        s.addEvent(e);
        // simulation end
        e= new SimEvent(SimEvent.EVENT_END_SIMULATION, timeToEnd_, null,this);
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

        //System.err.println("ScriptEngine: input line: " + s);

        // first we remove the comments
        String noComments = s.replaceFirst("//.*", "").trim();

        if (noComments.equals("")) {
            return null;
        }

        // now process
        String []args= noComments.split("\\s+");
        if (args.length < 2)
            return null;
        try {
            //System.err.println("ScriptEngine: process args: " + Arrays.asList(args));
                       

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
                    return new SimEvent(SimEvent.EVENT_START_ROUTER, time,null,this);

                } else if (args.length == 3) {
                    String address = args[2].trim();
                    return new SimEvent(SimEvent.EVENT_START_ROUTER, time, address,this);
                } else if (args.length == 4) {
                    String address = args[2].trim();
                    String name = args[3].trim();

                    Pair<String,String> pair = new Pair<String, String>(address, name);

                    return new SimEvent(SimEvent.EVENT_START_ROUTER, time, pair,this);
                } else {
                    throw new Exception("START_ROUTER requires router id or no arg "+ s + " => " + noComments + " args.length = " + args.length);
                }
            }
            else if (type.equals("START_LINK")) {
               if (args.length < 4 || args.length > 6) {
                   throw new Exception ("START_LINK requires two router ids "+ s);
               } else {
                   // process links
                   Pair<?,?> pair = null;

                   String arg2 = args[2].trim();
                   String arg3 = args[3].trim();

                   Scanner arg2Scanner = new Scanner(arg2);
                   Scanner arg3Scanner = new Scanner(arg3);

                   if (arg2Scanner.hasNextInt() && arg3Scanner.hasNextInt()) {
                       // both args are ints
                       int l1 = arg2Scanner.nextInt();
                       int l2 = arg3Scanner.nextInt();
                       pair = new Pair <Integer,Integer>(l1,l2);

                   } else {
                       // they are not both ints
                       pair = new Pair<String, String>(arg2, arg3);

                   }

                   // now get extra args
                   if (args.length == 4) {
                       // time START_LINK r1 r2
                       Object[] result = new Object[1];
                       result[0] = pair;

                       // return an Object[] of size 1
                       return new SimEvent(SimEvent.EVENT_START_LINK,time,result,this);

                   } else if (args.length == 5) {
                       // time START_LINK r1 r2 weight
                       Integer weight = Integer.parseInt(args[4].trim());

                       Object[] result = new Object[2];
                       result[0] = pair;
                       result[1] = weight;

                       // return an Object[] of size 1
                       return new SimEvent(SimEvent.EVENT_START_LINK,time,result,this);

                   } else  if (args.length == 6) {
                       // time START_LINK r1 r2 weight name
                       Integer weight = Integer.parseInt(args[4].trim());

                       // Construct return result
                       Object[] result = new Object[3];
                       result[0] = pair;
                       result[1] = weight;
                       result[2] = args[5].trim();

                       // return an Object[] of size 1
                       return new SimEvent(SimEvent.EVENT_START_LINK,time,result,this);

                   }
               }

            }
            else if (type.equals("END_ROUTER")) {
                if (args.length != 3) {
                   throw new Exception("END_ROUTER requires router id "+s);
                } else {
                    String arg2 = args[2].trim();

                    Scanner arg2Scanner = new Scanner(arg2);

                    if (arg2Scanner.hasNextInt()) {
                       // arg is int
                       int r = arg2Scanner.nextInt();
                       return new SimEvent(SimEvent.EVENT_END_ROUTER,time,r,this);

                    } else {
                        // arg is String
                       return new SimEvent(SimEvent.EVENT_END_ROUTER,time,arg2,this);
                    }
                }

            }
            else if (type.equals("END_LINK")) {
                if (args.length != 4) {
                    throw new Exception ("END_LINK requires 2 router ids "+ s);
                } else {
                   // process links
                   Pair<?,?> pair = null;

                   String arg2 = args[2].trim();
                   String arg3 = args[3].trim();

                   Scanner arg2Scanner = new Scanner(arg2);
                   Scanner arg3Scanner = new Scanner(arg3);

                   if (arg2Scanner.hasNextInt() && arg3Scanner.hasNextInt()) {
                       // both args are ints
                       int l1 = arg2Scanner.nextInt();
                       int l2 = arg3Scanner.nextInt();
                       pair = new Pair <Integer,Integer>(l1,l2);

                   } else {
                       // they are not both ints
                       pair = new Pair<String, String>(arg2, arg3);

                   }

                    return new SimEvent(SimEvent.EVENT_END_LINK,time, pair,this);
                }

            }
            else if (type.equals("END_SIMULATION")) {
                return new SimEvent(SimEvent.EVENT_END_SIMULATION, time, null,this);
            }

            else if (type.equals("ON_ROUTER")) {
                if (args.length < 4) {
                    throw new Exception ("ON_ROUTER requires router_id AND className, plus optional args");
                } else {

                    // eliminate time and ON_ROUTER
                    String[] cmdArgs = new String[args.length-2];

                    for (int a=2; a < args.length; a++) {
                        cmdArgs[a-2] = args[a];
                    }

                    return new SimEvent(SimEvent.EVENT_ON_ROUTER, time, cmdArgs,this);
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

