/** Interface for Script Engine reads an event Script and
 * executes events at given times
 */
package usr.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import us.monoid.json.JSONObject;
import usr.events.StartSimulationEvent;
import usr.events.EndSimulationEvent;
import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.events.vim.StartAppEvent;
import usr.events.vim.EndLinkEvent;
import usr.events.vim.EndRouterEvent;
import usr.events.vim.StartLinkEvent;
import usr.events.vim.StartRouterEvent;
import usr.logging.Logger;
import usr.logging.USR;

public class ScriptEngine implements EventEngine {
    int timeToEnd_;

    // the time of the last event seen during parsing events
    int lastEventTime = 0;

    // all the events
    ArrayList<Event> events_ = null;

    /** Contructor from Parameter string */
    public ScriptEngine(int time, String parms) throws EventEngineException {
        timeToEnd_ = time * 1000;
        readScript(parms);
    }

    /** Initial events to add to schedule */
    @Override
    public void initialEvents(EventScheduler s, EventDelegate g) {
        for (Event e : events_) {
            s.addEvent(e);
        }
    }

    @Override
    public void startStopEvents(EventScheduler s, EventDelegate g) {
        // simulation start
        StartSimulationEvent e0 = new StartSimulationEvent(0);

        s.addEvent(e0);

        /*

        // simulation end
        EndSimulationEvent e = new EndSimulationEvent(timeToEnd_);
        s.addEvent(e);
        */
    }

    /** Add or remove events following a simulation event */
    @Override
    public void preceedEvent(Event e, EventScheduler s, EventDelegate g) {
    }

    /** Add or remove events following a simulation event */
    @Override
    public void followEvent(Event e, EventScheduler s, JSONObject response, EventDelegate g) {
    }
    
    @Override
    public void finalEvents(EventDelegate obj) {
    }


    private void readScript(String fname) {
        events_ = new ArrayList<Event>();
        Scanner scanner = null;

        try {
            new StringBuilder();
            String NL = System.getProperty("line.separator");
            scanner = new Scanner(new File(fname));

            while (scanner.hasNextLine()) {
                Event e = parseEventLine(scanner.nextLine() + NL);

                if (e != null) {
                    events_.add(e);
                }
            }
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          "Cannot parse event list " + fname);
            System.exit(-1);
        }

        finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private Event parseEventLine(String s) {
        //System.err.println("ScriptEngine: input line: " + s);

        // first we remove the comments
        String noComments = s.replaceFirst("//.*", "").trim();

        if (noComments.equals("")) {
            return null;
        }

        // now process
        String [] args = noComments.split("\\s+");

        if (args.length < 2) {
            return null;
        }

        try {
            //System.err.println("ScriptEngine: process args: " +
            // Arrays.asList(args));

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

            String type = args[1].trim();

            if (type.equals("START_ROUTER")) {
                if (args.length == 2) {
                    return new StartRouterEvent(time, this);
                } else if (args.length == 3) {
                    String address = args[2].trim();
                    return new StartRouterEvent(time, this, address, null);
                } else if (args.length == 4) {
                    String address = args[2].trim();
                    String name = args[3].trim();

                    return new StartRouterEvent(time, this, address, name);
                } else {
                    throw new Exception(
                                        "START_ROUTER requires router id or no arg "
                                        +
                                        s
                                        + " => " + noComments + " args.length = "
                                        + args.length);
                }
            } else if (type.equals("START_LINK")) {
                if ((args.length < 4) || (args.length >
                                          6)) {
                    throw new Exception(
                                        "START_LINK requires two router ids "
                                        +
                                        s
                                        + " plus optional weight and name");
                } else {
                    // process links
                    String addStr1 = args[2].trim();
                    String addStr2 = args[3].trim();

                    Scanner a1Scanner = new Scanner(addStr1);
                    Scanner a2Scanner = new Scanner(addStr2);
                    int addr1 = 0;
                    int addr2 = 0;
                    StartLinkEvent e;

                    if (a1Scanner.hasNextInt() && a2Scanner.hasNextInt()) {
                        // both args are ints
                        addr1 = a1Scanner.nextInt();
                        addr2 = a2Scanner.nextInt();
                        a1Scanner.close();
                        a2Scanner.close();
                        e = new StartLinkEvent(time, this, addr1, addr2);
                    } else {
                        e = new StartLinkEvent(time, this, addStr1, addStr2);
                    }

                    if (args.length == 5) {
                        // time START_LINK r1 r2 weight
                        int weight;
                        try {
                            weight = Integer.parseInt(args[4].trim());
                        } catch (Exception ex) {
                            throw new Exception(
                                                "Cannot interpret weight as integer in "
                                                +
                                                s);
                        }

                        e.setWeight(weight);
                    } else if (args.length == 6) {
                        e.setLinkName(args[5]);
                    }

                    return e;
                }
            } else if (type.equals("END_ROUTER")) {
                if (args.length != 3) {
                    throw new Exception(
                                        "END_ROUTER requires router id "
                                        + s);
                } else {
                    String arg2 = args[2].trim();

                    Scanner arg2Scanner = new Scanner(arg2);

                    if (arg2Scanner.hasNextInt()) {
                        // arg is int
                        int r = arg2Scanner.nextInt();
                        arg2Scanner.close();
                        return new EndRouterEvent(time, this, r);
                    } else {
                        // arg is String
                    	arg2Scanner.close();
                        return new EndRouterEvent(time, this, arg2);
                    }
                }
            } else if (type.equals("END_LINK")) {
                if (args.length != 4) {
                    throw new Exception(
                                        "END_LINK requires 2 router ids "
                                        + s);
                } else {
                    String arg2 = args[2].trim();
                    String arg3 = args[3].trim();

                    Scanner arg2Scanner = new Scanner(arg2);
                    Scanner arg3Scanner = new Scanner(arg3);

                    if (arg2Scanner.hasNextInt()
                        && arg3Scanner.hasNextInt()) {
                        // both args are ints
                        int l1 = arg2Scanner.nextInt();
                        int l2 = arg3Scanner.nextInt();
                        arg2Scanner.close();
                        arg3Scanner.close();
                        return new EndLinkEvent(time, this, l1, l2);
                    } else {
                    	arg2Scanner.close();
                    	arg3Scanner.close();
                        return new EndLinkEvent(time, this, arg2, arg3);
                    }
                }
            } else if (type.equals("END_SIMULATION")) {
                return new EndSimulationEvent(time);
            } else if (type.equals("START_APP")) {
                if (args.length < 4) {
                    throw new Exception(
                                        "START_APP requires router_id AND className, plus optional args");
                } else {
                    // eliminate time and START_APP
                    String[] cmdArgs = new String[args.length - 4];

                    for (int a = 4; a < args.length; a++) {
                        cmdArgs[a - 4] = args[a];
                    }

                    String args2 = args[2].trim();
                    Scanner arg2Scanner = new Scanner(args2);

                    if (arg2Scanner.hasNextInt()) {
                        int rno = arg2Scanner.nextInt();
                        arg2Scanner.close();
                        return new StartAppEvent(time, this, rno, args[3], cmdArgs);
                    } else {
                    	arg2Scanner.close();
                        return new StartAppEvent(time, this, args[2], args[3], cmdArgs);
                    }
                }
            }

            throw new Exception(
                                "Unrecognised event in script line " + s);
        } catch (Exception ex) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot read simulation script line " + s);
            Logger.getLogger("log").logln(USR.ERROR, ex.getMessage());
            System.exit(-1);
        }

        return null;
    }

}
