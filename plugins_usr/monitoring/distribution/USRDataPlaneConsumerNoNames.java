// USRDataPlaneConsumerNoNames.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.monitoring.distribution;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import usr.net.SocketAddress;
import eu.reservoir.monitoring.core.ID;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.MeasurementReporting;
import eu.reservoir.monitoring.core.TypeException;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.core.plane.MessageType;
import eu.reservoir.monitoring.distribution.ConsumerMeasurementWithMetaData;
import eu.reservoir.monitoring.distribution.MeasurementDecoder;
import eu.reservoir.monitoring.distribution.MessageMetaData;
import eu.reservoir.monitoring.distribution.MetaData;
import eu.reservoir.monitoring.distribution.Receiving;
import eu.reservoir.monitoring.distribution.XDRDataInputStream;

public class USRDataPlaneConsumerNoNames extends
AbstractUSRDataPlaneConsumer implements DataPlane, MeasurementReporting,
Receiving
{
/**
 * Construct a USRDataPlaneConsumerNoNames.
 */
public USRDataPlaneConsumerNoNames(SocketAddress addr){
    super(addr);
}

/**
 * This method is called just after a packet
 * has been received from some underlying transport
 * at a particular address.
 * The expected message is XDR encoded and it's structure is:
 *
 ***************************+---------------------------------------------------------------------+
 * | data source id (2 X long) | msg type (int) | seq no (int) | payload
 *|
 *
 ***************************+---------------------------------------------------------------------+
 */
@Override
public void received(ByteArrayInputStream bis,
    MetaData metaData) throws IOException,
TypeException {
    //System.out.println("DC: Received " + metaData);

    try {
    	XDRDataInputStream dataIn = new XDRDataInputStream(bis);

        //System.err.println("DC: datainputstream available = " +
        // dataIn.available());

        // get the DataSource id
        // get the DataSource id
        long dataSourceIDMSB = dataIn.readLong();
        long dataSourceIDLSB = dataIn.readLong();
        ID dataSourceID = new ID(dataSourceIDMSB, dataSourceIDLSB);

        // check message type
        int type = dataIn.readInt();

        MessageType mType = MessageType.lookup(type);

        // delegate read to right object
        if (mType == null) {
        	dataIn.close();
            //System.err.println("type = " + type);
            return;
        }

        // get seq no
        int seq = dataIn.readInt();

        /*
         * Check the DataSource seq no.
         */
        if (seqNoMap.containsKey(dataSourceID)) {
            // we've seen this DataSource before
            int prevSeqNo = seqNoMap.get(dataSourceID);

            if (seq == prevSeqNo + 1) {
                // we got the correct message from that DataSource
                // save this seqNo
                seqNoMap.put(dataSourceID, seq);
            } else {
                // a DataSource message is missing
                // TODO: decide what to do
                // currently: save this seqNo
                seqNoMap.put(dataSourceID, seq);
            }
        } else {
            // this is a new DataSource
            seqNoMap.put(dataSourceID, seq);
        }

        //System.err.println("Received " + type + ". mType " + mType
        // +
        // ". seq " + seq);

        // Message meta data
        MessageMetaData msgMetaData =
            new MessageMetaData(dataSourceID, seq,
                mType);

        // read object and check it's type
        switch (mType) {
        case ANNOUNCE:
            System.err.println("ANNOUNCE not implemented yet!");
            break;

        case MEASUREMENT:
            // decode the bytes into a measurement object
            MeasurementDecoder decoder = new MeasurementDecoder();
            Measurement measurement = decoder.decode(dataIn);

            if (measurement instanceof
                ConsumerMeasurementWithMetaData) {
                // add the meta data into the Measurement
                ((ConsumerMeasurementWithMetaData)
                 measurement).
                setMessageMetaData(msgMetaData);
                ((ConsumerMeasurementWithMetaData)
                 measurement).
                setTransmissionMetaData(metaData);
            }

            //System.err.println("DC: datainputstream left = " +
            // dataIn.available());
            // report the measurement
            report(measurement);
            //System.err.println("DC: m = " + measurement);
            break;
        }
    } catch (IOException ioe) {
        System.err.println(
            "DataConsumer: failed to process measurement input. The Measurement data is likely to be bad.");
        throw ioe;
    } catch (Exception e) {
        System.err.println(
            "DataConsumer: failed to process measurement input. The Measurement data is likely to be bad.");
        throw new TypeException(e.getMessage());
    }
}
}
