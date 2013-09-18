package usr.router.command;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.applications.ApplicationHandle;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;

/**
 * The APP_LIST command.
 */
public class AppListCommand extends RouterCommand {
    /**
     * Construct a AppListCommand
     */
    public AppListCommand() {
        super(MCRP.APP_LIST.CMD, MCRP.APP_LIST.CODE, MCRP.ERROR.CODE);
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

            Collection<ApplicationHandle> apps = controller.appList();

            int count = 0;

            for (ApplicationHandle appH : apps) {

                array.put(appH.getName());
                count++;
            }

            jsobj.put("list", array);
            jsobj.put("size", count);

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