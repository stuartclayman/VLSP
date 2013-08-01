package usr.console;

import usr.net.Address;
import usr.logging.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;

/**
 * A ManagementConsole listens for connections
 * for doing component management.
 */
public interface RestManagementConsole {
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
    public void initialise(int port);

    /**
     * Register a new command with the ManagementConsole.
     */
    public void register(RestCommand command);

    /**
     * Register the relevant commands for the ManagementConsole.
     */
    public void registerCommands();

    /**
     * Get the ComponentController this ManagementConsole
     * interacts with.
     */
    public ComponentController getComponentController();
}