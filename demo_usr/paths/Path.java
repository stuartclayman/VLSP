package demo_usr.paths;

import java.util.ArrayList;
import java.util.List;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.vim.VimClient;

/**
 * A represention of a Path across a virtual network.
 * A Path is not just a simple point to point line.
 * A path can have multiple elements.
 * Each path uses one specific USR port internally and can have
 * many input UDP ports and many output UDP addresses.
 */
public class Path {
    // The port for this path
    int usrPort;

    // A list of IngressNodes
    List<IngressNode> ingressNodes;

    // A list of EgressNodes
    List<EgressNode> egressNodes;

    // A list of ForwardNodes
    List<ForwardNode> forwardNodes;

    // The VIM NEM that will activate this path
    VimClient vim;

    /**
     * Construct a Path with the specified internal USR port
     */
    public Path(int port, VimClient vim) {
        usrPort = port;
        ingressNodes = new ArrayList<IngressNode>();
        egressNodes = new ArrayList<EgressNode>();
        forwardNodes = new ArrayList<ForwardNode>();
        this.vim = vim;
    }

    /**
     * An Ingress node is setup on router with ID 'routerID' and listens
     * on UDP port 'udpPort'. All data is sent to USR port 'port' on
     * router 'forwardID'
     */
    public JSONObject addIngress(int routerID, int udpPort, int forwardID) throws JSONException {
        // check if there is already an Ingress node like this
        for (IngressNode n : ingressNodes) {
            if (n.router == routerID) {
                // the node already exists
                throw new Error("IngressNode on router: " +  routerID + " already listening on port " + udpPort);
            }
        }
        // if we get here it is ok
        IngressNode node = new IngressNode(routerID, udpPort, forwardID);

        ingressNodes.add(node);


        // now activate

         JSONObject jsobj = vim.createApp(routerID, "demo_usr.paths.Ingress", udpPort + " " + forwardID + ":" + usrPort + " -b 32 -v"); 

        jsobj.put("result", "IngressNode-" + routerID + "/" + udpPort + "/" + forwardID);

        return jsobj;
    }


    /**
     * An Egress node is setup on router with ID 'routerID' and
     * listens on USR port 'port'.  All data is sent to UDP address
     * 'udpAddr'
     */
    public JSONObject addEgress(int routerID, String udpAddr) throws JSONException {
        // check if there is already an Egress node like this
        for (EgressNode n : egressNodes) {
            if (n.router == routerID) {
                // the node already exists
                throw new Error("EgressNode on router: " +  routerID + " already sending to address " + udpAddr);
            }
        }
        // if we get here it is ok
        EgressNode node = new EgressNode(routerID, udpAddr);

        egressNodes.add(node);

        // now activate
        JSONObject jsobj = vim.createApp(routerID, "demo_usr.paths.Egress", usrPort + " " + udpAddr + " -v");

        jsobj.put("result", "IngressNode-" + routerID + "/" + udpAddr);

        return jsobj;
    }



    /**
     * An Forward node is setup on router with ID 'routerID'.  It
     * listens for data on USR port 'port' and forwards to router with ID
     * 'forwardID' on USR port 'port'
     */
    public JSONObject addForward(int routerID, int forwardID) throws JSONException {
        // check if there is already an Forward node like this
        for (ForwardNode n : forwardNodes) {
            if (n.router == routerID) {
                // the node already exists
                throw new Error("ForwardNode on router: " +  routerID + " already forwarding to " + forwardID);
            }
        }
        // if we get here it is ok
        ForwardNode node = new ForwardNode(routerID, forwardID);

        forwardNodes.add(node);

        JSONObject jsobj = vim.createApp(routerID, "demo_usr.paths.Forward", usrPort + " " + forwardID + ":" + usrPort + " -v"); 
        jsobj.put("result", "ForwardNode-" + routerID + "/" + forwardID);

        return jsobj;
    }


