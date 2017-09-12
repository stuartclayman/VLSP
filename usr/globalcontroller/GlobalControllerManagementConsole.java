package usr.globalcontroller;


import usr.console.USRRestConsole;
import usr.globalcontroller.command.GetRouterStatsCommand;
import usr.globalcontroller.command.StatusCommand;
import usr.globalcontroller.command.QuitCommand;
import usr.globalcontroller.command.ReportAPCommand;
import usr.globalcontroller.command.SendRouterStatsCommand;
import usr.globalcontroller.command.ShutDownEventCommand;
import usr.globalcontroller.command.UnknownCommand;

/**
 * A ManagementConsole for the GlobalController.
 * It listens for commands.
 */
public class GlobalControllerManagementConsole extends USRRestConsole {

    public GlobalControllerManagementConsole(GlobalController gc, int port) {

        setAssociated(gc);
        initialise(port);
    }

    @Override
	public void registerCommands() {

    	 	// setup default /localcontroller/ handler
        defineRequestHandler("/localcontroller/", new LocalControllerRestHandler());
    	
    		// setup default /router/ handler
        defineRequestHandler("/router/", new RouterRestHandler());

        // setup default /link/ handler
        defineRequestHandler("/link/", new LinkRestHandler());

        // setup default /router/id/app/ handler
        defineRequestHandler("/router/[0-9]+/app/.*", new AppRestHandler());

        // setup default /router/id/link/ handler
        defineRequestHandler("/router/[0-9]+/link/.*", new RouterLinkRestHandler());

        // setup default /router/id/link_stats handler
        defineRequestHandler("/router/[0-9]+/link_stats.*", new RouterRestHandler());

        // setup default /ap/ handler
        defineRequestHandler("/ap/", new AggPointRestHandler());

        // setup default /removed/ handler
        defineRequestHandler("/removed/", new RemovedRestHandler());

        // setup  /graph/ handler which gathers version of
        // virtual network as a graph - e.g. a dot file
        defineRequestHandler("/graph/", new GraphRestHandler());

        register(new UnknownCommand());
        //register(new LocalOKCommand());
        register(new QuitCommand());
        register(new ShutDownEventCommand());
        register(new StatusCommand());
        
        // sclayman 20140104 - not sure if these are needed any more
        register(new ReportAPCommand());
        register(new GetRouterStatsCommand());
        register(new SendRouterStatsCommand());
    }

}
