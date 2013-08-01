package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.applications.ApplicationHandle;
import java.util.Collection;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;

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

        finally {
            return false;
        }
    }

}