    /**
     * Adapts an Ingress node on router with ID 'routerID' which listens on
     * UDP port 'udpPort'. All data is sent to USR port 'port' on router
     * 'newForwardID' 
     */
    public JSONObject adaptIngress(int routerID, int newForwardID) throws JSONException {
        IngressNode adaptee = null;

        // check if there is already an Ingress node like this
        for (IngressNode n : ingressNodes) {
            if (n.router == routerID) {
                // the node already exists
                adaptee = n;
            }
        }

        if (adaptee == null) {
            // if we get here it is bad
            throw new Error("No IngressNode on router: " +  routerID + " listening");
        } else {

            // reset forwardID
            adaptee.forward = newForwardID;

            // mgmt port 
            int mgmtPort = (usrPort + 20000) % 32768;

            JSONObject jsobj = vim.createApp(routerID, "demo_usr.paths.AdaptIngress", routerID + ":" + mgmtPort + " " + newForwardID + ":" + usrPort); 

            jsobj.put("result", "IngressNode-" + adaptee.router + "/" + adaptee.udpPort + "/" + adaptee.forward);

            return jsobj;
        }
    }


    /**
     * Adapts an Egress node on router with ID 'routerID' and listens
     * on USR port 'port'.  All data is sent to UDP address 'udpAddr'
     */
    public JSONObject adaptEgress(int routerID, String newUdpAddr) throws JSONException {
        EgressNode adaptee = null;

        // check if there is already an Egress node like this
        for (EgressNode n : egressNodes) {
            if (n.router == routerID) {
                // the node already exists
                adaptee = n;
            }
        }

        if (adaptee == null) {
            // if we get here it is bad
            throw new Error("No EgressNode on router: " +  routerID + " sending");
        } else {

            // reset forwardID
            adaptee.udpAddr = newUdpAddr;

            // (create-app 15 "demo_usr.paths.AdaptEgress" "15:24000 195.134.65.214:1234")

            // mgmt port 
            int mgmtPort = (usrPort + 20000) % 32768;

            JSONObject jsobj = vim.createApp(routerID, "demo_usr.paths.AdaptEgress", routerID + ":" + mgmtPort + " " + newUdpAddr); 

            jsobj.put("result", "EgressNode-" + adaptee.router + "/" + newUdpAddr);

            return jsobj;
        }
    }


    /**
     * Adapts an Forward node on router with ID 'routerID'.  It
     * listens for data on USR port 'port' and forwards to router with
     * ID 'newForwardID' on USR port 'port'
     */
    public JSONObject adaptForward(int routerID, int newForwardID) throws JSONException {
        ForwardNode adaptee = null;

        // check if there is already an Forward node like this
        for (ForwardNode n : forwardNodes) {
            if (n.router == routerID) {
                // the node already exists
                adaptee = n;
            }
        }

        if (adaptee == null) {
            // if we get here it is bad
            throw new Error("No ForwardNode on router: " +  routerID + " forwarding");
        } else {

            // reset forwardID
            adaptee.forward = newForwardID;


            // mgmt port 
            int mgmtPort = (usrPort + 20000) % 32768;

            JSONObject jsobj = vim.createApp(routerID, "demo_usr.paths.AdaptForward", routerID + ":" + mgmtPort + " " + newForwardID + ":" + usrPort); 

            jsobj.put("result", "ForwardNode-" + adaptee.router + "/" + adaptee.forward);

            return jsobj;
        }
    }


}


/**
 * An internal represention of an Ingress node
 */
class IngressNode {
    public int router;
    public int udpPort;
    public int forward;

    public IngressNode(int router, int udpPort, int forward) {
        this.router = router;
        this.udpPort = udpPort;
        this.forward = forward;
    }
}



/**
 * An internal represention of an Egress node
 */
class EgressNode {
    public int router;
    public String udpAddr;

    public EgressNode(int router, String udpAddr) {
        this.router = router;
        this.udpAddr = udpAddr;
    }
}


/**
 * An internal represention of an Forward node
 */
class ForwardNode {
    public int router;
    public int forward;

    public ForwardNode(int router, int forward) {
        this.router = router;
        this.forward = forward;
    }
}

