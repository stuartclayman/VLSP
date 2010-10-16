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
    
    Logger mylog= null;
    private String fileName_="";
    private boolean clear_= true;
    private int outputType_ = 0;  // What output is required
    private int outputTimeType_ = 0;  // Repeated or at time?
    private int outputTime_= 0;   // Time parameter
    private boolean firstOutput_= true;
    
    public OutputType() {
    }
    
    /** Accessor for output type */
    public int getType() {
        return outputType_;    
    }
    
    /** Set type from string*/
    public void setType(String t) throws java.lang.IllegalArgumentException {
    
        if (t.equals("Network")) {
            outputType_= OUTPUT_NETWORK;
            return;
        }
        if (t.equals("Summary")) {
            outputType_= OUTPUT_SUMMARY;
            return;
        } 
        throw new java.lang.IllegalArgumentException("Cannot parse Type "+t);
    }
    
    /** Accessor for output time type */
    public int getTimeType() {
        return outputTimeType_;
    }
    
    /** Set time type from string*/
    public void setTimeType(String tt) throws java.lang.IllegalArgumentException {
        if (tt.equals("Start")) {
            outputTimeType_= AT_START;
            return;
        } 
        if (tt.equals("End")) {
            outputTimeType_= AT_END;
            return;
        }
        if (tt.equals("Interval")) {
            outputTimeType_= AT_INTERVAL;
            return;
        }
        if (tt.equals("Time")) {
            outputTimeType_= AT_TIME;
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
        outputTime_= t;
    }
    
    /** Accessor for file name */
    public String getFileName ()
    {
        return fileName_;
    }
            
    /** Set file name */
    public void setFileName (String name) 
    {
        fileName_= name;
    } 
    
    /** Is this the first time output produced by this type*/
    public boolean isFirst() {
        return firstOutput_;
    }
    
    /** Set whether this is first time output produced by this type*/
    public void setFirst(boolean f) {
        firstOutput_= f;
    }
    
    

}
