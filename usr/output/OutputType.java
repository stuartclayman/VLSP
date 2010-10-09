package usr.output;

import usr.globalcontroller.*;
import usr.logging.*;

/** This class produces output from the simulation */

public class OutputType {

    public static final int AT_TIME= 1;
    public static final int AT_START= 2;
    public static final int AT_INTERVAL= 3;
    public static final int AT_END= 4;
    
    public static final int OUTPUT_NETWORK= 1;
    public static final int OUTPUT_SUMMARY= 2;
    
    private String fileName_="";
    private boolean clear_= true;
    private GlobalController globalController_= null;
    private int outputType_ = 0;  // What output is required
    private int outputTimeType_ = 0;  // Repeated or at time?
    private int outputTime_= 0;   // Time parameter
    
    
    public OutputType(GlobalController gc) {
        globalController_= gc;
    }
    
    /** Accessor for output type */
    public int getType() {
        return outputType_;    
    }
    
    /** Accessor for output time type */
    public int getTimeType() {
        return outputTimeType_;
    }
    
    /** Accessor for time parameter */
    public int getTime() {
        return outputTime_;
    } 

    /** Send output to appropriate place */
    public void createOutput(long time) {
    
    }

}
