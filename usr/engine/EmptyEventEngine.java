/** Interface for Engine which adds events to the event list
*/

package usr.engine;

import usr.globalcontroller.*;
import usr.logging.*;

public class EmptyEventEngine implements EventEngine {
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
        SimEvent e0 = new SimEvent(SimEvent.EVENT_START_SIMULATION, 0, null,this);
        s.addEvent(e0);

        // simulation end
        SimEvent e= new SimEvent(SimEvent.EVENT_END_SIMULATION, timeToEnd_, null,this);
        s.addEvent(e);

    }
    
        /** Initial events to add to schedule */
    public void initialEvents(EventScheduler s, GlobalController g)
    {

    }
    
    /** Add or remove events following a simulation event */
    public void preceedEvent(SimEvent e, EventScheduler s,  GlobalController g) 
    {
    
    }
    
    /** Add or remove events following a simulation event */
    public void followEvent(SimEvent e, EventScheduler s,  GlobalController g,
        Object o)
    {
    
    }

}
