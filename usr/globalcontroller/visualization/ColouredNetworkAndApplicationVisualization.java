package usr.globalcontroller.visualization;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import usr.common.BasicRouterInfo;
import usr.localcontroller.LocalControllerInfo;
import usr.globalcontroller.GlobalController;
import usr.globalcontroller.TrafficInfo;


/**
 * A visualization of a network graph using colours for both the links and the
 * nodes.
 */
public class ColouredNetworkAndApplicationVisualization implements Visualization {
    GlobalController gc;

    public ColouredNetworkAndApplicationVisualization() {
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
    @SuppressWarnings("unchecked")
    public void visualize(PrintStream s) {

        // work out which router is where
        HashMap<LocalControllerInfo, List<BasicRouterInfo> > routerLocations = gc.getRouterLocations();

        // now visit each host and output the routers
        s.println("graph gg {");

        s.println("    K=1;");
        s.println("    ratio=0.7;");
        s.println("    maxiter=2;");
        s.println("    labelloc=t;");
        // s.println("    rank=source;");

        // set root node, if using twopi
        int noAPs = gc.getAPs().size(); 
        int noRouters = gc.getNoRouters();

        if (noAPs > 0) {
            int first = gc.getAPs().get(0); 
            s.println("    root=" + first + ";");
        }

        // set attributes for subgraphs
        s.println("    graph [");
        s.println("      splines=true,");
        s.println("      rankdir = \"TB\",");
        // s.println("      ranksep = 1.2,");
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
        long t = gc.getElapsedTime();
        int totalSecs = (int)t / 1000;
        int millis = (int)t % 1000;
        int hundreths = millis / 10;
        int minutes = totalSecs / 60;
        int secs = totalSecs % 60;
        s.printf("%02d:%02d:%02d", minutes, secs, hundreths);
        s.print(" hosts=" + routerLocations.keySet().size());
        s.print(" routers=" + noRouters);
        s.print(" links=" + gc.getNoLinks
                ());
        s.println("\";");

        // visit each host
        for (Map.Entry<LocalControllerInfo, List<BasicRouterInfo>  > entry  : routerLocations.entrySet()) {
            LocalControllerInfo localInfo = entry.getKey();
            List<BasicRouterInfo> routersOnHost = entry.getValue();

            s.println("    subgraph \"cluster_" + localInfo.getName() + "_" + localInfo.getPort() + "\" {");
            s.print("\tlabel=\"" + localInfo + " routers=" + routersOnHost.size() + "\";");
            s.println("\tgraph [fontname=\"Helvetica\",fontsize=16,fontcolor=red,style=filled,fillcolor=\"0.0, 0.0, 0.97\"];");
            s.println("\tnode [ shape=ellipse, nodesep=2.0 ];");

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
                    hue = (((float)position - 1) / 2) + 5 + 1;
                }

                hue = hue / 10;

                // output the router

                if (ap == r) { // router is also an Agg point
                    float sat = 0.6f;
                    float value = 0.6f;

                    s.print("\t" + r + " [ shape=diamond, label=\""
                            + routerInfo.getName() + "\"");
                    s.print(", style=\"filled,rounded\"" + ", fillcolor=\""
                            + hue + "," + sat + "," + value + "\""); // h,s,v

                } else { // router is not an Agg point
                    s.print("\t" + r + " [ label=\"" + routerInfo.getName()
                            + "\", style=\"rounded\"");

                    if (ap == 0) { // router has NO nominated AggPoint
                        float huew = 0f;
                        float sat = 0f;
                        float value = 1f;

                        s.print(", style=filled, " + " fillcolor=\"" + huew
                                + "," + sat + "," + value + "\""); // h,s,v

                    } else { // router has a nominated AggPoint

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

                        s.print(", style=filled, " + " fillcolor=\"" + hue
                                + "," + sat + "," + value + "\""); // h,s,v
                    }
                }

                s.println(" ];");
            }

            // now let's find out about what applications are running on the
            // router
            s.println("\n\tnode [shape=record];");

            for (BasicRouterInfo routerInfo : routersOnHost) {
                // get a router
                int r = routerInfo.getId();
                Set<String> apps = gc.findRouterInfo(r).getApplications();
                int total = apps.size();

                if (total > 0) {
                    String subgraphName = "apps_for_" + r;
                    s.print("\t" + subgraphName);
                    s.print(
                            " [ shape=record, fontname=\"Helvetica\", fontsize=11, color=\"#888888\", fontcolor=\"#888888\", label=\"{");

                    // determine apps to show
                    List<String> appsToShow = filterAppList(apps);

                    int count = 0;
                    total = appsToShow.size();

                    for (String app : appsToShow) {
                        count++;

                        // DO any processing on raw app name
                        String nodeName = processAppName(app);

                        // Get the application measurement data from the app itself

                        Map<String, Object> applicationData = routerInfo.getApplicationData(app);
                        // and get the MonitoringData from the applicationData

                        Map<String, String> monitoringData = null;

                        if (applicationData != null) {

                            monitoringData = (Map<String, String> )applicationData.get("MonitoringData");
                        }

                        // convert the map to a string
                        String monString = "";

                        if (monitoringData != null) {
                            for (Map.Entry<String, String> data : monitoringData.entrySet()) {
                                monString += (data.getKey() + "=" + data.getValue() + " ");
                            }
                        }



                        // create graph node for application
                        s.print("" + nodeName + "\\n" + monString + "  ");

                        if (count < total) {
                            s.print(" | ");
                        }
                    }

                    s.println(" }\" ];");
                    // connect application node to router node
                    s.println("\t" + r + " -- " + subgraphName + " [style=dotted, color=\"#888888\" ];");

                }
            }

            // end of host graph cluster
            s.println("}");

        }

        // Find the traffic reporter
        TrafficInfo reporter = (TrafficInfo)gc.findByInterface(TrafficInfo.class);

        // visit all the edges
        for (int i : gc.getRouterList()) {
            BasicRouterInfo router1 = gc.findRouterInfo(i);

            if (router1 == null) continue;

            for (int j : gc.getOutLinks(i)) {
                BasicRouterInfo router2 = gc.findRouterInfo(j);

                if (router2 == null) continue;

                if (i < j) {
                    s.print(i + " -- " + j);

                    s.print(" [ ");

                    String router1Name = gc.findRouterInfo(i).getName();
                    String router2Name = gc.findRouterInfo(j).getName();

                    // get trafffic for link i -> j as router1Name =>
                    // router2Name
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

    /**
     * Get a list of apps
     */
    protected List<String> filterAppList(Set<String> apps) {
        // Example filter --
        // I don't want TopologyManagers to be shown

        List<String> filteredApps = new ArrayList<String>();

        for (String app : apps) {
            if (!app.contains("TopologyManager")) {
                filteredApps.add(app);
            }
        }

        return filteredApps;
    }

    /**
     * Process name of app.
     * Example: /R1/App/usr.applications.Send/1
     */
    protected String processAppName(String app) {
        // options
        // 1. raw app name
        // return app;

        // 2. strip the string of the complete name
        return app.substring(app.lastIndexOf(".") + 1, app.length());

        // 3. compound name
        //return app.substring(app.lastIndexOf(".") + 1, app.length())
        //    .replace('/', '_') + "_on_" + app.substring(1, app.indexOf('/', 1));
    }

}
