// NetIFStatsReporter.java

package usr.globalcontroller;

import usr.router.*;
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.table.*;
import java.util.List;
import java.util.ArrayList;

/**
 * A NetIFStatsReporter collects measurements sent by
 * a NetIFStatsProbe embedded in each Router.
 */
public class NetIFStatsReporter implements Reporter {
    GlobalController globalController;

    /**
     * Constructor
     */
    public NetIFStatsReporter(GlobalController gc) {
        globalController = gc;
    }

    /**
     * This collects each measurement and processes it.
     */
    public void report(Measurement m) {
        if (m.getType().equals("NetIFStats")) {
            List<ProbeValue> values = m.getValues();

            // PV 0 is the router name
            ProbeValue pv0 = values.get(0);
            String routerName = (String)pv0.getValue();

            // PV 1 is the table
            ProbeValue pv1 = values.get(1);
            Table table = (Table)pv1.getValue();

            // print out table
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

        } else {
            // not what we were expecting
        }
    }

}
