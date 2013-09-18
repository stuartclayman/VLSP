package usr.router.command;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;
import usr.router.RoutingTable;
import usr.router.RoutingTableEntry;

/**
 * The LIST_ROUTING_TABLE command.
 */
public class ListRoutingTableCommand extends RouterCommand {
    /**
     * Construct a ListRoutingTableCommand
     */
    public ListRoutingTableCommand() {
        super(MCRP.LIST_ROUTING_TABLE.CMD, MCRP.LIST_ROUTING_TABLE.CODE,
              MCRP.LIST_ROUTING_TABLE.CODE);
    }

    /**
     * Evaluate the Command.
     */
    @Override
	public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            JSONObject jsobj = new JSONObject();
            JSONArray array = new JSONArray();

            RoutingTable table = controller.getRoutingTable();

            Collection<? extends RoutingTableEntry> c = table.getEntries();

            for (RoutingTableEntry e : c) {
                if (e.getNetIF() == null) {
                    // its the local NetIF
                    array.put(e.toString()  + " localnet");
                } else {
                    array.put(e.toString());
                }
            }

            jsobj.put("list", array);
            jsobj.put("size", table.size());

            out.println(jsobj.toString());
            response.close();

            return true;


        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        return false;

    }

}