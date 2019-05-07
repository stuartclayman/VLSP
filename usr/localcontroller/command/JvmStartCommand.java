package usr.localcontroller.command;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.protocol.MCRP;
import usr.common.BasicJvmInfo;

/**
 * The NEW_JVM command.
 */
public class JvmStartCommand extends LocalCommand {
    /**
     * Construct a JvmStartCommand.
     */
    public JvmStartCommand() {
        super(MCRP.NEW_JVM.CMD);
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

            if (args.length < 3) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Expected three or more arguments for JVM_START Command: JVM_START jvm_id className args");

                out.println(jsobj.toString());
                response.close();

                return false;

            } else {
                Scanner sc = new Scanner(args[1]);
                int jvmID;

                if (sc.hasNextInt()) {
                    jvmID = sc.nextInt();
                    sc.close();
                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "Argument for NEW_JVM command must be int");

                    out.println(jsobj.toString());
                    response.close();
                    sc.close();
                    return false;
                }

                String className = args[2];

                // collect args
                String[] cmdArgs = new String[args.length - 3];

                for (int a = 3; a < args.length; a++) {
                    cmdArgs[a-3] = args[a];
                }


                // get controller to do work
                String jvmName = controller.newJvm(jvmID, className, cmdArgs);

                

                if (jvmName != null) {
                    if (jvmName == "") {
                        // It started fine, but finished quickly
                        JSONObject jsobj = new JSONObject();

                        jsobj.put("jvmID", jvmID);
                        jsobj.put("jvmName", jvmName);
                        jsobj.put("status", "completed");

                        out.println(jsobj.toString());
                        response.close();

                        return true;
                    } else {
                        // find BasicJvmInfo from controller map
                        // and return all relevant values
                        BasicJvmInfo jvmi = controller.findJvmInfo(jvmID);

                        JSONObject jsobj = new JSONObject();

                        jsobj.put("jvmID", jvmi.getId());
                        jsobj.put("name", jvmi.getName());
                        jsobj.put("jvmName", jvmName);
                        jsobj.put("status", "running");

                        out.println(jsobj.toString());
                        response.close();

                        return true;
                    }
                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "JVM_START. ERROR with " + value);

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
