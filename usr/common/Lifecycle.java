package usr.common;


/**
 * Some lifecycle methods
 */
public interface Lifecycle {
    /**
     * Init
     */
    public boolean init();

    /**
     * Start
     */
    public boolean start();

    /**
     * Stop
     */
    public boolean stop();
}
