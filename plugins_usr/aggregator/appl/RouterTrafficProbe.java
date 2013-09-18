// RouterTrafficProbe.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Mar 2011

package plugins_usr.aggregator.appl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import usr.common.Pair;
import usr.router.NetIF;
import usr.router.NetStats;
import usr.router.Router;
import usr.router.RouterController;
import usr.router.RouterDirectory;
import usr.router.RouterPort;
import eu.reservoir.monitoring.appl.datarate.EveryNSeconds;
import eu.reservoir.monitoring.core.AbstractProbe;
import eu.reservoir.monitoring.core.DefaultProbeAttribute;
import eu.reservoir.monitoring.core.DefaultProbeValue;
import eu.reservoir.monitoring.core.Probe;
import eu.reservoir.monitoring.core.ProbeAttributeType;
import eu.reservoir.monitoring.core.ProbeMeasurement;
import eu.reservoir.monitoring.core.ProbeValue;
import eu.reservoir.monitoring.core.ProducerMeasurement;

/**
 * A probe that talks to a Router and collects the traffic.
 * It get stats for each NetIF and sums that.
 */
public class RouterTrafficProbe extends AbstractProbe implements Probe
{
// The controller of the router we are getting stats for
RouterController controller;

// Traffic on links
HashMap<String, Pair<Integer, Integer> > trafficLinkCounts = null;

// Start time
long startTime = 0L;

/**
 * Construct a RouterTrafficProbe
 */
public RouterTrafficProbe(String name){
    // get the Router on this JVM
    Router router = RouterDirectory.getRouter();

    // and now its controller
    controller = router.getRouterController();

    // set probe name
    setName(name);
    // set data rate
    setDataRate(new EveryNSeconds(10));     // == Rational(360, 1)

    // add a probe attribute
    addProbeAttribute(new DefaultProbeAttribute(0, "traffic",
            ProbeAttributeType.
            INTEGER, "bytes"));

    trafficLinkCounts = new HashMap<String, Pair<Integer, Integer> >();

    startTime = System.currentTimeMillis();
}

/**
 * Collect a measurement.
 */
@Override
public ProbeMeasurement collect(){
    try {
        // create a list for the result
        ArrayList<ProbeValue> list = new ArrayList<ProbeValue>(1);

        // check localnet first
        NetIF localNetIF = controller.getLocalNetIF();

        if (localNetIF == null)
            // the router is not ready yet
            return null;

        // get list of ports
        List<RouterPort> ports = controller.listPorts();

        if (ports.size() == 0) {
            // there are no ports to other routers
            // so there will be no network traffic
            return null;
        }

        // total volume  of traffic
        int total = 0;
        NetStats stats = null;
        Pair<Integer, Integer>inOut = null;

        /*
         * // DON'T INCLUDE localnet
         * // process localNetIF
         * stats = localNetIF.getStats();
         * name = localNetIF.getName();
         *
         * inOut =  linkVolume(localNetIF, stats);
         *
         * System.out.println("Traffic for " + name + " = " +
         *    inOut);
         *
         * total += inOut.getFirst() + inOut.getSecond();
         */

        // now visit each Port
        for (RouterPort rp : ports) {
            if (rp.equals(RouterPort.EMPTY)) {
                continue;
            } else {
                NetIF netIF = rp.getNetIF();

                netIF.getName();

                stats = netIF.getStats();

                inOut = linkVolume(netIF, stats);

                //System.out.println("Traffic for " + name + " to "
                // +
                // netIF.getRemoteRouterName() + " = " + inOut);

                total += inOut.getFirst() + inOut.getSecond();
            }
        }

        //System.out.println(" Total for " + controller.getName() +
        // " =
        // " + total);

        list.add(new DefaultProbeValue(0, total));

        // set the type to be: RouterTraffic
        return new ProducerMeasurement(this, list, "RouterTraffic");
    } catch (Exception e) {
        return null;
    }
}

/**
 * Get the volume on a link, given connection name and the latest stats
 * for that connection.
 */
private Pair<Integer, Integer>linkVolume(NetIF netIF,
    NetStats stats)                     {
    String name = netIF.getName();

    // given a NetStats object, we work out the amount of byes in
    // and
    // out
    int inBytes = stats.getValue(NetStats.Stat.InBytes);
    int outBytes = stats.getValue(NetStats.Stat.OutBytes);

    // last time
    Pair<Integer, Integer> lastTime = trafficLinkCounts.get(name);

    if (lastTime == null) {
        Pair<Integer, Integer> result = new Pair<Integer, Integer>(
            inBytes, outBytes);

        // save these stats for next time
        trafficLinkCounts.put(name, result);

        return result;
    } else {
        Pair<Integer, Integer> thisTime = new Pair<Integer, Integer>(
            inBytes, outBytes);

        // save these stats for next time
        trafficLinkCounts.put(name, thisTime);

        int oldInBytes = lastTime.getFirst();
        int oldOutBytes = lastTime.getSecond();

        Pair<Integer,
            Integer> result =
            new Pair<Integer, Integer>((inBytes - oldInBytes),
                                       (outBytes - oldOutBytes));

        return result;
    }
}
}