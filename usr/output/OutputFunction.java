package usr.output;

import java.io.PrintStream;
import usr.globalcontroller.GlobalController;
import usr.events.*;
import us.monoid.json.*;

/** This interface is for any function producing output */
public interface OutputFunction
{
public void makeOutput(long time, PrintStream s, OutputType out,
    GlobalController gc);
public void makeEventOutput(Event event, JSONObject result, 
    PrintStream s, OutputType out, GlobalController gc);
}
