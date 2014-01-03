package ikms.data;

import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Response;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;


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

