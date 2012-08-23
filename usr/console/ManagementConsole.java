package usr.console;

import usr.net.Address;
import usr.logging.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.*;
import java.nio.charset.Charset;

/**
 * A ManagementConsole listens for connections
 * for doing component management.
 */
public interface ManagementConsole
{
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
public void register(Command command);

/**
 * Find a command in the ManagementConsole.
 * @param commandName The name of the command
 */
public Command find(String commandName);

/**
 * Register the relevant commands for the ManagementConsole.
 */
public void  registerCommands();

/**
 * Define a handler for a request
 */
public void defineRequestHandler(String pattern, RequestHandler rh);

/**
 * Add a Request to the queue
 */
//    public BlockingQueue<Request> addRequest(Request q);

/**
 * Get the ComponentController this ManagementConsole
 * interacts with.
 */
public ComponentController getComponentController();

/**
 * The state that the FSM of the ManagementConsole might get to.
 */
enum FSMState {
    STATE0,                             // state 0
    START,                              // the ManagementConsole is
                                        // starting
    STOP,                               // the ManagementConsole is
                                        // stopping
    SELECTING,                          // the ManagementConsole is in a
                                        // select()
    CONNECTING,                         // the ManagementConsole is
                                        // seetting up
                                        // a new connection
    PROCESSING                          // the ManagementConsole is
                                        // processing
                                        // a command
}
}