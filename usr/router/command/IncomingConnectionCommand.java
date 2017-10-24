package usr.router.command;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.Scanner;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.protocol.MCRP;
import usr.router.NetIF;
import usr.router.RouterPort;


/**
 * The INCOMING_CONNECTION command.
 * INCOMING_CONNECTION connectionID routerName routerID weight connection-hash-code end-point-host end-point-port
 * INCOMING_CONNECTION /Router283836798/Connection-1 Router283836798 4132 20 573523232 127.0.0.1 54125
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
    @Override
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

            if (parts.length == 7) {

                String connectionID = parts[0];
                String remoteRouterName = parts[1];
                String remoteRouterID = parts[2];
                String weightStr = parts[3];
                String connectionHashCodeStr = parts[4];
                String endPointHostStr = parts[5];
                String endPointPortStr = parts[6];

                Scanner scanner;

                // get connectionHashCode
                scanner = new Scanner(connectionHashCodeStr);
                int connectionHashCode;

                try {
                    connectionHashCode = scanner.nextInt();
                    scanner.close();
                } catch (Exception e) {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", getName() + " bad hash code");

                    out.println(jsobj.toString());
                    response.close();

                    return false;
                }

                Logger.getLogger("log").logln(USR.STDOUT, "IncomingConnectionCommand hashCode => " + " # " + connectionHashCode);

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
                    scanner.close();

                    return false;
                }

                // Get the remote host end point info


                InetAddress endPointHost = null;

                try {
                    endPointHost = InetAddress.getByName(endPointHostStr);
                }  catch (Exception e) {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", getName() + " invalid value for endPointHost " + endPointHostStr);

                    out.println(jsobj.toString());
                    response.close();
                    scanner.close();

                    return false;
                }

                // and now end point port

                scanner = new Scanner(endPointPortStr);
                int endPointPort = 0;

                try {
                    endPointPort = scanner.nextInt();
                } catch (Exception e) {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", getName() + " invalid value for endPointPort " + endPointPortStr);

                    out.println(jsobj.toString());
                    response.close();
                    scanner.close();

                    return false;
                }


                String remoteAddr = remoteRouterID;


                /*
                 * Lookup netif and set its name
                 */
                NetIF netIF = controller.getTemporaryNetIFByID(connectionHashCode);

                if (netIF != null) {
                    Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Found temporary NetIF with id " + connectionHashCode);

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
                        scanner.close();

                        return false;
                    }

                    netIF.setRemoteRouterAddress(addr);

                    // set the remote actually Address
                    netIF.setRemoteAddress(endPointHost, endPointPort);

                    Logger.getLogger("log").logln(USR.STDOUT, leadin() + "endPointHostStr = " + endPointHostStr + " endPointPortStr = " + endPointPortStr + " endPointHost = " + endPointHost + " endPointPort = " + endPointPort);


                    Logger.getLogger("log").logln(USR.STDOUT, leadin() + "NetIF: " + netIF.getLocalAddress() + ":" + netIF.getLocalPort() +  " <-> " + netIF.getInetAddress() + ":" + netIF.getPort() );
                
                    // netif attributes all set




                    // now plug netIF into Router
                    RouterPort routerPort = controller.plugTemporaryNetIFIntoPort(netIF);

                    Logger.getLogger("log").logln(USR.STDOUT, leadin() + "plugged netIF into RouterPort " + routerPort.getPortNo());

                    JSONObject jsobj = new JSONObject();

                    jsobj.put("name", connectionID);
                    jsobj.put("weight", weight);
                    jsobj.put("address", controller.getAddress().asTransmitForm());
                    jsobj.put("remoteAddress", addr.asTransmitForm());
                    jsobj.put("remoteName", remoteRouterName);
                    jsobj.put("routerPort", routerPort.getPortNo());

                    out.println(jsobj.toString());
                    response.close();
                    scanner.close();
                    return true;

                } else {
                    response.setCode(302);

                    JSONObject jsobj = new JSONObject();
                    jsobj.put("error", "Cannot find NetIF for hashCode " + connectionHashCode);

                    out.println(jsobj.toString());
                    response.close();
                    scanner.close();
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
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());
        }

        return false;

    }

}
