package usr.localcontroller;

import usr.console.*;
import java.net.*;
import usr.localcontroller.command.*;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;
import usr.common.BasicRouterInfo;

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
        register(new ConnectRoutersCommand());
        register(new EndRouterCommand());
        register(new EndLinkCommand());
        register(new RouterConfigCommand());
    }
        
    public boolean endRouter(LocalHostInfo r) {
        return localController_.endRouter(r);
    }    
    
    public boolean endLink(LocalHostInfo r1, int r2) 
    {
        return localController_.endLink(r1,r2);
    }
    
    public String requestNewRouter(int rId, int port1, int port2)
    {
        return localController_.requestNewRouter(rId, port1, port2);
    }
    
    public String connectRouters(LocalHostInfo r1, LocalHostInfo r2)
    {
        return localController_.connectRouters(r1, r2);
    }

    public void contactFromGlobal(LocalHostInfo gc) {
        localController_.aliveMessage(gc);
        
    }

}
