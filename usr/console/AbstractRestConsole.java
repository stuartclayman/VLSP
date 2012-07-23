package usr.console;

import usr.logging.*;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.io.PrintStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.RequestLine;
import us.monoid.json.*;

/**
 * A ManagementConsole listens for REST requests
 * for doing component management.
 */
public abstract class AbstractRestConsole implements Container, ManagementConsole  {

    // the port this router is listening on
    int port;

    // HashMap of command name -> Command
    HashMap<String, Command> commandMap;

    // HashMap of command name -> Request
    HashMap<String, RequestHandler> handlerMap;

    // The handler of all the actual requests
    ContainerServer server;

    // The connection and socket
    Connection connection;
    SocketAddress address;


    /**
     * The no arg Constructor.
     */
    public AbstractRestConsole() {
    }


    /**
     * Construct a ManagementConsole, given a specific port.
     */
    public void initialise (int port) {
        this.port = port;

        // setp the Commands
        commandMap = new HashMap<String, Command>();

        handlerMap = new HashMap<String, RequestHandler>();


        // setup default /command handler
        defineRequestHandler("/command/", new CommandAsRestHandler());

        registerCommands();
    }

    /**
     * Start the ManagementConsole.
     */
    public boolean start() {
        // check the UnknownCommand exists
        Command unknown = commandMap.get("__UNKNOWN__");

        if (unknown == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "the UnknownCommand has not been registered");
            return false;
        }

        // initialise the socket
        try {
            server = new ContainerServer(this);
            connection = new SocketConnection(server);
            address = new InetSocketAddress(port);

            connection.connect(address);

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Listening on port: " + port);
            return true;

        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot listen on port: " + port);
            return false;
        }

    }


    /**
     * Stop the ManagementConsole.
     */
    public boolean stop() {
        try {
            server.stop();
            connection.close();
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }


    /**
     * This is the main handler method for a Container.
     * It takes the requests and delegates them to
     * define RequestHandler objects, based on a pattern.
     */
    public void handle(Request request, Response response) {
        try {
            /*
            System.out.println("method: " + request.getMethod());
            System.out.println("path: " + request.getPath());
            System.out.println("query: " + request.getQuery());
            System.out.println("target: " + request.getTarget());
            System.out.println("directory: " + request.getPath().getDirectory());
            */

            String path =  request.getPath().getPath();
            String directory = request.getPath().getDirectory();

            // skip through all patterns 
            // and try and find a RequestHandler for it

            Set<String> patterns = handlerMap.keySet();

            for (String pattern : patterns) {
                
                if (directory.equals(pattern) || path.matches(pattern)) {
                    RequestHandler handler = (RequestHandler)handlerMap.get(pattern);

                    handler.handle(request, response);

                    return;
                }
            }


            // if we fall through
            {
                System.out.println("AbstractRestConsole error: " + "no command");

                System.out.println("method: " + request.getMethod());
                System.out.println("path: " + request.getPath());
                System.out.println("query: " + request.getQuery());
                System.out.println("target: " + request.getTarget());
                System.out.println("directory: " + request.getPath().getDirectory());

                //fetch the UnknownCommand
                PrintStream out = response.getPrintStream();
                response.setCode(404);
                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "UnknownResource");
                out.println(jsobj.toString());
                response.close();
            }


        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        } catch (JSONException jex) {
            System.err.println(jex.getMessage());
        }
    }

    /**
     * Define a handler for a request
     */
    public void defineRequestHandler(String pattern, RequestHandler rh) {
        // set up the RequestHandler
        // point to ManagementConsole
        rh.setManagementConsole(this);
        rh.setPattern(pattern);

        // put in map
        handlerMap.put(pattern, rh);
    }


    /**
     * Register a new command with the ManagementConsole.
     */
    public void register(Command command) {
        String commandName = command.getName();

        command.setManagementConsole(this);

        commandMap.put(commandName, command);
    }

    /**
     * Find a command in the ManagementConsole.
     * @param commandName The name of the command
     */
    public Command find(String commandName) {
        return commandMap.get(commandName);
    }

    /**
     * Register the relevant commands for the ManagementConsole.
     * The actual ones are defined by the concrete implementations.
     */
    public abstract void  registerCommands();

    /**
     * Get the ComponentController this ManagementConsole
     * interacts with.
     */
    public abstract ComponentController getComponentController();



    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String MC = "MC: ";
        ComponentController control = getComponentController();

        if (control == null) {
            return MC;
        } else {
            return control.getName() + " " + MC;
        }

    }

}
