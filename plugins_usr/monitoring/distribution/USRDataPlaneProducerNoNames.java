// USRDataPlaneProducerNoNames.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.monitoring.distribution;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;

import usr.net.SocketAddress;
import eu.reservoir.monitoring.core.DataSourceDelegateInteracter;
import eu.reservoir.monitoring.core.ID;
import eu.reservoir.monitoring.core.ProbeMeasurement;
import eu.reservoir.monitoring.core.TypeException;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.core.plane.DataPlaneMessage;
import eu.reservoir.monitoring.core.plane.MeasurementMessage;
import eu.reservoir.monitoring.distribution.MeasurementEncoder;
import eu.reservoir.monitoring.distribution.Transmitting;
import eu.reservoir.monitoring.distribution.XDRDataOutputStream;

/**
 * A USRDataPlaneProducerNoNames is a DataPlane implementation
 * that sends Measurements by USR.
 * It is also a DataSourceDelegateInteracter so it can, if needed,
 * talk to the DataSource object it gets bound to.
 */
public class USRDataPlaneProducerNoNames extends
AbstractUSRDataPlaneProducer implements DataPlane,
DataSourceDelegateInteracter, Transmitting
{
/**
 * Construct a USRDataPlaneProducerNoNames
 */
public USRDataPlaneProducerNoNames(SocketAddress addr){
    super(addr);
}

/**
 * Send a message onto the address.
 * The message is XDR encoded and it's structure is:
 * +-------------------------------------------------------------------+
 * | data source id (long) | msg type (int) | seq no (int) | payload   |
 * +-------------------------------------------------------------------+
 */
@Override
public int transmit(DataPlaneMessage dsp) throws Exception {
    // convert DataPlaneMessage into a ByteArrayOutputStream
    // then transmit it

    try {
        // convert the object to a byte []
        ByteArrayOutputStream byteStream =
            new ByteArrayOutputStream();
        DataOutput dataOutput = new XDRDataOutputStream(byteStream);

        // write the DataSource id
        ID dataSourceID = dsp.getDataSource().getID();
        dataOutput.writeLong(dataSourceID.getMostSignificantBits());
        dataOutput.writeLong(dataSourceID.getLeastSignificantBits());

        // write type
        dataOutput.writeInt(dsp.getType().getValue());

        //System.err.println("DSP type = " +
        // dsp.getType().getValue());

        // write seqNo
        int seqNo = dsp.getSeqNo();
        dataOutput.writeInt(seqNo);

        // write object
        switch (dsp.getType()) {
        case ANNOUNCE:
            System.err.println("ANNOUNCE not implemented yet!");
            break;

        case MEASUREMENT:
            // extract Measurement from message object
            ProbeMeasurement measurement =
                ((MeasurementMessage)dsp).getMeasurement();
            // encode the measurement, ready for transmission
            MeasurementEncoder encoder = new MeasurementEncoder(
                measurement);
            encoder.encode(dataOutput);

            break;
        }

        //System.err.println("DP: " + dsp + " AS " + byteStream);

        // now tell the multicaster to transmit this byteStream
        udpTransmitter.transmit(byteStream, seqNo);

        return 1;
    } catch (TypeException te) {
        te.printStackTrace(System.err);
        return 0;
    }
}
}