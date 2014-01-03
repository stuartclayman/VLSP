package ikms.data;

/**
 * A ManagementConsole listens for connections
 * for doing component management.
 */
public interface ManagementConsole  {
    /**
     * Start the ManagementConsole.
     */
    public boolean start();

    /**
     * Stop the ManagementConsole.
     */
    public boolean stop();

    /**
     * Construct a ManagementConsole, given a specific port.
     */
    public void initialise (int port);


    public void registerCommands();

    /**
     * Define a handler for a request
     */
    public void defineRequestHandler(String pattern, RequestHandler rh);

}

