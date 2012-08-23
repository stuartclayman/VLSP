/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import usr.globalcontroller.*;
import usr.logging.*;
import usr.events.*;

public class EmptyEventEngine extends NullEventEngine {
    long timeToEnd_;

    /** Contructor from Parameter string */
    public EmptyEventEngine(int time, String parms)
    {
        timeToEnd_= time*1000;
    }

    /** Initial events to add to schedule */
    public void startStopEvents(EventScheduler s, GlobalController g)
    {
        // simulation start
        StartSimulationEvent e0 = new StartSimulationEvent(0,this);
        s.addEvent(e0);

        // simulation end
        EndSimulationEvent e= new EndSimulationEvent(timeToEnd_, this);
        s.addEvent(e);

    }

    /** Initial events to add to schedule */
    public void initialEvents(EventScheduler s, GlobalController g)
    {

    }

    /** Add or remove events following a simulation event */
    public void preceedEvent(Event e, EventScheduler s,  GlobalController g)
    {

    }

    /** Add or remove events following a simulation event */
    public void followEvent(Event e, EventScheduler s,  GlobalController g)
    {

    }

}
