package usr.controllers;

import usr.net.Address;
import usr.net.IPV4Address;
import usr.interactor.*;
import usr.controllers.localcommand.*;

/**
 * A ManagementConsole listens for connections
 * for doing router management.
 * <p>
 * It implements the MCRP (Management Console Router Protocol).
 */
public class LocalControllerManagementConsole extends ManagementConsole implements Runnable {

    private LocalController _localController;
    
    public LocalControllerManagementConsole(LocalController lc, int port) {
       
       _localController= lc;
       initialise(port);
    }

    public LocalController getLocalController() {
       return _localController;
    }

    public void registerCommands() {
        register(new UnknownCommand());
        register(new QuitCommand());
    }
    
    void register(LocalCommand command) {
        String commandName = command.getName();

        command.setManagementConsole(this);

        commandMap.put(commandName, command);
    }

}
