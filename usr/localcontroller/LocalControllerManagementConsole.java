package usr.localcontroller;

import usr.console.ComponentController;
import usr.console.USRRestConsole;
import usr.localcontroller.command.ConnectRoutersCommand;
import usr.localcontroller.command.EndLinkCommand;
import usr.localcontroller.command.EndRouterCommand;
import usr.localcontroller.command.GetRouterStatsCommand;
import usr.localcontroller.command.LocalCheckCommand;
import usr.localcontroller.command.MonitoringStartCommand;
import usr.localcontroller.command.MonitoringStopCommand;
import usr.localcontroller.command.NewRouterCommand;
import usr.localcontroller.command.AppStartCommand;
import usr.localcontroller.command.AppStopCommand;
import usr.localcontroller.command.QuitCommand;
import usr.localcontroller.command.ReportAPCommand;
import usr.localcontroller.command.RequestRouterStatsCommand;
import usr.localcontroller.command.RouterConfigCommand;
import usr.localcontroller.command.SetAPCommand;
import usr.localcontroller.command.SetLinkWeightCommand;
import usr.localcontroller.command.ShutDownCommand;
import usr.localcontroller.command.StatusCommand;
import usr.localcontroller.command.UnknownCommand;

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

    @Override
    public void registerCommands() {
        register(new UnknownCommand());
        register(new ShutDownCommand());
        register(new QuitCommand());
        register(new StatusCommand());

        register(new LocalCheckCommand());

        register(new NewRouterCommand());
        register(new ConnectRoutersCommand());
        register(new EndRouterCommand());
        register(new EndLinkCommand());
        register(new RouterConfigCommand());
        register(new SetAPCommand());
        register(new ReportAPCommand());
        register(new AppStartCommand());
        register(new AppStopCommand());
        register(new GetRouterStatsCommand());
        register(new RequestRouterStatsCommand());
        register(new MonitoringStartCommand());
        register(new MonitoringStopCommand());
        register(new SetLinkWeightCommand());
    }

}
