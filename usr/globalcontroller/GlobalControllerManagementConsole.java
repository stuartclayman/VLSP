package usr.globalcontroller;


import usr.logging.*;
import usr.console.*;
import usr.globalcontroller.command.*;
import java.util.concurrent.*;

/**
 * A ManagementConsole for the GlobalController.
 * It listens for commands.
 */
public class GlobalControllerManagementConsole extends USRRestConsole {

    public GlobalControllerManagementConsole(GlobalController gc, int port) {

        setAssociated(gc);
        initialise(port);
    }

    public void registerCommands() {

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

        // setup  /kbdata/ handler which handles callbacks
        // from the knowledgeblock
        //defineRequestHandler("/kbdata/.*", new KBDataHandler());

        // setup  /graph/ handler which gathers version of
        // virtual network as a graph - e.g. a dot file
        defineRequestHandler("/graph/", new GraphRestHandler());

        register(new UnknownCommand());
        register(new LocalOKCommand());
        register(new QuitCommand());
        register(new ShutDownCommand());
        //register(new NetworkGraphCommand());
        register(new ReportAPCommand());
        register(new OnRouterCommand());
        register(new GetRouterStatsCommand());
        register(new SendRouterStatsCommand());
    }

}
