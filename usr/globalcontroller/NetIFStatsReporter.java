// NetIFStatsReporter.java

package usr.globalcontroller;

import usr.router.*;
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.table.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A NetIFStatsReporter collects measurements sent by
 * a NetIFStatsProbe embedded in each Router.
 */
public class NetIFStatsReporter implements Reporter {
    GlobalController globalController;

    // A HashMap of RouterName -> latest measurement
    HashMap<String, Table> measurements;

    // A HashMap of RouterName -> old measurement
    HashMap<String, Table> old;

    // count of no of measurements
    int count = 0;

    /**
     * Constructor
     */
    public NetIFStatsReporter(GlobalController gc) {
	globalController = gc;

	measurements = new HashMap<String, Table>();
	old = new HashMap<String, Table>();
    }

    /**
     * This collects each measurement and processes it.
     * Each measurement has the following structure:
     * ProbeValue 0: String =
     * Router-1
     * ProbeValue 1: Table =
     * name | InBytes | InPackets | InErrors | InDropped | InDataBytes | InDataPackets | OutBytes | OutPackets | OutErrors | OutDropped | OutDataBytes | OutDataPackets | InQueue | BiggestInQueue | OutQueue | BiggestOutQueue |
     * Router-1 localnet | 2548 | 13 | 0 | 0 | 2548 | 13 | 10584 | 54 | 0 | 0 | 10584 | 54 | 0 | 1 | 0 | 0 |
     * Router-4 /Router-1/Connection-3 | 2925 | 18 | 0 | 0 | 2548 | 13 | 292 | 4 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 1 |
     * Router-5 /Router-1/Connection-4 | 3351 | 19 | 0 | 0 | 3136 | 16 | 308 | 4 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 1 |
     * Router-7 /Router-1/Connection-5 | 1314 | 8 | 0 | 0 | 1176 | 6 | 178 | 2 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 1 |

     */
    public void report(Measurement m) {
	if (m.getType().equals("NetIFStats")) {
	    count++;

	    List<ProbeValue> values = m.getValues();

	    // ProbeValue 0 is the router name
	    ProbeValue pv0 = values.get(0);
	    String routerName = (String)pv0.getValue();

	    // ProbeValue 1 is the table
	    ProbeValue pv1 = values.get(1);
	    Table table = (Table)pv1.getValue();

	    // print table
	    //printTable(table);

	    synchronized (measurements) {
		measurements.put(routerName, table);
	    }

	    //printTable(table);

	    // Calculate volume of traffic - in and out
	    //int volume = calculateTraffic(table);

	    //System.out.println("Traffic for " + routerName + " = " + volume);

	    // Any dropped ?
	    //printAnyDropped(routerName, table);

	    // print out total every 100 measurements
	    if (count % 100 == 0) {
		System.out.println("Total = " + calculateTotalTraffic());
	    }


	} else {
	    // not what we were expecting
	}
    }

    /**
     * Get the traffic for a link Router-i to Router-j
     * @param routerSrc the name of source router
     * @param routerDst the name of dest router
     */
    public List<Object> getTraffic(String routerSrc, String routerDst) {
	Table table = measurements.get(routerSrc);

	if (table == null) {
	    // there are no measurements
	    return null;
	} else {
	    // skip through all rows looking for routerDst
	    int rows = table.getRowCount();

	    // skip row 0, which is localhost
	    for (int r=1; r< rows; r++) {
		TableRow row = table.getRow(r);

		// get name
		TableValue tableValue = row.get(0);
		String linkName = (String)tableValue.getValue();

		// check name
		if (linkName.startsWith(routerDst)) {
		    // we've found it
		    List<TableValue> rowAsList = row.toList();

		    // now convert to List<Object>
		    List<Object> data = new ArrayList<Object>(row.size());

		    for (TableValue value : rowAsList) {
			data.add(value.getValue());
		    }

		    return data;
		}
	    }

	    // found nothing
	    return null;
	}
    }


    /**
     * Tell this reporter that a router has been deleted
     */
    protected void routerDeleted(String routerName) {
	Table oldData = null;

	synchronized (measurements) {
	    oldData = measurements.get(routerName);
	    old.put(routerName, oldData);
	    measurements.remove(routerName);
	}

	System.out.println("Deleted router: " + routerName + " Lost data " + calculateTraffic(oldData));

    }

    /**
     * Calculate the traffic for a router
     */
    protected int calculateTraffic(Table table) {
	int volume = 0;

	int rows = table.getRowCount();

	// skip row 0, which is localhost
	for (int r=1; r< rows; r++) {
	    TableRow row = table.getRow(r);

	    // in bytes
	    TableValue tableValue = row.get(1);
	    int inBytes = (Integer)tableValue.getValue();
	    // out bytes
	    tableValue = row.get(5);
	    int outBytes = (Integer)tableValue.getValue();

	    volume += inBytes + outBytes;
	}

	return volume;
    }


    /**
     * Caluclaute total traffic.
     */
    protected int calculateTotalTraffic() {
	int volume = 0;

	for (String routerName : measurements.keySet()) {
	    Table routerData = measurements.get(routerName);
	    volume += calculateTraffic(routerData);
	}

	System.out.println("Total volume = " + volume);

	int lost = 0;

	for (String routerName : old.keySet()) {
	    Table routerData = old.get(routerName);
	    lost += calculateTraffic(routerData);
	}

	System.out.println("Total lost = " + lost);

	return lost + volume;
    }

    /**
     * Any dropped ?
     */
    private void printAnyDropped(String routerName, Table table) {
	int rows = table.getRowCount();

	for (int r=0; r< rows; r++) {
	    TableRow row = table.getRow(r);

	    // name
	    TableValue tableValue = row.get(0);
	    String linkName = (String)tableValue.getValue();

	    // in dropped
	    tableValue = row.get(4);
	    int inDropped = (Integer)tableValue.getValue();

	    // out dropped
	    tableValue = row.get(10);
	    int outDropped = (Integer)tableValue.getValue();

	    if (inDropped > 0 || outDropped > 0) {
		System.out.print("Dropped: " + linkName);

		if (inDropped > 0) {
		    System.out.print(" In = " + inDropped);
		}

		if (outDropped > 0) {
		    System.out.print(" Out = " + outDropped);
		}
		System.out.println();
	    }
	}

    }

    /**
     * Print the table
     */
    private void printTable(Table table) {
	// get no of cols
	int cols = table.getColumnCount();

	for (int c=0; c<cols; c++) {
	    TableAttribute headerAttr = table.getColumnDefinitions().get(c);
	    System.out.print(headerAttr.getName() + " | ");
	}
	System.out.println();

	// now print out values
	int rows = table.getRowCount();

	for (int r=0; r< rows; r++) {
	    TableRow row = table.getRow(r);

	    for (int c=0; c<cols; c++) {
		TableValue tableValue = row.get(c);
		System.out.print(tableValue.getValue() + " | ");
	    }
	    System.out.println();
	}
	System.out.println();
    }

}
