/** This abstract class deals with the scheduling of events.
 */
package usr.events;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import usr.common.ANSI;
import usr.common.TimedThread;
import usr.logging.Logger;
import usr.logging.USR;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;


/**
 * An AbstractEventScheduler has the basic methods to implement an EventScheduler.
 * It goes through a queue of Events in a separate thread.
 * Each Event is passed of the an EventDelegate which is responsible 
 * for actually processing and handling thr event.
 * Sub classes can be specific for different uses.
 */
public abstract class AbstractEventScheduler  implements EventScheduler {
    ArrayList<Event> schedule_ = null;
    long lastEventTime_;
    long lastEventDuration_;        // length of time previous event took
    Object waitCounter_;          //  Just used in a wait loop
    EventDelegate delegate_;     // Hook back to gc
    long runStartTime_;    // time at which simulation started  (assuming realtime)
    boolean running;       // should the scheduler be running
    private Thread t;                    // Thread for scheduler


    AbstractEventScheduler() {
        schedule_ = new ArrayList<Event>();
        lastEventTime_ = 0;
        waitCounter_ = new Object();
        running = false;
    }

    /**
     * Start this EventScheduler in its own Thread.
     */
    public boolean start() {
        // Execute the EventScheduler
        running = true;
        t = new TimedThread(this);
        t.start();

        return true;
    }


    /**
     * Stop the EventScheduler
     */
    public boolean stop() {
        running = false;

        if (t.isAlive()) {
            wakeWait();
            try {
                t.join();
            } catch (InterruptedException ie) {
            }
        }

        return true;
    }


    /** Used if we are in emulation mode
     * Strips off an event, sends it to the  delegate for execution */
    @Override
    public void run() {
        while (running) {
            Event ev = getFirstEvent();
            long expectedStart = 0;

            long now = System.currentTimeMillis();

            if (ev == null) {
                Logger.getLogger("log").logln(USR.ERROR,  elapsedToString(getElapsedTime()) + " " + leadin() + "Run out of events to process");
                waitForever();

                // we waited, so update now
                now = System.currentTimeMillis();

            } else {
                expectedStart = ev.getTime() + getStartTime();
                waitUntil(expectedStart);

                // we waited, so update now
                now = System.currentTimeMillis();
            }

            if (!delegate_.isActive()) {
                break;
            }



            long lag = now - expectedStart;

            //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Lag is "+lag+" starting event "+ev);

            if (lag > delegate_.getMaximumLag()) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              leadin()+ delegate_.getName() + "  problem -- "
                                              + "lag is greater than allowed in options "
                                              + " allowed: " + delegate_.getMaximumLag() + " saw " +
                                              lag);
                delegate_.onEventSchedulerStop(now);
            }


