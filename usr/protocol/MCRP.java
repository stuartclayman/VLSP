package usr.protocol;

/**
 * Request strings and response codes for the MCRP protocol.
 */
public class MCRP implements Protocol {
    // This contains 4 lots of commands
    // Generic
    // Router
    // GlobalController
    // LocalController

    /*
     * Generic commands - 200 - 219
     */

    /**
     * Quit talking to the Component.
     * Close a connection to the ManagementConsole of the Component.
     */
    public final static class QUIT {
        public final static String CMD = "QUIT";
        public final static int CODE = 200;
        public final static int ERROR = 400;
    }

    /**
     * Shut down this Controller.
     */
    public final static class SHUT_DOWN {
        public final static String CMD = "SHUT_DOWN";
        public final static int CODE = 201;
        public final static int ERROR = 401;
    }

    /**
     * MCRP request for a Router
     START_APP router_id className args
     e.g. START_APP 1 usr.applications.Ping 2
    */
    public final static class START_APP {
        public final static String CMD = "START_APP";
        public final static int CODE = 202;
        public final static int ERROR = 401;
    }

    /**
     * Status of this Controller.
     */
    public final static class STATUS {
        public final static String CMD = "STATUS";
        public final static int CODE = 203;
        public final static int ERROR = 401;
    }

    /*
     * Router Commands - 220 - 249
     */

    /**
     * Get the name of the router.
     */
    public final static class GET_NAME {
        public final static String CMD = "GET_NAME";
        public final static int CODE = 221;
    }

    /**
     * Set the name of the router.
     */
    public final static class SET_NAME {
        public final static String CMD = "SET_NAME";
        public final static int CODE = 222;
    }

    /**
     * Get the port number to connect to on the router
     * in order to make a new router-to-router data connection.
     */
    public final static class GET_CONNECTION_PORT {
        public final static String CMD = "GET_CONNECTION_PORT";
        public final static int CODE = 223;
    }

    /**
     * List all the router-to-router connections that the router has.
     */
    public final static class LIST_CONNECTIONS {
        public final static String CMD = "LIST_CONNECTIONS";
        public final static int CODE = 224;
    }

    /**
     * Tell the router there has been an incoming connection
     * on the router-to-router port.
     */
    public final static class INCOMING_CONNECTION {
        public final static String CMD = "INCOMING_CONNECTION";
        public final static int CODE = 225;
        public final static int ERROR = 402;
    }

    /**
     * Get the global Address of the router.
     */
    public final static class GET_ROUTER_ADDRESS {
        public final static String CMD = "GET_ROUTER_ADDRESS";
        public final static int CODE = 226;
    }

    /**
     * Set the global Address of the router.
     */
    public final static class SET_ROUTER_ADDRESS {
        public final static String CMD = "SET_ROUTER_ADDRESS";
        public final static int CODE = 227;
    }

    /**
     * APP_START starts an app
     */
    public final static class APP_START {
        public final static String CMD = "APP_START";
        public final static int CODE = 228;
    }

    /**
     * APP_STOP starts an app
     */
    public final static class APP_STOP {
        public final static String CMD = "APP_STOP";
        public final static int CODE = 229;
    }

    /**
     * APP_LIST lists apps
     */
    public final static class APP_LIST {
        public final static String CMD = "APP_LIST";
        public final static int CODE = 230;
    }

    /**
     * MONITORING_START starts an monitoring
     */
    public final static class MONITORING_START {
        public final static String CMD = "MONITORING_START";
        public final static int CODE = 231;
    }

    /**
     * MONITORING_STOP starts an monitoring
     */
    public final static class MONITORING_STOP {
        public final static String CMD = "MONITORING_STOP";
        public final static int CODE = 232;
    }

    /**
     * GET_NETIF_STATS gets stats for each NetIF
     */
    public final static class GET_NETIF_STATS {
        public final static String CMD = "GET_NETIF_STATS";
        public final static int CODE = 237;
    }

    /**
     * GET_SOCKET_STATS gets stats for the sockets in the AppSocketMux
     */
    public final static class GET_SOCKET_STATS {
        public final static String CMD = "GET_SOCKET_STATS";
        public final static int CODE = 238;
    }

    /**
     * Create a new router-to-router data connection to another router.
     */
    public final static class CREATE_CONNECTION {
        public final static String CMD = "CREATE_CONNECTION";
        public final static int CODE = 239;
        public final static int ERROR = 401;
    }


    /*
     * Port of a Router
     */

    /**
     * Get the name of a port on the router.
     */
    public final static class GET_PORT_NAME {
        public final static String CMD = "GET_PORT_NAME";
        public final static int CODE = 241;
        public final static int ERROR = 403;
    }

    /**
     * Get the name of a remote router of a port on the router.
     */
    public final static class GET_PORT_REMOTE_ROUTER {
        public final static String CMD = "GET_PORT_REMOTE_ROUTER";
        public final static int CODE = 242;
        public final static int ERROR = 403;
    }

    /**
     * Get the address of a port on the router.
     */
    public final static class GET_PORT_ADDRESS {
        public final static String CMD = "GET_PORT_ADDRESS";
        public final static int CODE = 243;
        public final static int ERROR = 403;
    }

    /**
     * Set the address of a port on the router.
     */
    public final static class SET_PORT_ADDRESS {
        public final static String CMD = "SET_PORT_ADDRESS";
        public final static int CODE = 244;
        public final static int ERROR = 403;
    }


    /**
     * Get the weight of a port on the router.
     */
    public final static class GET_PORT_WEIGHT {
        public final static String CMD = "GET_PORT_WEIGHT";
        public final static int CODE = 245;
        public final static int ERROR = 403;
    }

