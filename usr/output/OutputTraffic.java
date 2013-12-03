package usr.output;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Hashtable;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import us.monoid.json.JSONObject;
import usr.events.Event;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

/** Class to output network stuff */
public class OutputTraffic implements OutputFunction {

    /** In fact this only requests output -- actual output occurs later */
    @Override
    public void makeOutput(long t, PrintStream p, OutputType o, GlobalController gc) {
        gc.checkTrafficOutputRequests(t, o);

    }

    @Override
    public void makeEventOutput(Event event, JSONObject result, PrintStream s, OutputType out, GlobalController gc) {
        makeOutput(event.getTime(), s, out, gc);
    }

    @Override
    public void parseExtraXML(Node n) throws SAXException {
    }

    public void produceOutput(long t, PrintStream p, OutputType o, GlobalController gc) {
        String routerStats = gc.getRouterStats();

        if (routerStats == null || routerStats.equals("")) {
            return;
        }
        synchronized (routerStats) {
            if (o.getParameter().equals("Local")) {
                outputTrafficLocal(o, t, p, routerStats);
            } else if (o.getParameter().equals("Aggregate")) {
                outputTrafficAggregate(o, t, p, routerStats, gc);
            } else if (o.getParameter().equals("Raw")) {

                for (String s : routerStats.split("\\*\\*\\*")) {
                    p.println(t+" "+s);
                }
            } else if (o.getParameter().equals("Separate")) {
                outputTrafficSeparate(o, t, p, routerStats);
            } else {
                Logger.getLogger("log").logln(USR.ERROR, "Unable to parse traffic output parameter "+o.getParameter());
            }
        }
    }

    void outputTrafficLocal(OutputType o, long t, PrintStream p, String routerStats) {
        for (String s : routerStats.split("\\*\\*\\*")) {
            String [] args = s.split("\\s+");

            if (o.isFirst()) {
                o.setFirst(false);
                p.print("Time r_no name ");

                for (int i = 3; i < args.length; i++) {
                    p.print(args[i].split("=")[0]);
                    p.print(" ");
                }
                p.println();
            }

            if (args.length < 3) {
                continue;
            }

            if (!args[2].equals("localnet")) {
                continue;
            }
            p.print(t+" ");
            p.print(args[0] + " " + args[1] + " ");

            for (int i = 3; i < args.length; i++) {
                p.print(args[i].split("=")[1]);
                p.print(" ");
            }
            p.println();
        }
    }

    void  outputTrafficAggregate (OutputType o, long t, PrintStream p, String routerStats, GlobalController gc) {

        HashMap<String, int []> trafficLinkCounts = gc.getTrafficLinkCounts();
        Hashtable<Integer, Boolean> routerCount = new Hashtable<Integer, Boolean>();

        String [] out = routerStats.split("\\*\\*\\*");

        if (out.length < 1) {
            return;
        }
        int nField = out[0].split("\\s+").length - 3;

        if (nField <= 0) {
            Logger.getLogger("log").logln(USR.ERROR, "Can't parse no of fields in stats line "+out[0]);
            Logger.getLogger("log").logln(USR.ERROR, "Stats Line \""+routerStats+"\"");

            return;
        }

        if (trafficLinkCounts == null) {
            trafficLinkCounts = new HashMap<String, int []>();
        }
        int nLinks = 0;
        int nRouters = 0;
        int [] totCount = new int [nField];

        for (int i = 0; i < nField; i++) {
            totCount[i] = 0;
        }

        for (String s : out) {
            int [] count = new int [nField];

            for (int i = 0; i < nField; i++) {
                count[i] = 0;
            }
            String [] args = s.split("\\s+");

            if (o.isFirst()) {
                o.setFirst(false);
                p.print("Time nRouters nLinks*2 ");

                for (int i = 3; i < args.length; i++) {
                    p.print(args[i].split("=")[0]);
                    p.print(" ");
                }
                p.println();
            }

            if (args.length < 3) {
                continue;
            }

            if (args[2].equals("localnet")) {
                continue;
            }
            nLinks++;

            int router = Integer.parseInt(args[0]);

            if (routerCount.get(router) == null) {
                nRouters++;
                routerCount.put(router, true);
                //System.err.println("Time "+t+" found router "+router);
            }


            String linkName = args[0]+args[2];

            for (int i = 3; i < args.length; i++) {
                String[] spl = args[i].split("=");

                if (spl.length !=2) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin()+
                                                  " Cannot parse traffic stats "+args[i]);
                } else {
                    count[i-3] = Integer.parseInt(spl[1]);

                }
            }
            //System.err.println("new count "+linkName+" "+count[0]);
            int [] oldCount = trafficLinkCounts.get(linkName);

            if (oldCount == null) {

                for (int i = 0; i < nField; i++) {
                    totCount[i] += count[i];
                }
            } else {
                //System.err.println("old count "+linkName+" "+oldCount[0]);
                for (int i = 0; i < nField; i++) {
                    totCount[i] += count[i]-oldCount[i];

                }
            }
            trafficLinkCounts.put(linkName, count);
        }


        p.print(t+" "+nRouters+" "+nLinks+" ");

        for (int i = 0; i < nField; i++) {
            p.print(totCount[i]+" ");
        }
        p.println();
    }

    void outputTrafficSeparate (OutputType o, long t, PrintStream p, String routerStats) {
        //System.err.println("Performing output");
        String [] out = routerStats.split("\\*\\*\\*");

        if (out.length < 1) {
            return;
        }

        for (String s : out) {
            //System.err.println("String is "+s);
            String [] args = s.split("\\s+");

            if (o.isFirst()) {
                o.setFirst(false);
                p.print("Time r_no name ");

                for (int i = 2; i < args.length; i++) {
                    p.print(args[i].split("=")[0]);
                    p.print(" ");
                }
                p.println();
            }

            if (args.length < 2) {
                continue;
            }

            if (args[1].equals("localnet")) {
                continue;
            }
            p.print(t+" ");
            p.print(args[1]+" "+args[2] + " ");

            for (int i = 3; i < args.length; i++) {
                String [] splitit = args[i].split("=");

                if (splitit.length < 2) {
                    System.err.println("Cannot split "+ args[i]);
                } else {
                    p.print(args[i].split("=")[1]);
                    p.print(" ");
                }
            }
            p.println();
        }
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String GCOT = "GCOT: ";

        return GCOT;
    }

}