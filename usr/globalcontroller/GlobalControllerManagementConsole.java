package usr.globalcontroller;

import usr.logging.*;
import usr.console.*;
import usr.globalcontroller.command.*;
import java.util.concurrent.*;

/**
 * A ManagementConsole for the GlobalController.
 * It listens for commands.
 */
public class GlobalControllerManagementConsole extends
AbstractRestConsole
{
private GlobalController globalController_;

public GlobalControllerManagementConsole(GlobalController gc,
    int port){
    globalController_ = gc;
    initialise(port);
}

public ComponentController getComponentController(){
    return globalController_;
}

/*
 * public BlockingQueue<Request> addRequest(Request q) {
 *  // call superclass addRequest
 *  BlockingQueue<Request> rq = super.addRequest(q);
 *  // notify the GlobalController
 *  globalController_.wakeWait();
 *
 *  return rq;
 * }
 */
public void registerCommands(){
    // setup default /router/ handler
    defineRequestHandler("/router/", new RouterRestHandler());

    // setup default /link/ handler
    defineRequestHandler("/link/", new LinkRestHandler());

    // setup default /router/id/app/ handler
    defineRequestHandler("/router/[0-9]+/app/", new AppRestHandler());

    register(new UnknownCommand());
    register(new LocalOKCommand());
    register(new QuitCommand());
    register(new ShutDownCommand());
    register(new NetworkGraphCommand());
    register(new ReportAPCommand());
    register(new OnRouterCommand());
    register(new GetRouterStatsCommand());
    register(new SendRouterStatsCommand());
}
}