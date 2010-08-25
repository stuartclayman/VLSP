package usr.controllers;


import usr.interactor.*;
import usr.controllers.globalcommand.*;
import java.util.concurrent.*;

/**
 * A ManagementConsole listens for connections
 * for doing router management.
 * <p>
 * It implements the MCRP (Management Console Router Protocol).
 */
public class GlobalControllerManagementConsole extends ManagementConsole implements Runnable {

    private GlobalController globalController_;
    public GlobalControllerManagementConsole(GlobalController gc, int port) {
       
       globalController_= gc;
       initialise(port);
    }

    public GlobalController getGlobalController() {
       return globalController_;
    }

    public BlockingQueue<Request> addRequest(Request q) {
        super.requestQueue.add(q);
        globalController_.notify();
        return requestQueue;
    }

    public void registerCommands() {
      
        register(new UnknownCommand());
        register(new LocalOKCommand());
        register(new QuitCommand());
    }
    
    void register(GlobalCommand command) {
        String commandName = command.getName();

        command.setManagementConsole(this);

        commandMap.put(commandName, command);
    }

}
