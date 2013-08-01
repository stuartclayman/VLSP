package usr.router;

import usr.net.Address;
import usr.logging.*;
import usr.console.*;
import usr.router.command.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.*;
import java.nio.charset.Charset;

/**
 * A ManagementConsole listens for connections
 * for doing router management.
 * <p>
 */
public class RouterManagementConsole extends USRRestConsole {

    private RouterController _routerController;
    public RouterManagementConsole(RouterController rc, int port) {

        _routerController = rc;
        initialise(port);
    }

    /**
     * Get the ComponentController this ManagementConsole
     * interacts with.
     */
    public ComponentController getComponentController() {
        return _routerController;
    }

    public void registerCommands() {
        register(new UnknownCommand());
        register(new ShutDownCommand());
        register(new GetNameCommand());
        register(new SetNameCommand());
        register(new GetRouterAddressCommand());
        register(new SetRouterAddressCommand());
        register(new GetConnectionPortCommand());
        register(new ListConnectionsCommand());
        register(new IncomingConnectionCommand());
        register(new CreateConnectionCommand());
        register(new GetPortNameCommand());
        register(new GetPortRemoteRouterCommand());
        register(new GetPortRemoteAddressCommand());
        register(new GetPortAddressCommand());
        register(new SetPortAddressCommand());
        register(new GetPortWeightCommand());
        register(new SetPortWeightCommand());
        register(new ListRoutingTableCommand());
        register(new EndLinkCommand());
        register(new ReadOptionsFileCommand());
        register(new ReadOptionsStringCommand());
        register(new AppStartCommand());
        register(new AppStopCommand());
        register(new AppListCommand());
        register(new PingCommand());
        register(new RunCommand());
        register(new EchoCommand());
        register(new SetAPCommand());
        register(new GetNetIFStatsCommand());
        register(new GetSocketStatsCommand());
        register(new MonitoringStartCommand());
        register(new MonitoringStopCommand());
        register(new RouterOKCommand());
        register(new SetLinkWeightCommand());
    }

}
