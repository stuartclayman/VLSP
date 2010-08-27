package usr.controllers;


import usr.interactor.*;
import usr.controllers.globalcommand.*;
import java.util.concurrent.*;

/**
 * A ManagementConsole for the GlobalController.
 * It listens for commands.
 * <p>
 * It implements the MCRP (Management Console Router Protocol).
 */
public class GlobalControllerManagementConsole extends AbstractManagementConsole implements Runnable {

    private GlobalController globalController_;
    public GlobalControllerManagementConsole(GlobalController gc, int port) {
       
       globalController_= gc;
       initialise(port);
    }

    public ComponentController getComponentController() {
       return globalController_;
    }

    public BlockingQueue<Request> addRequest(Request q) {
        // call superclass addRequest
        BlockingQueue<Request> rq = super.addRequest(q);
        // notify the GlobalController
        globalController_.notify();

        return rq;
    }

    public void registerCommands() {
      
        register(new UnknownCommand());
        register(new LocalOKCommand());
        register(new QuitCommand());
    }
    
}
