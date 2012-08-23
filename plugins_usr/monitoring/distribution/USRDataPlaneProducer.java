// USRDataPlaneProducer.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.monitoring.distribution;

import usr.net.SocketAddress;
import eu.reservoir.monitoring.core.DataSourceDelegateInteracter;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.distribution.Transmitting;

/**
 * A USRDataPlaneProducer is a DataPlane implementation
 * that sends Measurements by USR.
 * It is also a DataSourceDelegateInteracter so it can, if needed,
 * talk to the DataSource object it gets bound to.
 */
public class USRDataPlaneProducer extends USRDataPlaneProducerWithNames
implements DataPlane, DataSourceDelegateInteracter, Transmitting
{
/**
 * Construct a USRDataPlaneProducer.
 */
public USRDataPlaneProducer(SocketAddress addr){
    super(addr);
}
}