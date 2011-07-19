// USRTransmissionMetaData.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.monitoring.distribution;

import usr.net.*;
import eu.reservoir.monitoring.distribution.MetaData;
import java.io.Serializable;

/**
 * Information about a transmission.
 * Includes: packet length, src ip address, dst ip address
 */
public class USRTransmissionMetaData implements MetaData, Serializable {
    public final int length;
    public final Address srcIPAddr;
    public final Address dstIPAddr;

    /**
     * Construct a USRTransmissionMetaData object.
     */
    public USRTransmissionMetaData(int l, Address sia, Address dia) {
        length = l;
        srcIPAddr = sia;
        dstIPAddr = dia;
    }

    /**
     * USRTransmissionMetaData to string.
     */
    public String toString() {
        return dstIPAddr + ": "  + srcIPAddr + " => " + length;
    }
}
