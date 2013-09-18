package usr.console;

import java.util.HashMap;

import usr.logging.Logger;
import usr.logging.USR;
import cc.clayman.console.AbstractRestConsole;

/**
 * A ManagementConsole listens for REST requests
 * in the GlobalController
 */
public abstract class USRRestConsole extends AbstractRestConsole {


    // HashMap of command name -> Command
    HashMap<String, Command> commandMap;

    /**
     * The no arg Constructor.
     */
    public USRRestConsole() {
        // setup the Commands
        commandMap = new HashMap<String, Command>();
    }

    /**
     * Construct a ManagementConsole, given a specific port.
     */
    @Override
	public void initialise (int port) {
        super.initialise(port);

        // setup default /command handler
        defineRequestHandler("/command/", new CommandAsRestHandler());

    }

    /**
     * Start the ManagementConsole.
     */
    @Override
	public boolean start() {
        // check the UnknownCommand exists
        Command unknown = commandMap.get("__UNKNOWN__");

        if (unknown == null) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "the UnknownCommand has not been registered");
            return false;
        }


        return super.start();
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
     * Find a handler in the ManagementConsole.
     * @param pattern The pattern for the handler
     */
    public Command findHandler(String pattern) {
        return commandMap.get(pattern);
    }

}