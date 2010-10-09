package usr.applications;

/**
import usr.logging.*;
 * A package for user applications
 */
public interface Application extends Runnable {
    
    /** Start application*/
    public boolean start();
    
    /** Is this application the same as another */
    
    
    
    /** Implement graceful shut down */
    public boolean stop();
    
    /** Emergency hard exit */
    public void exit(int code);
}
