package usr.router;

/**
 * Request strings and response codes for the MCRP protocol.
 */
public class MCRP {
    /**
     * Quit talking to the router
     * Close a connection to the ManagementConsole of the router.
     */
    public final static class QUIT {
        public final static String CMD =  "QUIT";
        public final static int CODE = 200;
    }

    /**
     * Get the name of the router.
     */
    public final static class GET_NAME {
        public final static String CMD =  "GET_NAME";
        public final static int CODE = 201;
    }

    /**
     * Set the name of the router.
     */
    public final static class SET_NAME {
        public final static String CMD =  "SET_NAME";
        public final static int CODE = 202;
    }

    /**
     * Get the port number to connect to on the router
     * in order to make a new router-to-router data connection.
     */
    public final static class GET_CONNECTION_PORT {
        public final static String CMD =  "GET_CONNECTION_PORT";
        public final static int CODE = 203;
    }

    /**
     * Get the address of a port on the router.
     */
    public final static class GET_ADDRESS {
        public final static String CMD =  "GET_ADDRESS";
        public final static int CODE = 206;
        public final static int ERROR = 403;
    }

    /**
     * Set the address of a port on the router.
     */
    public final static class SET_ADDRESS {
        public final static String CMD =  "SET_ADDRESS";
        public final static int CODE = 207;
        public final static int ERROR = 403;
    }

    /**
     * Tell the router there has been an incoming connection
     * on the router-to-router port.
     */
    public final static class INCOMING_CONNECTION {
        public final static String CMD =  "INCOMING_CONNECTION";
        public final static int CODE = 204;
        public final static int ERROR = 402;
    }

    /**
     * List all the router-to-router connections that the router has.
     */
    public final static class LIST_CONNECTIONS {
        public final static String CMD =  "LIST_CONNECTIONS";
        public final static int CODE = 205;
    }

    /**
     * Create a new router-to-router data connection to another router.
     */
    public final static class CREATE_CONNECTION {
        public final static String CMD =  "CREATE_CONNECTION";
        public final static int CODE = 299;
        public final static int ERROR = 401;
    }

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

