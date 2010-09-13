/** Interface for Engine which adds events to the event list
*/

package usr.engine;

import usr.globalcontroller.*;

public class ProbabilisticEventEngine implements EventEngine {
    int timeToEnd_;

    /** Contructor from Parameter string */
    public ProbabilisticEventEngine(int time, String parms) 
    {
        timeToEnd_= time;
    }
    
    /** Initial events to add to schedule */
    public void initialEvents(EventScheduler s, GlobalController g)
    {
        // simulation start
        SimEvent e0 = new SimEvent(SimEvent.EVENT_START_SIMULATION, 0, null);
        s.addEvent(e0);

        // simulation end
        SimEvent e= new SimEvent(SimEvent.EVENT_END_SIMULATION, timeToEnd_, null);
        s.addEvent(e);

    }
    
    /** Add or remove events following a simulation event */
    public void preceedEvent(SimEvent e, EventScheduler s,  GlobalController g) 
    {
    
    }
    
    /** Add or remove events following a simulation event */
    public void followEvent(SimEvent e, EventScheduler s,  GlobalController g)
    {
    
    }

}
