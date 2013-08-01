// TimedPrintWriter.java

package usr.logging;

import java.io.PrintWriter;
import java.io.FileOutputStream;

/**
 * A class that adds a time before each println.
 */
public class TimedPrintWriter extends PrintWriter {
    public TimedPrintWriter(FileOutputStream fos, boolean flush) {
        super(fos, flush);
    }

    /**
     * Print a String and then terminate the line. This method behaves
     * as though it invokes print(String) and then println().
     */
    public void println(String x) {
        super.println(x);
    }

}