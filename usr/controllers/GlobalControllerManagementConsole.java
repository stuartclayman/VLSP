package usr.controllers;


import usr.interactor.*;
import usr.controllers.globalcommand.*;


/**
 * A ManagementConsole listens for connections
 * for doing router management.
 * <p>
 * It implements the MCRP (Management Console Router Protocol).
 */
public class GlobalControllerManagementConsole extends ManagementConsole implements Runnable {

    private GlobalController _globalController;
    public GlobalControllerManagementConsole(GlobalController gc, int port) {
       
       _globalController= gc;
       initialise(port);
    }

    public GlobalController getGlobalController() {
       return _globalController;
    }

    public void registerCommands() {
      
        register(new UnknownCommand());
        register(new QuitCommand());
    }
    
    void register(Command command) {
        String commandName = command.getName();

        command.setManagementConsole(this);

        commandMap.put(commandName, command);
    }

}
