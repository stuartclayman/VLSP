package usr.localcontroller;

import usr.console.*;
import java.net.*;
import usr.localcontroller.command.*;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;

/**
 * A ManagementConsole listens for the LocalController.
 * It listens for commands.
 * <p>
 * It implements the MCRP (Management Console Router Protocol).
 */
public class LocalControllerManagementConsole extends AbstractManagementConsole implements Runnable {

    public LocalController localController_;
    
    public LocalControllerManagementConsole(LocalController lc, int port) {
       
       localController_= lc;
       initialise(port);
    }

    public ComponentController getComponentController() {
       return localController_;
    }

    public void registerCommands() {
        register(new UnknownCommand());
        register(new LocalCheckCommand());
        register(new ShutDownCommand());
        register(new QuitCommand());
        register(new NewRouterCommand());
    }
        
    public boolean requestNewRouter(int rId)
    {
        return localController_.requestNewRouter(rId);
    }

    public void contactFromGlobal(LocalHostInfo gc) {
        localController_.aliveMessage(gc);
        
    }

}
