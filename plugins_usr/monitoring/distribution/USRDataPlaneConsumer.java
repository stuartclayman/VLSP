// USRDataPlaneConsumer.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.monitoring.distribution;

import usr.net.SocketAddress;
import eu.reservoir.monitoring.core.MeasurementReporting;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.distribution.Receiving;

public class USRDataPlaneConsumer extends USRDataPlaneConsumerWithNames
implements DataPlane, MeasurementReporting, Receiving
{
/**
 * Construct a USRDataPlaneConsumer.
 */
public USRDataPlaneConsumer(SocketAddress addr){
    super(addr);
}
}