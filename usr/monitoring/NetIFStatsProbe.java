// NetIFStatsProbe.java

package usr.monitoring;

import usr.router.*;
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.table.*;
import eu.reservoir.monitoring.appl.datarate.*;
import java.util.List;
import java.util.ArrayList;

/**
 * A probe that talks to a Router can collects the stats
 * for each NetIF.
 */
public class NetIFStatsProbe extends AbstractProbe implements Probe {
    // The controller of the router we are getting stats for
    RouterController controller;

    // The TableHeader for the table of stats
    TableHeader statsHeader; 

    /**
     * Construct a NetIFStatsProbe
     */
    public NetIFStatsProbe(RouterController cont) {
        controller = cont;

        // set probe name
        setName(cont.getName()+".NetIFStats");
        // set data rate
        setDataRate(new EveryNSeconds(10));

	// Define the header. Has:
        // NetIF name,
        // InBytes,
        // InPackets,
        // InErrors,
        // InDropped,
        // OutBytes,
        // OutPackets,
        // OutErrors,
        // OutDropped,
        // InQueue,
        // BiggestInQueue,
        // OutQueue,
        // BiggestOutQueue
	statsHeader = new DefaultTableHeader().
	    add("name", ProbeAttributeType.STRING).
	    add("InBytes", ProbeAttributeType.INTEGER).
	    add("InPackets", ProbeAttributeType.INTEGER).
	    add("InErrors", ProbeAttributeType.INTEGER).
	    add("InDropped", ProbeAttributeType.INTEGER).
	    add("OutBytes", ProbeAttributeType.INTEGER).
	    add("OutPackets", ProbeAttributeType.INTEGER).
	    add("OutErrors", ProbeAttributeType.INTEGER).
	    add("OutDropped", ProbeAttributeType.INTEGER).
	    add("InQueue", ProbeAttributeType.INTEGER).
	    add("BiggestInQueue", ProbeAttributeType.INTEGER).
	    add("OutQueue", ProbeAttributeType.INTEGER).
	    add("BiggestOutQueue", ProbeAttributeType.INTEGER);


        // setup the probe attributes
        // The router name
        // The table of stats
        addProbeAttribute(new DefaultProbeAttribute(0, "RouterName", ProbeAttributeType.STRING, "name"));
        addProbeAttribute(new TableProbeAttribute(1, "Data", statsHeader));

    }

    /**
     * Collect a measurement.
     */
    public ProbeMeasurement collect() {
        System.out.println("NetIFStats: collect()");

        try {
            // check localnet first
            NetIF localNetIF = controller.getLocalNetIF();

            if (localNetIF == null) {
                // the router is not ready yet
                return null;
            }


            // get list of ports
            List<RouterPort> ports = controller.listPorts();

            if (ports.size() == 0) {
                // there are no ports to other routers
                // so there will be no network traffic
                return null;
            }

            // collate measurement values
            ArrayList<ProbeValue> list = new ArrayList<ProbeValue>();

            // add router name
            list.add(new DefaultProbeValue(0, controller.getName()));

            // now allocate a table
            Table statsTable = new DefaultTable();
            statsTable.defineTable(statsHeader);


            // process localNetIF
            NetStats stats = localNetIF.getStats();

            // create a row for localnet data
            TableRow localNetRow = new DefaultTableRow();

            // work out localnet netif name
            String ifLabel = localNetIF.getRemoteRouterName()+ " "+localNetIF.getName();

            // add name of NetIf to row
            localNetRow.add(new DefaultTableValue(ifLabel));

            // now add all NetStats to row
            for (NetStats.Stat s : NetStats.Stat.values()) {
                localNetRow.add(new DefaultTableValue(stats.getValue(s)));
            }


            // add this row to the table
            statsTable.addRow(localNetRow);

            // now visit each Port
            for (RouterPort rp : ports) {
                if (rp.equals(RouterPort.EMPTY)) {
                    continue;
                } else {
                    NetIF netIF = rp.getNetIF();

                    stats = netIF.getStats();

                    // create a row for NetIF data
                    TableRow netIFRow = new DefaultTableRow();

                    // work out netif name
                    ifLabel = netIF.getRemoteRouterName()+ " " + netIF.getName();


                    // add name of NetIf to row
                    netIFRow.add(new DefaultTableValue(ifLabel));

                    // now add all NetStats to row
                    for (NetStats.Stat s : NetStats.Stat.values()) {
                        netIFRow.add(new DefaultTableValue(stats.getValue(s)));
                    }


                    // add this row to the table
                    statsTable.addRow(netIFRow);

                }               
            }             

            list.add(new DefaultProbeValue(1, statsTable));

            // set the type to be: NetIFStats
            return new ProducerMeasurement(this, list, "NetIFStats");
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
