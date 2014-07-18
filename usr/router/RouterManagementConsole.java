package usr.router;

import usr.console.ComponentController;
import usr.console.USRRestConsole;
import usr.router.command.AppListCommand;
import usr.router.command.AppStartCommand;
import usr.router.command.AppStopCommand;
import usr.router.command.CreateConnectionCommand;
import usr.router.command.EchoCommand;
import usr.router.command.EndLinkCommand;
import usr.router.command.GetConnectionPortCommand;
import usr.router.command.GetNameCommand;
import usr.router.command.GetNetIFStatsCommand;
import usr.router.command.GetPortAddressCommand;
import usr.router.command.GetPortNameCommand;
import usr.router.command.GetPortRemoteAddressCommand;
import usr.router.command.GetPortRemoteRouterCommand;
import usr.router.command.GetPortWeightCommand;
import usr.router.command.GetRouterAddressCommand;
import usr.router.command.GetSocketStatsCommand;
import usr.router.command.IncomingConnectionCommand;
import usr.router.command.ListConnectionsCommand;
import usr.router.command.ListRoutingTableCommand;
import usr.router.command.MonitoringStartCommand;
import usr.router.command.MonitoringStopCommand;
import usr.router.command.PingCommand;
import usr.router.command.ReadOptionsFileCommand;
import usr.router.command.ReadOptionsStringCommand;
import usr.router.command.RouterOKCommand;
import usr.router.command.RunCommand;
import usr.router.command.SetAPCommand;
import usr.router.command.SetLinkWeightCommand;
import usr.router.command.SetNameCommand;
import usr.router.command.SetPortAddressCommand;
import usr.router.command.SetPortWeightCommand;
import usr.router.command.SetRouterAddressCommand;
import usr.router.command.ShutDownCommand;
import usr.router.command.UnknownCommand;

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

    @Override
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
