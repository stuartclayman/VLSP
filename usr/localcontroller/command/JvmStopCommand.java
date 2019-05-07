package usr.localcontroller.command;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.BasicJvmInfo;
import usr.common.LocalHostInfo;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;

/**
 * The STOP_JVM command.
 */
public class JvmStopCommand extends LocalCommand {
    /**
     * Construct a JvmStopCommand.
     */
    public JvmStopCommand() {
        super(MCRP.STOP_JVM.CMD);
    }

    /**
     * Evaluate the Command.
     */
    @Override
    public boolean evaluate(Request request, Response response) {

        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);

            String [] args = value.split(" ");

            if (args.length != 2) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Expected argument for End Jvm Command");

                out.println(jsobj.toString());
                response.close();

                return false;
            } else {
                int jvmID;

                Scanner sc = new Scanner(args[1]);

                if (sc.hasNextInt()) {
                    jvmID = sc.nextInt();
                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "Argument for STOP_JVM command must be int");

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }
           

            

                // end jvm
                // find BasicJvmInfo from controller
                // based on LocalHostInfo port
                BasicJvmInfo bji = controller.findJvmInfo(jvmID);

                if (controller.endJvm(bji)) {
                    JSONObject jsobj = new JSONObject();

                    jsobj.put("jvmID", bji.getId());
                    jsobj.put("msg", "JVM ENDED "+ bji);
                    jsobj.put("success", Boolean.TRUE);
                    out.println(jsobj.toString());
                    response.close();

                    return true;
                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "CANNOT END JVM "+bji);

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }
            }
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }
        return false;
    }

}