            // now process the event

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + ANSI.CYAN + elapsedToString(getElapsedTime()) + " " + " starting event "+ev + ANSI.RESET_COLOUR);


            try {

                JSONObject js = delegate_.executeEvent(ev);

                // check if we got a null or a NON success result
                Boolean success= false;

                if (js != null) {
                    success = (Boolean)js.get("success");
                }

                if (success) {
                    delegate_.onEventSuccess(now, ev);

                } else { // failure
                    delegate_.onEventFailure(now, ev);

                    continue;
                }

            } catch (InstantiationException ine) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "Unexpected error in scheduled operation: "
                                              + ine.getMessage() + "\nEvent: "+ ev.toString());
                ine.printStackTrace();
                delegate_.onEventFailure(now, ev);
            } catch (InterruptedException ie) {
                Logger.getLogger("log").logln(USR.ERROR, delegate_.getName() + " problem -- scheduler interrupted");
                delegate_.onEventFailure(now, ev);
            } catch (TimeoutException te) {
                Logger.getLogger("log").logln(USR.ERROR, delegate_.getName() + " must be lagging, cannot interrupt "
                                              + "scheduler in time");
                delegate_.onEventFailure(now, ev);
            } catch (JSONException je) {
                Logger.getLogger("log").logln(USR.ERROR, "Event "+ev+" failed to return success in JSON");
                delegate_.onEventFailure(now, ev);

            } catch (Exception e) {
                e.printStackTrace();
                Logger.getLogger("log").logln(USR.ERROR, "Event "+ev+" failed to return success");
                delegate_.onEventFailure(now, ev);
            }

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + ANSI.MAGENTA + elapsedToString(getElapsedTime()) + " " + " finishing event "+ev + ANSI.RESET_COLOUR);

            lag = System.currentTimeMillis() - expectedStart;
            //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Lag is "+lag+" finishing event "+ev);
        }
    }


    /** Return first event from schedule
     */
    @Override
    public Event getFirstEvent() {
        if (schedule_.size() == 0) {
            return null;
        }

        Event ev = schedule_.remove(0);
        lastEventTime_ = ev.getTime();
        return ev;
    }

    /**
     * Wait forever -- no events in scheduler
     */
    private void waitForever() {
        try {
            synchronized (waitCounter_) {
                waitCounter_.wait();
            }
        } catch (InterruptedException e) {
            if (delegate_.isActive()) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "Scheduler interrupted without reason");
            }
        }
    }

    /** Interrupt above wait*/
    private void wakeWait() {
        synchronized (waitCounter_) {
            waitCounter_.notify();
        }
    }

    /**
     * Wait until a specified absolute time is milliseconds.
     */
    private void waitUntil(long time) {
        long now = System.currentTimeMillis();

        if (time <= now) {
            return;
        }

        try {
            long timeout = time - now;
            Logger.getLogger("log").logln(USR.STDOUT,
                                          "EVENT: " + "<"
                                          + lastEventDuration_ + "> " + (now - getStartTime()) + " @ "
                                          + now + " waiting " + timeout);
            synchronized (waitCounter_) {
                waitCounter_.wait(timeout);
            }
            lastEventDuration_ = System.currentTimeMillis() - now;
        } catch (InterruptedException e) {
            if (delegate_.isActive()) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "Scheduler interrupted without reason");
            }
        }
    }



    /** Adds an event to the schedule in time order
     */
    @Override
    public void addEvent(Event e) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() +
                                      ANSI.CYAN + "Adding Event at time: " + e.getTime() + " Event " + e + ANSI.RESET_COLOUR);

        // set the Scheduler for the event
        e.setEventScheduler(this);

        long time = e.getTime();

        for (int i = 0; i < schedule_.size(); i++) {
            if (schedule_.get(i).getTime() > time) {
                // Add at given position in list
                schedule_.add(i, e);
                //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Event position " +  i);
                return;
            }
        }

        schedule_.add(e);   // Add at end of list
        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Event position " +  "END");
    }

    /**
     * return list of events
     */
    public ArrayList <Event> getEvents()
    {
    	return schedule_;
    }


    /** Return start time */
    @Override
    public long getStartTime() {
        return runStartTime_;
    }

    /** Get the time of the last event
     */
    @Override
    public long getLastEventTime() {
        return lastEventTime_;
    }

    /** Get the duration of the last event
     */
    @Override
    public long getLastEventDuration() {
        return lastEventDuration_;
    }


    /** Return the time since the start of the run 
     * It is important to note that this can be called between events.
     */
    @Override
    public long getElapsedTime() {
        return System.currentTimeMillis() - getStartTime();
    }


    /**
     * Convert an elasped time, in milliseconds, into a string.
     * Converts something like 35432 into 35:43
     */
    public String elapsedToString(long elapsedTime) {
        long millis = (elapsedTime % 1000) / 10;

        long rawSeconds = elapsedTime / 1000;
        long seconds = rawSeconds % 60;
        long minutes = rawSeconds / 60;

        StringBuilder builder = new StringBuilder();

        if (minutes < 10) {
            builder.append("0");
        }
        builder.append(minutes);

        builder.append(":");

        if (seconds < 10) {
            builder.append("0");
        }
        builder.append(seconds);

        builder.append(":");

        if (millis < 10) {
            builder.append("0");
        }
        builder.append(millis);


        return builder.toString();

    }



    abstract String leadin();


}
