package usr.test;

import static us.monoid.web.Resty.content;
import static us.monoid.web.Resty.delete;
import static us.monoid.web.Resty.form;
import static us.monoid.web.Resty.put;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

/**
 * Test some calls to GlobalController using Resty
 */
class RestyTest {


    /**
     * Equivalent of: curl -X POST http://localhost:8888/router/
     *
     * Returns JSONObject: {"address":"1","mgmtPort":11000,"name":"Router-1","r2rPort":11001,"routerID":1}
     */
    public JSONObject createRouter() {
        try {
            Resty rest = new Resty();

            String uri = "http://localhost:8888/router/";

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            System.err.println("createRouter FAILED");
        } catch (JSONException je) {
            System.err.println("createRouter FAILED");
        }

        throw new Error();

    }

    /**
     * Equivalent of: curl -X POST http://localhost:8888/router/
     *
     * Returns JSONObject: {"address":"1","mgmtPort":11000,"name":"Router-1","r2rPort":11001,"routerID":1}
     */
    public JSONObject createRouter(String name) {
        try {
            Resty rest = new Resty();

            String uri = "http://localhost:8888/router/?name=" + name;

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            System.err.println("createRouter FAILED");
        } catch (JSONException je) {
            System.err.println("createRouter FAILED");
        }

        throw new Error();

    }

    /**
     * Equivalent of:  DELETE http://localhost:8888/router/1
     *
     * Returns JSONObject: {"status":"done"}
     */
    public JSONObject deleteRouter(int routerID) {
        try {
            Resty rest = new Resty();

            String uri = "http://localhost:8888/router/" + routerID;

            // Delete
            JSONObject jsobj = rest.json(uri, delete()).toObject();

            return jsobj;

        } catch (IOException ioe) {
            System.err.println("deleteRouter FAILED");
        } catch (JSONException je) {
            System.err.println("deleteRouter FAILED");
        }

        throw new Error();

    }

    /**
     * Equivalent of: curl http://localhost:8888/router/
     *
     * Returns JSONObject:  {"list":[12,6,5,7,8,9,10,1,3,11,4],"type":"router"}
     */
    public JSONObject listRouters() {
        try {
            Resty rest = new Resty();

            String uri = "http://localhost:8888/router/";

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            System.err.println("listRouters FAILED");
        } catch (JSONException je) {
            System.err.println("listRouters FAILED");
        }

        throw new Error();

    }



    /**
     * Equivalent of: POST http://localhost:8888/link/?router1=1&router2=2
     *
     * Returns JSONObject: {"linkID":196612,"linkName":"Router-1.Connection-0","router1":1,"router2":2,"weight":1}
     */
    public JSONObject createLink(int routerID1, int routerID2, int weight) {
        try {
            Resty rest = new Resty();

            String uri = "http://localhost:8888/link/?router1=" + routerID1 + "&router2=" + routerID2+"&weight="+weight;

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            System.err.println("createLink FAILED");
        } catch (JSONException je) {
            System.err.println("createLink FAILED");
        }

        throw new Error();

    }

    /**
     * Equivalent of:  PUT http://localhost:8888/link/196612?weight=w
     *
     * Returns JSONObject: {"status":"done"}
     */
    public JSONObject setLinkWeight(int linkID, int weight) {
        try {
            Resty rest = new Resty();

            String uri = "http://localhost:8888/link/" + linkID + "?weight="+weight;

            // PUT
            JSONObject jsobj = rest.json(uri, put(content(""))).toObject();

            return jsobj;

        } catch (IOException ioe) {
            System.err.println("setLinkWeight FAILED");
        } catch (JSONException je) {
            System.err.println("setLinkWeight FAILED");
        }

        throw new Error();

    }

    /**
     * Equivalent of:  DELETE http://localhost:8888/link/196612
     *
     * Returns JSONObject: {"status":"done"}
     */
    public JSONObject deleteLink(int linkID) {
        try {
            Resty rest = new Resty();

            String uri = "http://localhost:8888/link/" + linkID;

            // Delete
            JSONObject jsobj = rest.json(uri, delete()).toObject();

            return jsobj;

        } catch (IOException ioe) {
            System.err.println("deleteLink FAILED");
        } catch (JSONException je) {
            System.err.println("deleteLink FAILED");
        }

        throw new Error();

    }

    /**
     * Equivalent of: curl http://localhost:8888/link/
     *
     * Returns JSONObject: {"list":[786553,5505145,7864441],"type":"link"}
     */
    public JSONObject listLinks() {
        try {
            Resty rest = new Resty();

            String uri = "http://localhost:8888/link/";

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            System.err.println("listLinks FAILED");
        } catch (JSONException je) {
            System.err.println("listLinks FAILED");
        }

        throw new Error();

    }



    /**
     * Equivalent of: POST http://localhost:8888/router/2/app/?className=usr.applications.Recv&args=4000

     *
     * Returns JSONObject: {"aid":1,"id":262145,"name":"/Router-2/App/usr.applications.Recv/1","routerID":2}
     */
    public JSONObject createApp(int routerID, String className, String args) {
        try {
            Resty rest = new Resty();

            String uri = "http://localhost:8888/router/" + routerID + "/app/?className=" + className + "&args=" + java.net.URLEncoder.encode(args, "UTF-8");

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            System.err.println("createApp FAILED");
        } catch (JSONException je) {
            System.err.println("createApp FAILED");
        }

        throw new Error();

    }



}

