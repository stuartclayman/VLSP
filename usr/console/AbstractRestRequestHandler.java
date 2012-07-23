package usr.console;

import usr.logging.*;
import usr.common.BasicRouterInfo;
import usr.console.RequestHandler;
import usr.console.AbstractRequestHandler;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import us.monoid.json.*;
import java.util.Scanner;
import java.io.PrintStream;
import java.io.IOException;

/**
 * A class to handle REST requests
 */
public abstract class AbstractRestRequestHandler extends AbstractRequestHandler implements RequestHandler {


    /**
     * Complain with an error - HTTP error code 400
     */
    protected AbstractRestRequestHandler badRequest(Response response, String msg) throws IOException, JSONException {
        PrintStream out = response.getPrintStream();

        response.setCode(400);

        JSONObject jsobj = new JSONObject();
        jsobj.put("error", msg);

        out.println(jsobj.toString());

        return this;
    }


    /**
     * Complain with an error - HTTP error code 403
     */
    protected AbstractRestRequestHandler complain(Response response, String msg) throws IOException, JSONException {
        PrintStream out = response.getPrintStream();

        response.setCode(403);

        JSONObject jsobj = new JSONObject();
        jsobj.put("error", msg);

        out.println(jsobj.toString());

        return this;
    }


    /**
     * Complain with an error - HTTP error code 404
     */
    protected AbstractRestRequestHandler notFound(Response response, String msg) throws IOException, JSONException {
        PrintStream out = response.getPrintStream();

        response.setCode(404);

        JSONObject jsobj = new JSONObject();
        jsobj.put("error", msg);

        out.println(jsobj.toString());

        return this;
    }


}