    /**
     * Set the weight of a port on the router.
     */
    public final static class SET_PORT_WEIGHT {
        public final static String CMD = "SET_PORT_WEIGHT";
        public final static int CODE = 246;
        public final static int ERROR = 403;
    }


    /**
     * Get the address of a remote router of a port on the router.
     */
    public final static class GET_PORT_REMOTE_ADDRESS {
        public final static String CMD = "GET_PORT_REMOTE_ADDRESS";
        public final static int CODE = 247;
        public final static int ERROR = 403;
    }




    /*
     * Global Controller commands - 250 - 269
     */

    /**
     * Connect to and confirm existence of a local controller
     */

    public final static class OK_LOCAL_CONTROLLER {
        public final static String CMD = "OK_LOCAL_CONTROLLER";
        public final static int CODE = 250;
        public final static int ERROR = 410;
    }


    /**
     * Get the network connectivity graph managed by the GlobalController
     */
    public final static class NETWORK_GRAPH {
        public final static String CMD = "NETWORK_GRAPH";
        public final static int CODE = 251;
    }

    /*
     * Local Controller commands - 270 - 289
     */

    /**
     * Connect to and confirm existence of a local controller
     */

    public final static class CHECK_LOCAL_CONTROLLER {
        public final static String CMD = "CHECK_LOCAL_CONTROLLER";
        public final static int CODE = 270;
        public final static int ERROR = 420;
    }

    /** Local controller starts router */

    public final static class NEW_ROUTER {
        public final static String CMD = "NEW_ROUTER";
        public final static int CODE = 271;
        public final static int ERROR = 421;
    }


    /** Local controller joins routers */

    public final static class CONNECT_ROUTERS {
        public final static String CMD = "CONNECT_ROUTERS";
        public final static int CODE = 272;
        public final static int ERROR = 422;
    }

    /**  Router lists routing table */

    public final static class LIST_ROUTING_TABLE {
        public final static String CMD = "LIST_ROUTING_TABLE";
        public final static int CODE = 273;
        public final static int ERROR = 423;
    }

    /**  Request to Local Controller to shut down a router */

    public final static class ROUTER_SHUT_DOWN {
        public final static String CMD = "ROUTER_SHUT_DOWN";
        public final static int CODE = 274;
        public final static int ERROR = 424;
    }

    /**  Request to Local Controller to end a link */

    public final static class END_LINK {
        public final static String CMD = "END_LINK";
        public final static int CODE = 275;
        public final static int ERROR = 425;
    }

    /**  Request to router to read an options string */

    public final static class READ_OPTIONS_STRING {
        public final static String CMD = "READ_OPTIONS_STRING";
        public final static int CODE = 276;
        public final static int ERROR = 426;
    }

    /**  Request to router to read an options string */

    public final static class READ_OPTIONS_FILE {
        public final static String CMD = "READ_OPTIONS_FILE";
        public final static int CODE = 277;
        public final static int ERROR = 427;
    }

    /**  Router configuration string to send to local controller */

    public final static class ROUTER_CONFIG {
        public final static String CMD = "ROUTER_CONFIG";
        public final static int CODE = 278;
        public final static int ERROR = 428;
    }

    /**  Ping all neighbours */
    public final static class PING_NEIGHBOURS {
        public final static String CMD = "PING_NEIGHBOURS";
        public final static int CODE = 279;
        public final static int ERROR = 429;
    }

    /**  Ping all neighbours */
    public final static class PING {
        public final static String CMD = "PING";
        public final static int CODE = 280;
        public final static int ERROR = 430;
    }

    /** Echo -- ping response */
    public final static class ECHO {
        public final static String CMD = "ECHO";
        public final static int CODE = 281;
        public final static int ERROR = 431;
    }

    /** Echo -- ping response */
    public final static class RUN {
        public final static String CMD = "RUN";
        public final static int CODE = 282;
        public final static int ERROR = 432;
    }

    /** Set AP status */
    public final static class SET_AP {
        public final static String CMD = "SET_AP";
        public final static int CODE = 283;
        public final static int ERROR = 433;
    }

    /** Report AP status */
    public final static class REPORT_AP {
        public final static String CMD = "REPORT_AP";
        public final static int CODE = 284;
        public final static int ERROR = 434;
    }

    /**
     * GET_ROUTER_STATS gets stats for each Router
     */
    public final static class GET_ROUTER_STATS {
        public final static String CMD = "GET_ROUTER_STATS";
        public final static int CODE = 285;
    }

    /**
     * REQUEST_ROUTER_STATS gets stats for each Router
     */
    public final static class REQUEST_ROUTER_STATS {
        public final static String CMD = "REQUEST_ROUTER_STATS";
        public final static int CODE = 286;
    }

    /**
     * RETURN_ROUTER_STATS gets stats for each Router
     */
    public final static class SEND_ROUTER_STATS {
        public final static String CMD = "SEND_ROUTER_STATS";
        public final static int CODE = 287;
    }

    /**
     * ROUTER_OK -- check router on
     */
    public final static class ROUTER_OK {
        public final static String CMD = "ROUTER_OK";
        public final static int CODE = 288;
    }

    /**  Request to Local Controller to set a link weight */

    public final static class SET_LINK_WEIGHT {
        public final static String CMD = "SET_LINK_WEIGHT";
        public final static int CODE = 289;
    }



    /*
     * Spare - 290 - 299
     */

    /**
     * The standard error code.
     */
    public final static class ERROR {
        // can get to the standard error code
        // via CODE or ERROR, for convenience
        public final static int CODE = 400;
        public final static int ERROR = 400;
    }


}
