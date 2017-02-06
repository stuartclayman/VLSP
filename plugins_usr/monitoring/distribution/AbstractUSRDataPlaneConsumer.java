// AbstractUSRDataPlaneConsumer.java
// Author: Stuart Clayman
// Email: sclayman@ee.ucl.ac.uk
// Date: Oct 2010

package plugins_usr.monitoring.distribution;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;

import usr.net.SocketAddress;
import eu.reservoir.monitoring.core.ID;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.MeasurementReceiver;
import eu.reservoir.monitoring.core.MeasurementReporting;
import eu.reservoir.monitoring.core.TypeException;
import eu.reservoir.monitoring.core.plane.DataPlane;
import eu.reservoir.monitoring.core.plane.DataPlaneMessage;
import eu.reservoir.monitoring.distribution.MetaData;
import eu.reservoir.monitoring.distribution.Receiving;

public abstract class AbstractUSRDataPlaneConsumer implements DataPlane, MeasurementReporting, Receiving {
    // The address we are sending to
    SocketAddress address;

    // We don't want to transmit measurement data.
    // Producers will only transmit, and Consumers will receive.

    // The USRReceiver
    USRReceiver udpReceiver;

    // the MeasurementReceiver
    MeasurementReceiver measurementReceiver;

    // This keeps the last seqNo from each DataSource that is seen
    HashMap<ID, Integer> seqNoMap;

    /**
     * Construct a AbstractUSRDataPlaneConsumer.
     */
    public AbstractUSRDataPlaneConsumer(SocketAddress addr){
        // sending address
        address = addr;

        seqNoMap = new HashMap<ID, Integer>(32);
    }

    /**
     * Connect to a delivery mechansim.
     */
    @Override
    public boolean connect(){
        try {
            // only connect if we're not already connected
            if (udpReceiver == null) {
                USRReceiver rr = new USRReceiver(this, address);

                rr.listen();

                udpReceiver = rr;

                return true;
            } else {
                return true;
            }
        } catch (IOException ioe) {
            // Current implementation will be to do a stack trace
            ioe.printStackTrace();

            return false;
        }
    }

    /**
     * Dicconnect from a delivery mechansim.
     */
    @Override
    public boolean disconnect(){
        if (udpReceiver != null) {
            try {
                udpReceiver.end();
                udpReceiver = null;
                return true;
            } catch (IOException ieo) {
                udpReceiver = null;
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Announce that the plane is up and running
     */
    @Override
    public boolean announce(){
        // do nothing currenty
        return true;
    }

    /**
     * Un-announce that the plane is up and running
     */
    @Override
    public boolean dennounce(){
        // do nothing currenty
        return true;
    }

    /**
     * This method is called just after a packet
     * has been received from some underlying transport
     * at a particular address.
     * The expected message is XDR encoded and it's structure is:
     * +-------------------------------------------------------------------+
     * | data source id (long) | msg type (int) | seq no (int) | payload   |
     * +-------------------------------------------------------------------+
     */
    @Override
    public abstract void received(ByteArrayInputStream bis, MetaData metaData) throws IOException, TypeException;
    /**
     * This method is called just after there has been an error
     * in received from some underlying transport.
     * This passes the exception into the Receiving object.
     */

    /**
     * This method is called just after there has been EOF
     * in received from some underlying transport.
     */
    @Override
    public void eof(){
        disconnect();
    }

    @Override
    public void error(Exception e){
        System.err.println(
                           "DataConsumer: notified of error " + e.getMessage());
        System.err.println("Stack Trace:");
        e.printStackTrace(System.err);
    }

    /**
     * Send a message.
     */
    @Override
    public int sendData(DataPlaneMessage dpm) throws Exception {
        // currenty do nothing
        return -1;
    }

    /**
     * This method is called just after a message
     * has been sent to the underlying transport.
     */
    @Override
    public boolean sentData(int id){
        return false;
    }

    /**
     * Receiver of a measurment, with an extra object that has context info
     */
    @Override
    public Measurement report(Measurement m){
        //System.err.println("USRDataPlaneConsumer: got " + m);
        measurementReceiver.report(m);
        return m;
    }

    /**
     * Set the object that will recieve the measurements.
     */
    @Override
    public Object setMeasurementReceiver(MeasurementReceiver mr){
        Object old = measurementReceiver;

        measurementReceiver = mr;
        return old;
    }
}
