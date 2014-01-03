package ikms.data;



/**
 * A Command object processes a command handled by the ManagementConsole
 * of a ComponentController.
 */
public abstract class AbstractRequestHandler implements RequestHandler {
    // The pattern this RequestHandler deals with
    String pattern;

    ManagementConsole managementConsole;

    /**
     * Construct a RequestHandler
     */
    protected AbstractRequestHandler() { }

    /**
     * Get the pattern this RequestHandler deals with.
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Set the pattern this RequestHandler deals with.
     */
    public void setPattern(String p) {
        pattern = p;
    }


    /**
     * Get the ManagementConsole this is a command for.
     */
    public ManagementConsole getManagementConsole() {
        return managementConsole;
    }

    /**
     * Set the ManagementConsole this is a command for.
     */
    public void setManagementConsole(ManagementConsole mc) {
        managementConsole = mc;
    }

    /**
     * Hash code
     */
    public int hashCode() {
        return pattern.hashCode();
    }

    /**
     * Create the String to print out before a message
     */
    protected String leadin() {
        final String MC = "RH: ";

        return MC;
    }
}

