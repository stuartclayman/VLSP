package usr.test;

import usr.events.*;
import usr.engine.*;
import usr.vim.VimClient;
import usr.common.ANSI;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

public class RemoteExecution implements EventDelegate {
    EventEngine eventEngine;    // An Event Engine that generates Events
                                // The events are managed by the EventScheduler

    EventScheduler scheduler;   // The EventScheduler schedules Events from the event list

    VimClient vim;              // The remote Vim we talk to

    boolean running = false;

    // Object used  simply to wait
    private Object runLock = new Object();



    /**
     * Construct a RemoteExecution.
     */
    public RemoteExecution() throws UnknownHostException, IOException {
        vim = new VimClient();
    }

    /**
     * Construct a RemoteExecution.
     */
    public RemoteExecution(String addr, int port)  throws UnknownHostException, IOException {
        vim = new VimClient(addr, port);
    }

    /**
     * Construct a RemoteExecution.
     */
    public RemoteExecution(InetAddress addr, int port)  throws UnknownHostException, IOException {
        vim = new VimClient(addr, port);
    }

    /**
     * Init
     */
    public boolean init() {
        // allocate a new logger
        Logger logger = Logger.getLogger("log");

        // tell it to output to stdout and tell it what to pick up
        // it will actually output things where the log has bit
        // USR.STDOUT set

        // tell it to output to stderr and tell it what to pick up
        // it will actually output things where the log has bit
        // USR.ERROR set
        logger.addOutput(System.err, new BitMask(USR.ERROR));
        logger.addOutput(System.out, new BitMask(USR.STDOUT));


        // start EventScheduler and pass in EventDelegate
        scheduler = startEventScheduler(this);

        // start EventEngine and pass in EventDelegate
        eventEngine = startEventEngine(this);


        eventEngine.startStopEvents(scheduler, this);
        eventEngine.initialEvents(scheduler, this);

        return true;
    }

    /**
     * Start
     */
    public boolean start() {
        running = true;

        synchronized (runLock) {

            // Start Scheduler as thread
            scheduler.start();


            while (running) {
                try {
                    Logger.getLogger("log").logln(USR.STDOUT, leadin()+ "runLock wait");
                    runLock.wait();
                } catch (InterruptedException ie) {
                } catch (IllegalMonitorStateException ims) {
                }
            }
        }

        scheduler.stop();

        return true;
    }

    /**
     * Stop
     */
    public boolean stop() {
        running = false;

        return true;
    }



    /**
     * Start an EventScheduler
     */
    public EventScheduler startEventScheduler(EventDelegate ed) {
        return new SimpleEventScheduler(ed);
    }


    /**
     * Start an EventEngine
     */
    public EventEngine startEventEngine(EventDelegate ed) {
        try {
            // time to run:  86400 seconds = 1 day
            // startup script

            //return new usr.engine.ScriptEngine(86400, "scripts/AppScript2Ca");

            return new usr.engine.IKMSEventEngine(1800, "scripts/ikms.xml");
        } catch (EventEngineException eee) {
            throw new Error("Cant start event engine");
        }
    }

    /** Execute an event,
     * return a JSON object with information about it
     * throws Instantiation if creation fails
     * Interrupted if acquisition of lock interrupted
     * Timeout if acquisition timesout
     */
    public JSONObject executeEvent(Event e) throws InstantiationException, InterruptedException, TimeoutException {
        try {
            //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"EVENT executeEvent: " + e);

            ExecutableEvent ee = null;

            if (e instanceof ExecutableEvent) {
                ee = (ExecutableEvent) e;
            } else {
                // resolve event
                EventResolver resolver = new REEventResolver();
                        
                ee = resolver.resolveEvent(e);

                if (ee == null) {
                    Logger.getLogger("log").logln(USR.ERROR, ANSI.RED + "EVENT not ExecutableEvent: " + e + ANSI.RESET_COLOUR);
                    return null;
                }

                // tell the event which EventScheduler we are using
                ee.setEventScheduler(scheduler);

            }

            // event preceeed
            ee.preceedEvent(this);

            // pass in RemoteExecution as EventDelegate, and VimClient as context object
            JSONObject js = ee.execute(this, vim);

            //Logger.getLogger("log").logln(USR.STDOUT, "EVENT result:  " + js);

            // event follow
            if (js != null) {
                try {
                    if (js.getBoolean("success")) {
                        ee.followEvent(js, this);
                    }
                } catch (JSONException je) { }
            }



            //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"EVENT done: " + e );
            String str = js.toString();

            Logger.getLogger("log").logln(USR.STDOUT, leadin()+ " result "+str);
            return js;
        } catch (Error err) {
            err.printStackTrace();
            throw new InstantiationException("Event " + e + " failed");
        }
    }

    /**
     * checks if the delegate is active
     */
    public boolean isActive() {
        return running;
    }

    /**
     * Notification for start of EventScheduler
     */
    public void onEventSchedulerStart(long time) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Start of run  at: " + time + " " + System.currentTimeMillis());
    }

    /**
     * Notification for stop of EventScheduler
     */
    public void onEventSchedulerStop(long time) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + "End of run  at " + time + " " + System.currentTimeMillis());
    }

    /** 
     * Notification for an event execution success 
     */
    public void onEventSuccess(long time, Event ev) {
    }

    /**
     * Notification for an event execution failure
     */
    public void onEventFailure(long time, Event ev) {
        Logger.getLogger("log").logln(USR.ERROR, leadin() + "Event "+ev+" failed");
    }

    /**
     * Get the maximum lag this delegate will allow.
     */
    public long getMaximumLag() {
        return 4000;   // 4 seconds == 4000 ms
    }

    /**
     * Get the name of the delegate
     */
    public String getName() {
        return "RemoteExecution";
    }


    private String leadin() {
        final String leadin =  getName() + ": ";
        return leadin;
    }


    public static void main(String[] args) {
        try {
            RemoteExecution rexec = new RemoteExecution();

            rexec.init();

            rexec.start();

            rexec.stop();
        } catch (Exception e) {
        }
    }

}

class REEventResolver implements EventResolver {
    public REEventResolver() {
    }

    public ExecutableEvent resolveEvent(Event e) {
        if (e instanceof usr.events.vim.StartRouterEvent) {
            usr.events.vim.StartRouterEvent sre = (usr.events.vim.StartRouterEvent)e;
            return new usr.events.vimfunctions.StartRouterEvent(sre);

        } else if (e instanceof   usr.events.vim.EndRouterEvent) {
            usr.events.vim.EndRouterEvent ere = (usr.events.vim.EndRouterEvent)e;
            return new usr.events.vimfunctions.EndRouterEvent(ere);

        } else if (e instanceof   usr.events.vim.StartLinkEvent) {
            usr.events.vim.StartLinkEvent sle = (usr.events.vim.StartLinkEvent)e;
            return new usr.events.vimfunctions.StartLinkEvent(sle);

        } else if (e instanceof   usr.events.vim.EndLinkEvent) {
            usr.events.vim.EndLinkEvent ele = (usr.events.vim.EndLinkEvent)e;
            return new usr.events.vimfunctions.EndLinkEvent(ele);

        } else if (e instanceof  usr.events.vim.AppStartEvent) {
            usr.events.vim.AppStartEvent ase = (usr.events.vim.AppStartEvent)e;
            return new usr.events.vimfunctions.AppStartEvent(ase);

        } else {
        }

        return null;

    }
}
