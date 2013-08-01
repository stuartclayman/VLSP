package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;
import usr.router.NetIF;
import usr.net.AddressFactory;
import usr.net.Address;
import java.net.*;
import java.util.Scanner;


/**
 * The INCOMING_CONNECTION command.
 * INCOMING_CONNECTION connectionID routerName routerID weight TCP-port
 * INCOMING_CONNECTION /Router283836798/Connection-1 Router283836798 4132 20 57352
 */
public class IncomingConnectionCommand extends RouterCommand {
    /**
     * Construct a IncomingConnectionCommand
     */
    public IncomingConnectionCommand() {
        super(MCRP.INCOMING_CONNECTION.CMD, MCRP.INCOMING_CONNECTION.CODE, MCRP.INCOMING_CONNECTION.ERROR);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);
            // strip off COMMAND
            String args = value.substring(MCRP.INCOMING_CONNECTION.CMD.length()).trim();

            String[] parts = args.split(" ");

            if (parts.length == 5) {

                String connectionID = parts[0];
                String remoteRouterName = parts[1];
                String remoteRouterID = parts[2];
                String weightStr = parts[3];
                String remotePort = parts[4];

                Scanner scanner;

                // get remote port
                scanner = new Scanner(remotePort);
                int port;

                try {
                    port = scanner.nextInt();
                } catch (Exception e) {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", getName() + " bad port number");

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }

                // get connection weight
                scanner = new Scanner(weightStr);
                int weight = 0;

                try {
                    weight = scanner.nextInt();
                } catch (Exception e) {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", getName() + " invalid value for weight");

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }

                // create an address from the same host, but
                // using the passed in port number
                InetSocketAddress client = request.getClientAddress();
                InetSocketAddress refAddr = new InetSocketAddress(client.getAddress(), port);
                //Logger.getLogger("log").logln(USR.ERROR, "ManagementConsole => " + refAddr + " # " + refAddr.hashCode());

                String remoteAddr = remoteRouterID;


                /*
                 * Lookup netif and set its name
                 */
                NetIF netIF = controller.getTemporaryNetIFByID(refAddr.hashCode());

                if (netIF != null) {
                    Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Found temporary NetIF with id " + refAddr.hashCode());

                    // set its name
                    netIF.setName(connectionID);

                    // set its weight
                    netIF.setWeight(weight);
                    // set its Address
                    netIF.setAddress(controller.getAddress());
                    // set remote router
                    netIF.setRemoteRouterName(remoteRouterName);

                    // try and set remote router addresss
                    Address addr = null;
                    try {
                        addr = AddressFactory.newAddress(remoteAddr);
                    } catch (java.net.UnknownHostException e) {
                        Logger.getLogger("log").logln(USR.ERROR, leadin() + getName() + " failed");


                        response.setCode(302);

                        JSONObject jsobj = new JSONObject();
                        jsobj.put("error", "Cannot construct address from "+remoteAddr);

                        out.println(jsobj.toString());
                        response.close();

                        return false;
                    }

                    netIF.setRemoteRouterAddress(addr);

                    // netif attributes set

                    // now plug netIF into Router
                    controller.plugTemporaryNetIFIntoPort(netIF);

                    JSONObject jsobj = new JSONObject();

                    jsobj.put("name", connectionID);
                    jsobj.put("weight", weight);
                    jsobj.put("address", controller.getAddress().asTransmitForm());
                    jsobj.put("remoteAddress", addr.asTransmitForm());
                    jsobj.put("remoteName", remoteRouterName);

                    out.println(jsobj.toString());
                    response.close();

                    return true;

                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "Cannot find NetIF for port " + port);

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }
            } else {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", getName() + " wrong no of args ");

                out.println(jsobj.toString());
                response.close();

                return false;
            }

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