// RouterLifecycleProbe.java

package usr.globalcontroller;

import java.util.ArrayList;

import usr.logging.Logger;
import usr.logging.USR;
import eu.reservoir.monitoring.core.AbstractProbe;
import eu.reservoir.monitoring.core.DefaultProbeAttribute;
import eu.reservoir.monitoring.core.DefaultProbeValue;
import eu.reservoir.monitoring.core.Probe;
import eu.reservoir.monitoring.core.ProbeAttributeType;
import eu.reservoir.monitoring.core.ProbeMeasurement;
import eu.reservoir.monitoring.core.ProbeValue;
import eu.reservoir.monitoring.core.ProducerMeasurement;

/**
 * A probe that talks to a Router and passes on the current lifecycle
 * state of the Router.  Currently only STARTED and STOPPED  are supported.
 */
public class RouterLifecycleProbe extends AbstractProbe implements Probe, RouterCreatedNotification, RouterDeletedNotification {
    // Globalcontroller
    GlobalController globalcontroller;

    // The last lifecycle values
    String routerName;
    String lifecycleValue;

    /**
     * Construct a RouterLifecycleProbe
     */
    public RouterLifecycleProbe(GlobalController gc) {
        globalcontroller = gc;

        // set probe name
        setName(gc.getName()+".router-lifecycle");

        // setup the probe attributes
        // The router name
        // The router status
        addProbeAttribute(new DefaultProbeAttribute(0, "RouterName", ProbeAttributeType.STRING, "name"));
        addProbeAttribute(new DefaultProbeAttribute(1, "Status", ProbeAttributeType.STRING, "status"));
    }

    /**
     * Set the started lifecycle
     */
    @Override
	public void beginThreadBody() {
        Logger.getLogger("log").logln(USR.STDOUT,"RouterLifecycleProbe: started");
    }

    /**
     * Set the stopped lifecycle
     */
    @Override
	public void endThreadBody() {
        Logger.getLogger("log").logln(USR.STDOUT,"RouterLifecycleProbe: stopped");

    }

    /**
     * The named router has been created
     */
    @Override
	public void routerCreated(String routerName) {
        this.routerName = routerName;
        lifecycleValue = (String)inform("STARTED");
    }

    /**
     * The named router has been deleted
     */
    @Override
	public void routerDeleted(String routerName) {
        this.routerName = routerName;
        lifecycleValue = (String)inform("STOPPED");
    }

    /**
     * Collect a measurement.
     */
    @Override
	public ProbeMeasurement collect() {
        //lifecycleValue = (String)getLastOnEventData();

        try {

            if (lifecycleValue == null) {
                // jump in here where probe is first turned on, but
                // there is nothing to do
                // we wait until we get informed of a router start or router stop
                return null;
            } else {

                if (lifecycleValue.equals("STARTED")) {
                    Logger.getLogger("log").logln(USR.STDOUT,
                        getName() + " lifecycleValue is STARTED for " + routerName);
                    ProducerMeasurement lastestM = measure();
                    return lastestM;

                } else if (lifecycleValue.equals("STOPPED")) {
                    Logger.getLogger("log").logln(USR.STDOUT,
                        getName() + " lifecycleValue is STOPPED for " + routerName);

                    ProducerMeasurement lastestM = measure();
                    return lastestM;

                } else {
                    return null;
                }


            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private ProducerMeasurement measure() throws Exception {

        // collate measurement values
        ArrayList<ProbeValue> list = new ArrayList<ProbeValue>();

        // add router name
        list.add(new DefaultProbeValue(0, routerName));
        list.add(new DefaultProbeValue(1, lifecycleValue));

        // set the type to be: RouterLifecycle
        ProducerMeasurement lastestM = new ProducerMeasurement(this, list, "RouterLifecycle");

        return lastestM;
    }

}
