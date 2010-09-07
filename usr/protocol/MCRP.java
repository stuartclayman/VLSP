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
        public final static String CMD =  "QUIT";
        public final static int CODE = 200;
        public final static int ERROR = 400;
    }

    /**
     * Shut down this Controller.
     */
    public final static class SHUT_DOWN {
        public final static String CMD =  "SHUT_DOWN";
        public final static int CODE = 201;
        public final static int ERROR = 401;
    }

    /*
     * Router Commands - 220 - 249
     */

    /**
     * Get the name of the router.
     */
    public final static class GET_NAME {
        public final static String CMD =  "GET_NAME";
        public final static int CODE = 221;
    }

    /**
     * Set the name of the router.
     */
    public final static class SET_NAME {
        public final static String CMD =  "SET_NAME";
        public final static int CODE = 222;
    }

    /**
     * Get the port number to connect to on the router
     * in order to make a new router-to-router data connection.
     */
    public final static class GET_CONNECTION_PORT {
        public final static String CMD =  "GET_CONNECTION_PORT";
        public final static int CODE = 223;
    }

    /**
     * List all the router-to-router connections that the router has.
     */
    public final static class LIST_CONNECTIONS {
        public final static String CMD =  "LIST_CONNECTIONS";
        public final static int CODE = 224;
    }

    /**
     * Tell the router there has been an incoming connection
     * on the router-to-router port.
     */
    public final static class INCOMING_CONNECTION {
        public final static String CMD =  "INCOMING_CONNECTION";
        public final static int CODE = 225;
        public final static int ERROR = 402;
    }

    /**
     * Create a new router-to-router data connection to another router.
     */
    public final static class CREATE_CONNECTION {
        public final static String CMD =  "CREATE_CONNECTION";
        public final static int CODE = 249;
        public final static int ERROR = 401;
    }


    /*
     * Port of a Router
     */

    /**
     * Get the name of a port on the router.
     */
    public final static class GET_PORT_NAME {
        public final static String CMD =  "GET_PORT_NAME";
        public final static int CODE = 231;
        public final static int ERROR = 403;
    }

    /**
     * Get the name of a remote router of a port on the router.
     */
    public final static class GET_PORT_REMOTE_ROUTER {
        public final static String CMD =  "GET_PORT_REMOTE_ROUTER";
        public final static int CODE = 232;
        public final static int ERROR = 403;
    }

    /**
     * Get the address of a port on the router.
     */
    public final static class GET_PORT_ADDRESS {
        public final static String CMD =  "GET_PORT_ADDRESS";
        public final static int CODE = 233;
        public final static int ERROR = 403;
    }

    /**
     * Set the address of a port on the router.
     */
    public final static class SET_PORT_ADDRESS {
        public final static String CMD =  "SET_PORT_ADDRESS";
        public final static int CODE = 234;
        public final static int ERROR = 403;
    }

    
    /**
     * Get the weight of a port on the router.
     */
    public final static class GET_PORT_WEIGHT {
        public final static String CMD =  "GET_PORT_WEIGHT";
        public final static int CODE = 235;
        public final static int ERROR = 403;
    }

    /**
     * Set the weight of a port on the router.
     */
    public final static class SET_PORT_WEIGHT {
        public final static String CMD =  "SET_PORT_WEIGHT";
        public final static int CODE = 236;
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
       public final static int CODE= 250;
       public final static int ERROR = 410;
    }

    /*
     * Local Controller commands - 270 - 289
     */

    /** 
     * Connect to and confirm existence of a local controller
     */

    public final static class CHECK_LOCAL_CONTROLLER {
       public final static String CMD = "CHECK_LOCAL_CONTROLLER";
       public final static int CODE= 270;
       public final static int ERROR = 420;
    }

    /** Local controller starts router */
    
    public final static class NEW_ROUTER {
        public final static String CMD = "NEW_ROUTER";
        public final static int CODE= 271;
        public final static int ERROR= 421;
    }
   

    /** Local controller joins routers */
    
    public final static class CONNECT_ROUTERS {
        public final static String CMD = "CONNECT_ROUTERS";
        public final static int CODE= 272;
        public final static int ERROR= 422;
    }

  /**  Router lists routing table */
    
    public final static class LIST_ROUTING_TABLE {
        public final static String CMD = "LIST_ROUTING_TABLE";
        public final static int CODE= 273;
        public final static int ERROR= 423;
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

