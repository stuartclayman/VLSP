// NetIFStatsReporter.java

package ikms.processor;

import ikms.util.ANSI;

import java.util.HashMap;
import java.util.List;

import com.timeindexing.index.IndexView;
import com.timeindexing.index.TimeIndexException;

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
 * It then saves these measurements in a Timeindex data store,
 * with one Timeindex per Router.
 */
public class NetIFStatsReporter implements Reporter {
    // A HashMap of RouterName -> latest measurement
    HashMap<String, Table> measurements;

  // A HashMap of RouterName -> timeindex measurement store
    TimeIndexMap dataStore;


    // start time
    long startTime = 0;

    // count of no of measurements
    int count = 0;

    /**
     * Constructor
     */
    public NetIFStatsReporter() {
        measurements = new HashMap<String, Table>();
        dataStore = new TimeIndexMap();

        startTime = System.currentTimeMillis();
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


            synchronized (dataStore) {
                try {
                    dataStore.put(routerName, table);
                } catch (TimeIndexException tie) {
                    System.err.println("Failed to add to index " + routerName +
                                       " with error " + tie.getMessage());
                }
            }


            //Logger.getLogger("log").logln(1<<7, tableToString(table, false, true));

            // print out total every 100 measurements

            // print out total every 100 measurements
            if (count % 100 == 0) {
                System.out.println("* [" + count + "] ");

                for (String router : dataStore.keySet()) {
                    IndexView index = dataStore.get(router);

                    if (index == null) {
                        System.out.println("TimeIndexMap cant find " + router);
                    } else {
                        index.locate(index.getEndTime());
                    }
                }
            }

        } else {
            // not what we handle
        }
    }





    /**
     * Tell this reporter that a router has been deleted
     */
    public void routerDeleted(String routerName) {
        @SuppressWarnings("unused")
		Table oldData = null;

        synchronized (measurements) {
            oldData = measurements.get(routerName);
            measurements.remove(routerName);
        }

        //System.out.println("Deleted router: " + routerName + " Lost data " + calculateTraffic(oldData));

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
        long elapsed = System.currentTimeMillis() - startTime;

        // get no of cols
        int cols = table.getColumnCount();

        // output header
        if (withHeader) {
            if (withTime) {
                builder.append(elapsedToString(elapsed) + " ");
            }

            for (int c=0; c<cols; c++) {
                TableAttribute headerAttr = table.getColumnDefinitions().get(c);
                builder.append(headerAttr.getName() + " | ");
            }

            builder.append("\n");
        }

        // now print out values
        int rows = table.getRowCount();

        for (int r=0; r< rows; r++) {
            if (withTime) {
                builder.append(elapsedToString(elapsed) + " ");
            }

            TableRow row = table.getRow(r);

            for (int c=0; c<cols; c++) {
                TableValue tableValue = row.get(c);

                switch (c) {
                case 0: {        // NetIF name col
                    if (tableValue.getValue().toString().endsWith("localnet")) {
                        builder.append(coloured(ANSI.MAGENTA, tableValue.getValue()));
                    } else {
                        builder.append(coloured(ANSI.BLUE,tableValue.getValue()));
                    }
                    builder.append(" | ");    
                    break;
                }

                case 4:         // Dropped cols
                case 10:
                    Integer dropped = (Integer)tableValue.getValue();
                    if (dropped > 0) {
                        builder.append(ANSI.BRIGHT_COLOUR);
                        builder.append(coloured(ANSI.RED,dropped));
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
     * Convert an elasped time, in milliseconds, into a string.
     * Converts something like 35432 into 35:43
     */
    public String elapsedToString(long elapsedTime) {
        long millis = (elapsedTime % 1000) / 10;

        long rawSeconds = elapsedTime / 1000;
        long seconds = rawSeconds % 60;
        long minutes = rawSeconds / 60;

        StringBuilder builder = new StringBuilder();

        if (minutes < 10) {
            builder.append("0");
        }
        builder.append(minutes);

        builder.append(":");
        if (seconds < 10) {
            builder.append("0");
        }
        builder.append(seconds);

        builder.append(":");
        if (millis < 10) {
            builder.append("0");
        }
        builder.append(millis);


        return builder.toString();
        
    }

}
