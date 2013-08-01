package usr.localcontroller;

import usr.console.*;
import usr.logging.*;
import java.net.*;
import usr.localcontroller.command.*;
import java.nio.channels.SocketChannel;
import usr.common.LocalHostInfo;
import usr.common.BasicRouterInfo;

/**
 * A ManagementConsole listens for the LocalController.
 * It listens for commands.
 */
public class LocalControllerManagementConsole extends USRRestConsole {

    public LocalController localController_;

    public LocalControllerManagementConsole(LocalController lc, int port) {

        localController_ = lc;
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
        register(new SetAPCommand());
        register(new ReportAPCommand());
        register(new OnRouterCommand());
        register(new GetRouterStatsCommand());
        register(new RequestRouterStatsCommand());
        register(new MonitoringStartCommand());
        register(new MonitoringStopCommand());
        register(new SetLinkWeightCommand());
    }

}
