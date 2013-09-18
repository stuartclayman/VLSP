package demo_usr.viewer;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import demo_usr.httpd.NanoHTTPD;

/**
 * An example of subclassing NanoHTTPD to make a custom HTTP server.
 */
public class WebServer extends NanoHTTPD
{
// The Viewer of the Network
Viewer viewer = null;

/**
 * Construct a WebServer
 */
public WebServer() throws IOException {
    super(8080);
}

/**
 * Construct a WebServer
 */
public WebServer(int port) throws IOException {
    super(port);
}

/**
 * Serve up a URL.
 */
@Override
public Response serve(String uri,
    String method,
    Properties header,
    Properties parms,
    Properties files)
{
    System.out.println(method + " '" + uri + "' " + parms);

    Response response = null;

    if (uri.equals("/GRAPH")) {
        String msg = null;

        if (viewer == null) {
            msg = "graph E { \"Viewer not connected yet. \" }";

            response = new NanoHTTPD.Response(HTTP_OK, MIME_HTML,
                msg);
        } else {
            // get param "ref"
            String value = parms.getProperty("ref");

            System.err.println(
                "WebServer: About to get snapshot " + value);

            // try and get the nth snapshot
            String data = viewer.getSnapshot(value);

            if (data == null || data.equals(""))
                // get latest snapshot
                msg = viewer.getData();
            else
                msg = data;

            response = new NanoHTTPD.Response(HTTP_OK, MIME_HTML,
                msg);
            response.addHeader("Set-Cookie",
                "xdot=" + viewer.getSnapshotNumber());
        }

        return response;
    } else {
        return super.serve(uri, method, header, parms, files);
    }
}

/**
 * Serve up a File.
 * Root is demo_usr/canviz
 */
@Override
public Response serveFile(String uri,
    Properties header,
    File homeDir,
    boolean allowDirectoryListing)
{
    return super.serveFile(uri, header, new File(
            "demo_usr/canviz/"), true);
}

/**
 * Set the Viewer object.
 */
void setViewer(Viewer v){
    viewer = v;
}
}