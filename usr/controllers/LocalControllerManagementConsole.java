package usr.controllers;

import usr.interactor.*;
import java.net.*;
import usr.controllers.localcommand.*;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;

/**
 * A ManagementConsole listens for connections
 * for doing router management.
 * <p>
 * It implements the MCRP (Management Console Router Protocol).
 */
public class LocalControllerManagementConsole extends ManagementConsole implements Runnable {

    private LocalController localController_;
    
    public LocalControllerManagementConsole(LocalController lc, int port) {
       
       localController_= lc;
       initialise(port);
    }

    public LocalController getLocalController() {
       return localController_;
    }

    public void registerCommands() {
        register(new UnknownCommand());
        register(new LocalCheckCommand());
        register(new ShutDownCommand());
        register(new QuitCommand());
    }
    
    void register(LocalCommand command) {
        String commandName = command.getName();

        command.setManagementConsole(this);

        commandMap.put(commandName, command);
    }

    public void contactFromGlobal(LocalHostInfo gc) {
        localController_.aliveMessage(gc);
        
    }

}
