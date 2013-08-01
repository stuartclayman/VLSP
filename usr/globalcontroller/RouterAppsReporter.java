// RouterAppsReporter.java

package usr.globalcontroller;

import usr.logging.*;
import usr.common.ANSI;
import usr.common.BasicRouterInfo;
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.table.*;
import eu.reservoir.monitoring.core.list.MList;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A RouterAppsReporter collects measurements sent by
 * a AppListProbe embedded in each Router.
 * It shows the apps running on a router.
 */
public class RouterAppsReporter implements Reporter {
    GlobalController globalController;


    // count of no of measurements
    int count = 0;

    /**
     * Constructor
     */
    public RouterAppsReporter(GlobalController gc) {
        globalController = gc;
    }

    /**
     * This collects each measurement and processes it.
     * Each measurement has the following structure:
     * ProbeValue 0: String =
     * Router-1
     * ProbeValue 1: Table =
     * ID | StartTime | RunTime | State | ClassName | Args | Name | RuntimeKeys | RuntimeValues
     * 1 | 1331298150361 | 10000 | RUNNING | usr.applications.Send | [4, 3000, 250000, -d, 250, -i, 10] | /R1/App/usr.applications.Send/1 | [] | []
     */
    public void report(Measurement m) {
        if (m.getType().equals("AppList")) {

            List<ProbeValue> values = m.getValues();

            // ProbeValue 0 is the router name
            ProbeValue pv0 = values.get(0);
            String routerName = (String)pv0.getValue();

            // ProbeValue 1 is the table
            ProbeValue pv1 = values.get(1);
            Table table = (Table)pv1.getValue();

            // print out to channel 10
            //Logger.getLogger("log").logln(USR.STDOUT, routerName + ":\n" + appListToString(table));

            /*
             * Patch values into BasicRouterInfo table
             */

            // find the BasicRouterInfo for the named router
            BasicRouterInfo routerInfo = globalController.findRouterInfo(routerName);

            if (routerInfo == null) {
                Logger.getLogger("log").logln(USR.ERROR, routerName + ": has no BasicRouterInfo. Probably shutdown");
            } else {
                updateRouterAppInfo(routerInfo, table);
            }

        } else {
            // not what we were expecting
        }
    }

    /**
     * Update the router info for the apps.
     */
    private void updateRouterAppInfo(BasicRouterInfo routerInfo, Table table) {
        // get no of cols
        int cols = table.getColumnCount();

        TableHeader header = table.getColumnDefinitions();

        // now process rows
        int rows = table.getRowCount();

        for (int r = 0; r< rows; r++) {
            TableRow row = table.getRow(r);

            // Name is field 6
            String name = (String)row.get(6).getValue();

            // now fill in infoMap
            HashMap<String, Object> infoMap = new HashMap<String, Object>();

            // visit every column except 6
            // and the last 2
            for (int c = 0; c< (cols-2); c++) {
                if (c == 6) {
                    continue;
                } else {
                    infoMap.put(header.get(c).getName(), row.get(c).getValue());

                }
            }

            // now convert last 2 lists into a map
            HashMap<String, String> appMonitoringData = new HashMap<String, String>();

            MList keys = (MList)row.get(7).getValue();
            MList values = (MList)row.get(8).getValue();

            for (int pos = 0; pos < keys.size(); pos++) {
                appMonitoringData.put((String)keys.get(pos).getValue(), (String)values.get(pos).getValue());
            }
            // add the map into the infoMap
            infoMap.put("MonitoringData", appMonitoringData);

            // and save into BasicRouterInfo
            routerInfo.setApplicationData(name, infoMap);
        }
    }

    /**
     * Print AppList data
     */
    private String appListToString(Table table) {
        StringBuilder builder = new StringBuilder();

        // get the time
        long startTime = globalController.getStartTime();
        long elapsed = System.currentTimeMillis() - startTime;

        // get no of cols
        int cols = table.getColumnCount();

        // header
        builder.append(globalController.elapsedToString(elapsed) + " ");

        for (int c = 0; c<cols; c++) {
            TableAttribute headerAttr = table.getColumnDefinitions().get(c);
            builder.append(headerAttr.getName() + " | ");
        }

        builder.append("\n");



        // now print out values
        int rows = table.getRowCount();

        for (int r = 0; r< rows; r++) {
            builder.append(globalController.elapsedToString(elapsed) + " ");

            TableRow row = table.getRow(r);

            for (int c = 0; c<cols; c++) {
                TableValue tableValue = row.get(c);
                builder.append(tableValue.getValue() + " | ");
            }
            builder.append("\n");
        }


        return builder.toString();

    }

}
