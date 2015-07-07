/** Interface for Engine which adds events to the event list
 */

package usr.engine;

import java.util.logging.Level;
import java.util.logging.Logger;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.events.EndSimulationEvent;
import usr.events.Event;
import usr.events.EventDelegate;
import usr.events.EventScheduler;
import usr.events.vim.StartRouterEvent;
import usr.events.StartSimulationEvent;
import usr.events.vim.EndRouterEvent;
import usr.globalcontroller.GlobalController;

public class TestEventEngine extends EmptyEventEngine implements EventEngine {
    int routerID;   // the ID of the created router

    /** Contructor from Parameter string */
    public TestEventEngine(int time, String parms) throws EventEngineException {
        super(time, parms);
    }

    /** Start up and shut down events */
    @Override
    public void startStopEvents(EventScheduler s, EventDelegate g) {
        // simulation start
        StartSimulationEvent e0 = new StartSimulationEvent(0);

        s.addEvent(e0);

        // simulation end
        EndSimulationEvent e = new EndSimulationEvent(timeToEnd_);
        s.addEvent(e);
    }

    /** Initial events to add to schedule */
    @Override
    public void initialEvents(EventScheduler s, EventDelegate g) {
        StartRouterEvent e1 = new StartRouterEvent(250, this);

        s.addEvent(e1);
    }

    /** Add or remove events following a simulation event */
    @Override
    public void preceedEvent(Event e, EventScheduler s, EventDelegate g) {
    }

    /** Add or remove events following a simulation event */
    @Override
    public void followEvent(Event e, EventScheduler s, JSONObject response, EventDelegate g) {
        try {
            routerID = response.getInt("routerID");
        } catch (JSONException jse) {
            routerID = -1;
        }


    }

        
    @Override
    public void finalEvents(EventDelegate obj) {
        try {
            EndRouterEvent e2 = new EndRouterEvent(0, this, routerID);
            
            obj.executeEvent(e2);
        } catch (Exception ex) {
        }

    }

}
