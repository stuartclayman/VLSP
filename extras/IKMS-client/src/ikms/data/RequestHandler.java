package ikms.data;

import org.simpleframework.http.Response;
import org.simpleframework.http.Request;

/**
 * An interface for classes that handle a bunch of requests.
 */
public interface RequestHandler {
    /**
     * Handle a request and send a response.
     */
    public void  handle(Request request, Response response);

    /**
     * Get the pattern this RequestHandler deals with.
     */
    public String getPattern();

    /**
     * Set the pattern this RequestHandler deals with.
     */
    public void setPattern(String pattern);

    /**
     * Get the ManagementConsole this is a command for.
     */
    public ManagementConsole getManagementConsole();

    /**
     * Set the ManagementConsole 
     */
    public void setManagementConsole(ManagementConsole console);
}
