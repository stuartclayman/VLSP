package usr.globalcontroller.visualization;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import usr.common.BasicRouterInfo;
import usr.localcontroller.LocalControllerInfo;
import usr.globalcontroller.GlobalController;
import usr.globalcontroller.TrafficInfo;


/**
 * A visualization of a network graph using colours for
 * both the links and the nodes.
 */
public class ColouredNetworkVisualization implements Visualization {
    GlobalController gc;

    public ColouredNetworkVisualization() {
    }

    /**
     * Set the GlobalController this Visualization gets data from.
     */
    @Override
    public void setGlobalController(GlobalController gc) {
        this.gc = gc;
    }

    /**
     * Visualize the current topology of the network.
     */
    @Override
    public void visualize(PrintStream s) {

        // work out which router is where
        HashMap<LocalControllerInfo, List<BasicRouterInfo> > routerLocations = gc.getRouterLocations();

        // now visit each host and output the routers
        s.println("graph gg {");

        s.println("    K=1;");
        s.println("    ratio=0.7;");
        s.println("    maxiter=2;");
        s.println("    labelloc=t;");
        //s.println("    rank=source;");

        // set root node, if using twopi
        int noAPs = gc.getAPs().size(); 
        int noRouters = gc.getNoRouters();

        if (noAPs > 0) {
            int first = gc.getAPs().get(0);
            s.println("    root=" + first +";");
        }

        // set attributes for subgraphs
        s.println("    graph [");
        s.println("      splines=true,");
        s.println("      rankdir = \"TB\",");
        //s.println("      ranksep = 1.2,");
        s.println("      style=\"setlinewidth(2)\",");
        s.println("      center=true,");
        s.println("      overlap=false,");
        s.println("      fontname=\"Helvetica\", fontsize=16, fontcolor=red");
        s.println("    ];");

        // set attributes for nodes
        s.println("    node [style=filled, fillcolor=\"white\", fontname=\"Helvetica\"];");

        // set attributes for edges
        s.println("    edge [ fontname=\"Helvetica\", fontsize=12 ];");

        // the label of the graph
        s.print("    label=" + "\"snapshot:");
        s.print(" time=");
        long t = gc.getTime()-gc.getStartTime();
        int totalSecs = (int)t / 1000;
        int millis = (int)t % 1000;
        int hundreths = millis / 10;
        int minutes = totalSecs / 60;
        int secs = totalSecs % 60;
        s.printf("%02d:%02d:%02d", minutes, secs, hundreths);
        s.print(" hosts=" + routerLocations.keySet().size());
        s.print(" routers=" + noRouters);
        s.print(" links=" + gc.getNoLinks());
        s.println("\";");

        // visit each host
        for (Map.Entry<LocalControllerInfo, List<BasicRouterInfo>  > entry  : routerLocations.entrySet()) {
            LocalControllerInfo localInfo = entry.getKey();
            List<BasicRouterInfo> routersOnHost = entry.getValue();

            s.println("    subgraph \"cluster_" + localInfo.getName() + "_" + localInfo.getPort() + "\" {");
            s.print("\tlabel=\"" + localInfo + " routers=" + routersOnHost.size() +"\";");
            s.println("\tgraph [fontname=\"Helvetica\",fontsize=16,fontcolor=red,style=filled,fillcolor=\"0.0, 0.0, 0.97\"];");
            s.println("\tnode [ shape=ellipse, style=rounded, nodesep=2.0 ];");

            // now get routers for this host
            for (BasicRouterInfo routerInfo : routersOnHost) {
                // get a router
                int r = routerInfo.getId();

                // get the AggPoint for this router
                int ap = gc.getAP(r); // WAS gc.getAPController().getAP(r);

                // get position of AggPoint in AggPoint list
                int position = gc.getAPs().indexOf(ap);  // WAS gc.getAPController().getAPList().indexOf(ap);

                // work out the hue for the colour of the router
                float hue = 1.0f;

                if (position == -1) {
                    hue = 1.0f;
                } else if (position % 2 == 0) { // even
                    hue = ((float)position / 2) + 1;
                } else {
                    hue = (((float)position -1) / 2) + 5 + 1;
                }

                hue = hue / 10;

                // output the router

                if (ap == r) { // router is also an Agg point
                    float sat = 0.6f;
                    float value = 0.6f;

                    s.print("\t" + r +" [ shape=diamond, label=\"" + routerInfo.getName() + "\"");
                    s.print(", style=\"filled,rounded\"" + ", fillcolor=\"" + hue + "," + sat + "," + value + "\"");  // h,s,v

                } else {  // router is not an Agg point
                    s.print("\t" + r +" [ label=\"" + routerInfo.getName() + "\"");

                    if (ap == 0) {                    // router has NO nominated AggPoint
                        float huew = 0f;
                        float sat = 0f;
                        float value = 1f;

                        s.print(", style=filled, " + " fillcolor=\"" + huew + "," + sat + "," + value + "\"");  // h,s,v

                    } else {                      // router has a nominated AggPoint

                        float sat = 0f;
                        float value = 0f;

                        // is the router directly connected to its AggPoint
                        if (gc.isConnected(r, ap)) {
                            value = 0.85f;
                            sat = 0.5f;
                        } else {
                            value = 0.95f;
                            sat = 0.2f;
                        }


                        s.print(", style=filled, " + " fillcolor=\"" + hue + "," + sat + "," + value + "\"");  // h,s,v
                    }
                }

                s.println(" ];");
            }

            s.println("    }");
        }

        // Find the traffic reporter
        // This is done by asking the GlobalController for
        // a class that implements TrafficInfo.
        // It is this class that has the current traffic info.
        TrafficInfo reporter = (TrafficInfo)gc.findByInterface(TrafficInfo.class);

        System.err.println("reporter = " + reporter);

        // visit all the edges
        for (int i : gc.getRouterList()) {
            BasicRouterInfo router1 = gc.findRouterInfo(i);

            if (router1 == null) continue;

            for (int j : gc.getOutLinks(i)) {
                BasicRouterInfo router2 = gc.findRouterInfo(j);

                if (router2 == null) continue;

                if (i < j) {
                    s.print(i+ " -- "+j);


                    s.print(" [ ");

                    String router1Name = gc.findRouterInfo(i).getName();
                    String router2Name = gc.findRouterInfo(j).getName();

                    // get trafffic for link i -> j as router1Name => router2Name
                    List<Object> iToj = null;

                    if (reporter != null) {
                        iToj = reporter.getTraffic(router1Name, router2Name);
                    }

                    if (iToj != null) {
                        // name | InBytes | InPackets | InErrors | InDropped | InDataBytes | InDataPackets | OutBytes | OutPackets |
                        // OutErrors | OutDropped | OutDataBytes | OutDataPackets | InQueue | BiggestInQueue | OutQueue |
                        // BiggestOutQueue |
                        // Router-1 localnet | 2548 | 13 | 0 | 0 | 2548 | 13 | 10584 | 54 | 0 | 0 | 10584 | 54 | 0 | 1 | 0 | 0 |
                        // pos 1 is InBytes
                        // pos 7 is OutBytes
                        int traffic = (Integer)iToj.get(1) + (Integer)iToj.get(7);

                        s.print("label = \"" + traffic + "\", ");

                        // link colour
                        s.print("color = \"");

                        if (traffic < 1000) {
                            s.print("black");
                        } else if (traffic >= 1000 && traffic < 3000) {
                            s.print("blue");
                        } else if (traffic >= 3000 && traffic < 5000) {
                            s.print("green");
                        } else if (traffic >= 5000 && traffic < 7000) {
                            s.print("yellow");
                        } else if (traffic >= 7000 && traffic < 10000) {
                            s.print("orange");
                        } else if (traffic >= 10000) {
                            s.print("red");

                        }

                        s.print("\", ");

                        if (traffic >= 3000 && traffic < 7000) {
                            s.print(" style=\"setlinewidth(2)\", ");
                        } else if (traffic >= 7000 && traffic < 20000) {
                            s.print(" style=\"setlinewidth(3)\", ");
                        } else if (traffic >= 20000) {
                            s.print(" style=\"setlinewidth(4)\", ");
                        } else {
                        }
                    }

                    s.println(" ];");
                }
            }
        }
        s.println("}");


        s.close();

    }

}
