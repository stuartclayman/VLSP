package usr.output;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import us.monoid.json.JSONObject;
import usr.events.Event;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;

/** This class produces output from the simulation */

public class OutputType {

    public static final int AT_TIME = 1;
    public static final int AT_START = 2;
    public static final int AT_INTERVAL = 3;
    public static final int AT_END = 4;
    public static final int AT_EVENT = 5;

    Logger mylog = null;
    private String fileName_ = "";
    private OutputFunction outputFunction_ = null;
    private boolean clear_ = true;
    private int outputTimeType_ = 0;  // Repeated or at time?
    private int outputTime_ = 0;   // Time parameter
    private boolean firstOutput_ = true;
    String parameter_ = "";

    public OutputType() {
    }

    /** Set type from string*/
    public void setType(String t) throws java.lang.IllegalArgumentException {

        //  These names are defined for legacy reasons -- please use
        // class name in future
        if (t.equals("Network")) {
            outputFunction_ = new OutputNetwork();
            return;
        }

        if (t.equals("Summary")) {
            outputFunction_ = new OutputSummary();
            return;
        }

        if (t.equals("Traffic")) {
            outputFunction_ = new OutputTraffic();
            return;
        }
        try {
            java.lang.Class<?> func = java.lang.Class.forName(t).asSubclass(OutputFunction.class);
            Constructor<?> c = func.getConstructor();
            outputFunction_ = (OutputFunction)c.newInstance();

        } catch (ClassCastException e) {
            throw new java.lang.
                  IllegalArgumentException("Class name " + t
                                           + " must be valid class name implementing OutputFunction");
        } catch (ClassNotFoundException e) {
            throw new java.lang.IllegalArgumentException("Class name " + t +
                                   " must be valid class name implementing OutputFunction");
        } catch (NoSuchMethodException ie) {
            throw new java.lang.IllegalArgumentException("Cannot construct -- No such method " +
            		ie.getMessage() +" "+ t);
        } catch (InstantiationException ie) {
            throw new java.lang.IllegalArgumentException("Cannot construct -- Instantiation exception " +
            		ie.getMessage() +" " + t);
        } catch (IllegalAccessException ie) {
            throw new java.lang.IllegalArgumentException("Cannot construct -- Illegal Argument " +
            		ie.getMessage() +" "+ t);
        } catch (InvocationTargetException ie) {
            throw new java.lang.IllegalArgumentException("Cannot construct -- InvocationTargetException " +
            		ie.getMessage() +" "+t);
        }
    }

    /** Accessor for output time type */
    public int getTimeType() {
        return outputTimeType_;
    }

    /** Set time type from string*/
    public void setTimeType(String tt) throws java.lang.IllegalArgumentException {
        if (tt.equals("Start")) {
            outputTimeType_ = AT_START;
            return;
        }

        if (tt.equals("End")) {
            outputTimeType_ = AT_END;
            return;
        }

        if (tt.equals("Interval")) {
            outputTimeType_ = AT_INTERVAL;
            return;
        }

        if (tt.equals("Time")) {
            outputTimeType_ = AT_TIME;
            return;
        }

        if (tt.equals("Event")) {
            outputTimeType_ = AT_EVENT;
            return;
        }


        throw new java.lang.IllegalArgumentException("Cannot parse Time Type "+tt);
    }

    /** Clear output file at start of run */
    public boolean clearOutputFile() {
        return clear_;
    }

    /** Accessor for time parameter */
    public int getTime() {
        return outputTime_;
    }

    /** Setter for time */
    public void setTime(int t) {
        outputTime_ = t;
    }

    /** Accessor for file name */
    public String getFileName () {
        return fileName_;
    }

    /** Set file name */
    public void setFileName (String name) {
        fileName_ = name;
    }

    /** Setter function for string parameter */
    public void setParameter(String parm) {
        parameter_ = parm;
    }

    /** Accessor function for string parameter*/
    public String getParameter() {
        return parameter_;
    }

    /** Is this the first time output produced by this type*/
    public boolean isFirst() {
        return firstOutput_;
    }

    /** Set whether this is first time output produced by this type*/
    public void setFirst(boolean f) {
        firstOutput_ = f;
    }

    /** Get output function */
    public OutputFunction getOutputClass() {
        return outputFunction_;
    }

    /** Create the required output */
    public void makeOutput(long time, PrintStream s, GlobalController gc) {
        outputFunction_.makeOutput(time, s, this, gc);
    }

    /** Create the required output after an event */
    public void makeEventOutput(Event event, JSONObject result, PrintStream s, GlobalController gc) {
        outputFunction_.makeEventOutput(event, result, s, this, gc);
    }

    public void parseExtraXML(Node n) throws SAXException {
        outputFunction_.parseExtraXML(n);
    }

    /**
     * To String
     */
    @Override
	public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(outputFunction_.toString());

        builder.append(" ");

        switch (outputTimeType_) {
        case AT_START :
            builder.append("AT_START");
            break;
        case AT_END:
            builder.append("AT_END");
            break;
        case AT_INTERVAL:
            builder.append("AT_INTERVAL");
            break;
        case AT_TIME:
            builder.append("AT_TIME");
            break;
        default:
            break;
        }

        builder.append(" ");
        builder.append(outputTime_);
        builder.append(" "+parameter_);
        return builder.toString();
    }

}
