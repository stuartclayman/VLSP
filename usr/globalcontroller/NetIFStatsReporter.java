// NetIFStatsReporter.java

package usr.globalcontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import usr.common.ANSI;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.ProbeValue;
import eu.reservoir.monitoring.core.Reporter;
import eu.reservoir.monitoring.core.table.Table;
import eu.reservoir.monitoring.core.table.TableAttribute;
import eu.reservoir.monitoring.core.table.TableRow;
import eu.reservoir.monitoring.core.table.TableValue;

/**
 * A NetIFStatsReporter collects measurements sent by
 * a NetIFStatsProbe embedded in each Router.
 * It shows the traffic sent over the network since last time.
 */
public class NetIFStatsReporter implements Reporter,
RouterDeletedNotification, TrafficInfo {
    GlobalController globalController;

    // A HashMap of RouterName -> latest measurement
    HashMap<String, Table> measurements;

    // A HashMap of RouterName -> old measurement
    // GT: data for deleted routers
    HashMap<String, Table> old;

    // A HashMap of RouterName -> old measurement
    // GT: measurements from previous monitoring cycle
    HashMap<String, Table> previousMeasurements;

    // count of no of measurements
    int count = 0;

    /**
     * Constructor
     */
    public NetIFStatsReporter(GlobalController gc) {
        globalController = gc;

        measurements = new HashMap<String, Table>();
        old = new HashMap<String, Table>();
        previousMeasurements = new HashMap<String, Table>();
    }

    /**
     * This collects each measurement and processes it.
     * Each measurement has the following structure:
     * ProbeValue 0: String =
     * Router-1
     * ProbeValue 1: Table =
     * name | InBytes | InPackets | InErrors | InDropped | InDataBytes |
     *******************************InDataPackets | OutBytes | OutPackets |
     *****************OutErrors
     *|
     *|*|*|*|*|*|*|*|*|*|************OutDropped
     *|
     *|*|*|*|*|*|*|*|*|*|*|*|********OutDataBytes | OutDataPackets | InQueue
     *||
     *||*||*||*||*||*|*|*|*|*|*|*|*|*|BiggestInQueue
     *||
     *||*||*||*||*||*||*||*|*|*|****OutQueue
     *|
     *|*|*|*|*|*|*|*|*|*|*|*|*|*|***BiggestOutQueue |
     * Router-1 localnet | 2548 | 13 | 0 | 0 | 2548 | 13 | 10584 | 54 | 0 |
     *******************************0 | 10584 | 54 | 0 | 1 | 0 | 0 |
     * Router-4 /Router-1/Connection-3 | 2925 | 18 | 0 | 0 | 2548 | 13 | 292
     *| 4 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 1 |
     * Router-5 /Router-1/Connection-4 | 3351 | 19 | 0 | 0 | 3136 | 16 | 308
     *| 4 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 1 |
     * Router-7 /Router-1/Connection-5 | 1314 | 8 | 0 | 0 | 1176 | 6 | 178 |
     *******************************2 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 1 |
     *
     */
    @Override
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

            // GT - save old Measurement
            previousMeasurements.put(routerName,
                                     measurements.get(routerName));

            synchronized (measurements) {
                measurements.put(routerName, table);
            }

            // sclayman 20130808 THIS IS A RECEIVER OF DATA. DONT CALL EVENT
            // sclayman 20130808 NetStatsEvent nse = new NetStatsEvent(globalController.getElapsedTime(), tableToString(table, false, true));
            // sclayman 20130808 globalController.addEvent(nse);

            // Calculate volume of traffic - in and out
            //int volume = calculateTraffic(table);

            //System.out.println("Traffic for " + routerName + " = " +
            // volume);

            // Any dropped ?
            //printAnyDropped(routerName, table);

            // print out total every 100 measurements
            if (count % 100 == 0) {
                System.out.println("Total = " + calculateTotalTraffic());
            }
        } else {
            // not what we handle
        }
    }

    /**
     * Get the traffic for a link Router-i to Router-j
     * @param routerSrc the name of source router
     * @param routerDst the name of dest router
     */
    @Override
	public List<Object> getTraffic(String routerSrc, String routerDst) {
        Table tablePrev = previousMeasurements.get(routerSrc);

        Table table = measurements.get(routerSrc);

        if (tablePrev == null) {
            if (table == null) {
                // there are no measurements
                return null;
            } else {
                // there are some measurements in measurements
                // but none in previousMeasurements
                return findRowData(table, routerDst);
            }
        } else {
            // find previous row for routerDst
            List<Object> rowPrev = findRowData(tablePrev, routerDst);

            // find current row for routerDst
            List<Object> row = findRowData(table, routerDst);

            if (rowPrev == null) {
                if (row == null) {
                    // found nothing
                    return null;
                } else {
                    return row;
                }
            } else {
                // calcualte difference between the rows
                // name | InBytes | InPackets | InErrors | InDropped |
                // InDataBytes | InDataPackets | OutBytes | OutPackets |
                // OutErrors | OutDropped | OutDataBytes |
                // OutDataPackets |
                // InQueue | BiggestInQueue | OutQueue | BiggestOutQueue
                // |
                // Router-1 localnet | 2548 | 13 | 0 | 0 | 2548 | 13 |
                // 10584
                // | 54 | 0 | 0 | 10584 | 54 | 0 | 1 | 0 | 0 |
                List<Object> data = new ArrayList<Object>(row.size());

                // add name
                data.add(row.get(0));

                // now skip through all the numbers
                for (int pos = 1; pos < row.size(); pos++) {
                    // get values
                    Integer prevValue = (Integer)rowPrev.get(pos);
                    Integer currentValue = (Integer)row.get(pos);
                    Integer diff = 0;

                    if (prevValue > currentValue) {
                        // a data oddity
                        System.err.println("Data error: " + row.get(0)
                                           + " previous valueof "
                                           + table.getColumnDefinitions().get(pos).getName()
                                           + " = " + prevValue + " > current value: "
                                           + currentValue);
                    } else {
                        diff = currentValue - prevValue;
                    }

                    // add to data
                    data.add(diff);
                }

                return data;
            }
        }
    }

    /**
     * Find data for the row in a table for a Router
     */
    private List<Object> findRowData(Table table, String router) {
        // skip through all rows looking for routerDst
        int rows = table.getRowCount();

        // skip row 0, which is localhost
        for (int r = 1; r < rows; r++) {
            TableRow row = table.getRow(r);

            // get name
            TableValue tableValue = row.get(0);
            String linkName = (String)tableValue.getValue();

            // check name
            if (linkName.startsWith(router)) {
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

    /**
     * Calculate the traffic.
     * private
     *
     * /**
     * Tell this reporter that a router has been deleted
     */
    @Override
	public void routerDeleted(String routerName) {
        Table oldData = null;

        synchronized (measurements) {
            oldData = measurements.get(routerName);
            old.put(routerName, oldData);
            measurements.remove(routerName);
        }

        System.out.println(
            "Deleted router: " + routerName + " Lost data "
            + calculateTraffic(oldData));
    }

    /**
     * Calculate the traffic for a router
     */
    protected int calculateTraffic(Table table) {
        if (table == null) {
            return 0;
        } else {
            int volume = 0;

            int rows = table.getRowCount();

            // skip row 0, which is localhost
            for (int r = 1; r < rows; r++) {
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
    @SuppressWarnings("unused")
	private void printAnyDropped(String routerName, Table table) {
        int rows = table.getRowCount();

        for (int r = 0; r < rows; r++) {
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

            if ((inDropped > 0) || (outDropped > 0)) {
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
    @SuppressWarnings("unused")
	private void printTable(Table table) {
        System.out.println(tableToString(table, true, false));
    }

    private String tableToString(Table table, boolean withHeader, boolean withTime) {
        StringBuilder builder = new StringBuilder();

        // get the time
        long elapsed = globalController.getElapsedTime();

        // get no of cols
        int cols = table.getColumnCount();

        // output header
        if (withHeader) {
            if (withTime) {
                builder.append(globalController.elapsedToString(
                                   elapsed) + " ");
            }

            for (int c = 0; c < cols; c++) {
                TableAttribute headerAttr
                    = table.getColumnDefinitions().get(c);
                builder.append(headerAttr.getName() + " | ");
            }

            builder.append("\n");
        }

        // now print out values
        int rows = table.getRowCount();

        for (int r = 0; r < rows; r++) {
            if (withTime) {
                builder.append(globalController.elapsedToString(
                                   elapsed) + " ");
            }

            TableRow row = table.getRow(r);

            for (int c = 0; c < cols; c++) {
                TableValue tableValue = row.get(c);

                switch (c) {
                case 0: {                     // NetIF name col
                    if (tableValue.getValue().toString().endsWith(
                            "localnet")) {
                        builder.append(coloured(ANSI.MAGENTA,
                                                tableValue.getValue()));
                    } else {
                        builder.append(coloured(ANSI.BLUE,
                                                tableValue.getValue()));
                    }

                    builder.append(" | ");
                    break;
                }

                case 4:       // Dropped cols
                case 10:
                    Integer dropped = (Integer)tableValue.getValue();

                    if (dropped > 0) {
                        builder.append(ANSI.BRIGHT_COLOUR);
                        builder.append(coloured(ANSI.RED, dropped));
                        builder.append(ANSI.BRIGHT_OFF);
                    } else {
                        builder.append(dropped);
                    }

                    builder.append(" | ");
                    break;

                default:
                    builder.append(tableValue.getValue() + " | ");
                    break;
                }
            }

            builder.append("\n");
        }

        return builder.toString();
    }

    /**
     * Coloured text
     */
    private String coloured(String colour, Object text) {
        return colour + text + ANSI.RESET_COLOUR;
    }

    /**
     * Print AppList data
     */
    @SuppressWarnings("unused")
	private String appListToString(Table table) {
        StringBuilder builder = new StringBuilder();

        // get the time
        long elapsed = globalController.getTime();

        // get no of cols
        int cols = table.getColumnCount();

        // header
        builder.append(globalController.elapsedToString(elapsed) + " ");

        for (int c = 0; c < cols; c++) {
            TableAttribute headerAttr
                = table.getColumnDefinitions().get(c);
            builder.append(headerAttr.getName() + " | ");
        }

        builder.append("\n");

        // now print out values
        int rows = table.getRowCount();

        for (int r = 0; r < rows; r++) {
            builder.append(globalController.elapsedToString(
                               elapsed) + " ");

            TableRow row = table.getRow(r);

            for (int c = 0; c < cols; c++) {
                TableValue tableValue = row.get(c);
                builder.append(tableValue.getValue() + " | ");
            }

            builder.append("\n");
        }

        return builder.toString();
    }

}
