package usr.output;

import java.io.PrintStream;
import usr.globalcontroller.GlobalController;

/** This interface is for any function producing output */
public interface OutputFunction {
 
  public void makeOutput(long time, PrintStream s, OutputType out, GlobalController gc);
}
