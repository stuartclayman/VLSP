package usr.vim;

import static us.monoid.web.Resty.content;
import static us.monoid.web.Resty.delete;
import static us.monoid.web.Resty.form;
import static us.monoid.web.Resty.put;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;
import usr.logging.Logger;
import usr.logging.USR;

/**
 * Makes REST calls to VIM / GlobalController using Resty
 */
public class VimClient implements VimFunctions {
    // A URI for a VIM / GlobalController to interact with
    String vimURI;
    Resty rest;
    int port;

    /**
     * Construct a VimClient
     * using defaults of localhost and port 8888
     */
    public VimClient() throws UnknownHostException, IOException {
        this("localhost", 8888);
    }

    /**
     * Constructor for a VimClient
     * to the ManagementConsole of a VIM / GlobalController.
     * @param addr the name of the host
     * @param port the port the server is listening on
     */
    public VimClient(String addr, int port) throws UnknownHostException, IOException  {
        initialize(InetAddress.getByName(addr), port);
    }

    /**
     * Constructor for a VimClient
     * to the ManagementConsole of a VIM / GlobalController.
     * @param addr the InetAddress of the host
     * @param port the port the server is listening on
     */
    public VimClient(InetAddress addr, int port) throws UnknownHostException, IOException  {
        initialize(addr, port);
    }

    /**
     * Initialize
     */
    private synchronized void initialize(InetAddress addr, int port) {
        this.port = port;
        vimURI = "http://" + addr.getHostName() + ":" + Integer.toString(port);

        Logger.getLogger("log").logln(USR.STDOUT, "globalControllerURI: " + vimURI);

        rest = new Resty();
    }

    /**
     * Get the port this VimClient is connecting to
     */
    public int getPort() {
        return port;
    }

    /**
     * Equivalent of: curl -X POST http://localhost:8888/router/
     *
     * Returns JSONObject: {"address":"1","mgmtPort":11000,"name":"Router-1","r2rPort":11001,"routerID":1}
     */
    public JSONObject createRouter() throws JSONException {
        try {
            String uri = vimURI + "/router/";

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("createRouter FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl -X POST http://localhost:8888/router/?parameters=ppp
     *
     * Returns JSONObject: {"address":"1","parameters":"-c%201","mgmtPort":11000,"name":"Router-1","r2rPort":11001,"routerID":1}
     */
    public JSONObject createRouter(String parameters) throws JSONException {
        try {
            String uri = vimURI + "/router/?parameters=" +  parameters.replace(" ", "%20");

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();
            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("createRouter FAILED" + " IOException: " + ioe.getMessage());
        }
    }
    
    /**
     * Equivalent of: curl -X POST http://localhost:8888/router/?name=nnn&address=aaa
     *
     * Returns JSONObject: {"address":"1","mgmtPort":11000,"name":"Router-1","r2rPort":11001,"routerID":1}
     */
    public JSONObject createRouter(String name, String address) throws JSONException {
        try {
            String uri = vimURI + "/router/?name=" + name + "&address=" + address;

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("createRouter FAILED" + " IOException: " + ioe.getMessage());
        }
    }
    
    // added new createRouter method that supports extra parameter passing (Lefteris)
    /**
     * Equivalent of: curl -X POST http://localhost:8888/router/?name=nnn&address=aaa&parameters=ccc
     *
     * Returns JSONObject: {"address":"1","parameters":"-c%201","mgmtPort":11000,"name":"Router-1","r2rPort":11001,"routerID":1}
     */
    public JSONObject createRouter(String name, String address, String parameters) throws JSONException {
        try {
            String uri = vimURI + "/router/?name=" + name + "&address=" + address + "&parameters=" + parameters.replace(" ", "%20");

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("createRouter FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl -X POST http://localhost:8888/router/?name=nnn
     *
     * Returns JSONObject: {"address":"1","mgmtPort":11000,"name":"Router-1","r2rPort":11001,"routerID":1}
     */
    public JSONObject createRouterWithName(String name) throws JSONException {
        try {
            String uri = vimURI + "/router/?name=" + name;

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("createRouter FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl -X POST http://localhost:8888/router/?address=aaa
     *
     * Returns JSONObject: {"address":"1","mgmtPort":11000,"name":"Router-1","r2rPort":11001,"routerID":1}
     */
    public JSONObject createRouterWithAddress(String address) throws JSONException {
        try {
            String uri = vimURI + "/router/?address=" + address;

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("createRouter FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of:  DELETE http://localhost:8888/router/1
     *
     * Returns JSONObject: {"status":"done"}
     */
    public JSONObject deleteRouter(int routerID) throws JSONException {
        try {
            String uri = vimURI + "/router/" + routerID;
            JSONObject jsobj = rest.json(uri, delete()).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("deleteRouter FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl GET http://localhost:8888/router/
     *
     * Returns JSONObject:  {"list":[12,6,5,7,8,9,10,1,3,11,4],"type":"router"}
     */
    public JSONObject listRouters() throws JSONException {
        try {
            String uri = vimURI + "/router/";

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("listRouters FAILED" + " IOException: " + ioe.getMessage());
        }
    }
    
    /**
     * Equivalent of: curl GET http://localhost:8888/localcontroller/
     *
     * Returns JSONObject:  {TBA}
     */
    public JSONObject listLocalControllers() throws JSONException {
        try {
            String uri = vimURI + "/localcontroller/";

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("listLocalControllers FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl GET http://localhost:8888/router/?detail=[id | all]
     *
     * Returns JSONObject:  {"list":[12,6,5,7,8,9,10,1,3,11,4],"type":"router"}
     */
    public JSONObject listRouters(String arg) throws JSONException {
        try {
            String uri;

            if (arg == null || arg.equals("")) {
                uri = vimURI + "/router/";
            } else if (!arg.contains("=")) {
                uri = vimURI + "/router/?detail" + arg;
            } else if (arg.startsWith("detail=")) {
                uri = vimURI + "/router/?" + arg;
            } else if (arg.startsWith("name=")) {
                uri = vimURI + "/router/?" + arg;
            } else if (arg.startsWith("address=")) {
                uri = vimURI + "/router/?" + arg;
            } else {
                throw new Error();
            }

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("listRouters FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl GET http://localhost:8888/removed/
     *
     * Returns JSONObject:  {"list":[12,6,5,7,8,9,10,1,3,11,4],"type":"shutdown"}
     */
    public JSONObject listRemovedRouters() throws JSONException {
        try {
            String uri = vimURI + "/removed/";

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("listRemovedRouters FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl GET http://localhost:8888/router/id
     *
     * Returns JSONObject:  {"routerID": 2, "address": "2", "name":"Router-2", "links": [1], "mgmtPort": 11003, "r2rPort": 11004, "time": 1361817254727}
     */
    public JSONObject getRouterInfo(int id) throws JSONException {
        try {
            String uri = vimURI + "/router/" + id;

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getRouterInfo FAILED" + " IOException: " + ioe.getMessage());
        }
    }
    
    /**
     * Equivalent of: curl GET http://localhost:8888/localcontroller/name
     *
     * Returns JSONObject:  {TBA}
     */
    public JSONObject getLocalControllerInfo(String name) throws JSONException {
        try {
            String uri = vimURI + "/localcontroller/" + name;

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getLocalControllerInfo FAILED" + " IOException: " + ioe.getMessage());
        }
    }


    /**
     * Equivalent of: curl PUT http://localhost:8888/localcontroller/name?status=online
     *
     * Returns JSONObject:  {TBA}
     */
    public JSONObject setLocalControllerStatus(String name, String status) throws JSONException {
        try {
            String uri = vimURI + "/localcontroller/" + name + "?status="+status;

            // PUT
            JSONObject jsobj = rest.json(uri, put(content(""))).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("setLocalControllerStatus FAILED" + " IOException: " + ioe.getMessage());
        }
    }



    /**
     * Equivalent of: curl GET http://localhost:8888/router/id/link_stats
     *
     * Returns JSONObject:  { "routerID": 7, "type": "link_stats", "links": [12 30 104], "link_stats": [ ["Router-12 Router-7.Connection-2" 358050 1714 0 21 337824 1656 228431 1078 0 0 205428 1007 0 1 0 1], ["Router-30 Router-7.Connection-7" 155976 715 0 5 136884 671 73858 302 0 0 53040 260 0 1 0 2], ["Router-104 Router-7.Connection-19" 50203 232 0 0 45900 225 2067 3 0 0 0 0 0 1 0 1] ] }
     *
     * <p>
     * link_stats fields match:
     * name | InBytes | InPackets | InErrors | InDropped | InDataBytes | InDataPackets | OutBytes | OutPackets | OutErrors |
     * OutDropped | OutDataBytes | OutDataPackets | InQueue | BiggestInQueue | OutQueue | BiggestOutQueue |
     */
    public JSONObject getRouterLinkStats(int id) throws JSONException {
        try {
            String uri = vimURI + "/router/" + id + "/link_stats";

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getRouterInfo FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl GET http://localhost:8888/router/id/link_stats/dst
     *
     * Returns JSONObject:  { "routerID": 7, "type": "link_stats", "links": [12], "link_stats": [ ["Router-12 Router-7.Connection-2" 358050 1714 0 21 337824 1656 228431 1078 0 0 205428 1007 0 1 0 1] ] }
     *
     * <p>
     * link_stats fields match:
     * name | InBytes | InPackets | InErrors | InDropped | InDataBytes | InDataPackets | OutBytes | OutPackets | OutErrors |
     * OutDropped | OutDataBytes | OutDataPackets | InQueue | BiggestInQueue | OutQueue | BiggestOutQueue |
     */
    public JSONObject getRouterLinkStats(int id, int dstID) throws JSONException {
        try {
            String uri = vimURI + "/router/" + id + "/link_stats/" + dstID;

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getRouterInfo FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl GET http://localhost:8888/router/count
     *
     * Returns JSONObject:  { "value:" 5 }
     */
    public JSONObject getRouterCount() throws JSONException {
        try {
            String uri = vimURI + "/router/" + "count";

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getRouterCount FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl GET http://localhost:8888/router/maxid
     *
     * Returns JSONObject:  { "value:" 5 }
     */
    public JSONObject getMaxRouterID() throws JSONException {
        try {
            String uri = vimURI + "/router/" + "maxid";

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getMaxRouterID FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: POST http://localhost:8888/link/?router1=1&router2=2
     *
     * Returns JSONObject: {"linkID":196612,"linkName":"Router-1.Connection-0","router1":1,"router2":2,"weight":1}
     */
    public JSONObject createLink(int routerID1, int routerID2) throws JSONException {
        try {
            int weight = 1;
            String uri = vimURI + "/link/?router1=" + routerID1 + "&router2=" + routerID2+"&weight="+weight;

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("createLink FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: POST http://localhost:8888/link/?router1=1&router2=2&weight=www
     *
     * Returns JSONObject: {"linkID":196612,"linkName":"Router-1.Connection-0","router1":1,"router2":2,"weight":1}
     */
    public JSONObject createLink(int routerID1, int routerID2, int weight) throws JSONException {
        try {
            String uri = vimURI + "/link/?router1=" + routerID1 + "&router2=" + routerID2+"&weight="+weight;

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("createLink FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: POST http://localhost:8888/link/?router1=1&router2=2&weight=www&linkName=name
     *
     * Returns JSONObject: {"linkID":196612,"linkName":"Router-1.Connection-0","router1":1,"router2":2,"weight":1}
     */
    public JSONObject createLink(int routerID1, int routerID2, int weight, String linkName) throws JSONException {
        try {
            String uri = vimURI + "/link/?router1=" + routerID1 + "&router2=" + routerID2+"&weight="+weight+"&linkName="+linkName;

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("createLink FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of:  DELETE http://localhost:8888/link/196612
     *
     * Returns JSONObject: {"status":"done"}
     */
    public JSONObject deleteLink(int linkID) throws JSONException {
        try {
            String uri = vimURI + "/link/" + linkID;

            // Delete
            JSONObject jsobj = rest.json(uri, delete()).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("deleteLink FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl http://localhost:8888/link/
     *
     * Returns JSONObject: {"list":[786553,5505145,7864441],"type":"link"}
     */
    public JSONObject listLinks() throws JSONException {
        try {
            String uri = vimURI + "/link/";

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("listLinks FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl http://localhost:8888/link/?detail=[id | all]
     *
     * Returns JSONObject: {"list":[786553,5505145,7864441],"type":"link"}
     */
    public JSONObject listLinks(String detail) throws JSONException {
        try {
            String uri = vimURI + "/link/?detail=" + detail;

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("listLinks FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl http://localhost:8888/link/id
     *
     * Returns JSONObject: {"linkID":8, "linkName":"Router-5.Connection-0", "weight":10, "nodes":[5,6], "time":1362079709109}
     */
    public JSONObject getLinkInfo(int id) throws JSONException {
        try {
            String uri = vimURI + "/link/" + id;

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getLinkInfo FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of:  PUT http://localhost:8888/link/196612?weight=w
     *
     * Returns JSONObject: {"linkID":8, "linkName":"Router-5.Connection-0", "weight":30, "nodes":[5,6], "time":1362079709109}
     */
    public JSONObject setLinkWeight(int linkID, int weight) throws JSONException {
        try {
            String uri = vimURI + "/link/" + linkID + "?weight="+weight;

            // PUT
            JSONObject jsobj = rest.json(uri, put(content(""))).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("setLinkWeight FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl http://localhost:8888/link/count
     *
     * Returns JSONObject: { "value:" 10 }
     */
    public JSONObject getLinkCount() throws JSONException {
        try {
            String uri = vimURI + "/link/" + "count";

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getLinkCount FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl GET http://localhost:8888/router/id/link
     *
     * Returns JSONObject:  {"routerID": 9,"type":" link", "list":[9830481,7078009,6488164]}
     */
    public JSONObject listRouterLinks(int routerID) throws JSONException {
        try {
            String uri = vimURI + "/router/" + routerID + "/link/";

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getRouterInfo FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl GET http://localhost:8888/router/id/link/?attr=[id | name | weight | connected ]
     *
     * Returns JSONObject:  {"routerID": 9,"type":" link", "list":[9830481,7078009,6488164]}
     */
    public JSONObject listRouterLinks(int rid, String attr) throws JSONException {
        try {
            String uri = vimURI + "/router/" + rid + "/link/" + "?attr=" + attr;

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getRouterInfo FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl GET http://localhost:8888/router/rid/link/lid
     *
     * Returns JSONObject:
     */
    public JSONObject getRouterLinkInfo(int routerID, int linkID) throws JSONException {
        try {
            String uri = vimURI + "/router/" + routerID + "/link/" + linkID;

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getRouterInfo FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: POST http://localhost:8888/router/2/app/?className=usr.applications.Recv&args=4000

     *
     * Returns JSONObject: {"aid":1,"id":262145,"name":"/Router-2/App/usr.applications.Recv/1","routerID":2}
     */
    public JSONObject createApp(int routerID, String className, String args) throws JSONException {
        try {
            String uri = vimURI + "/router/" + routerID + "/app/?className=" + className + "&args=" + java.net.URLEncoder.encode(args, "UTF-8");

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("createApp FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of:  DELETE http://localhost:8888/router/1/app/262145
     *
     * Returns JSONObject: {"status":"done"}
     */
    public JSONObject stopApp(int routerID, int appID) throws JSONException {
        try {
            String uri = vimURI + "/router/" + routerID + "/app/" + appID;

            // Delete
            JSONObject jsobj = rest.json(uri, delete()).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("deleteApp FAILED" + " IOException: " + ioe.getMessage());
        }
    }



    /**
     * Equivalent of: curl http://localhost:8888/router/2/app/
     *
     * Returns JSONObject: {"list":[786553,5505145,7864441],"type":"app"}
     */
    public JSONObject listApps(int routerID) throws JSONException {
        try {
            String uri = vimURI + "/router/" + routerID + "/app/";


            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("listApps FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl http://localhost:8888/router/2/app/
     *
     * Returns JSONObject: {"appID":2, "appName":"/Router-7/App/usr.applications.Send/1", "classname": "usr.applications.Send", "args": "[8, 4000, 2500000, -s, 1024]", "routerID": 7, "starttime": 1362090555686, "runtime": 42854}
     */
    public JSONObject getAppInfo(int routerID, int appID) throws JSONException {
        try {
            String uri = vimURI + "/router/" + routerID + "/app/" + appID;


            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getAppInfo FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl GET http://localhost:8888/ap/
     *
     * Returns JSONObject:  {"list":[12,6,5],"type":"ap"}
     */
    public JSONObject listAggPoints() throws JSONException {
        try {
            String uri = vimURI + "/ap/";

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("listAggPoints FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl GET http://localhost:8888/ap/id
     *
     * Returns JSONObject: {"ap": 10, "routerID": 5}
     */
    public JSONObject getAggPointInfo(int id) throws JSONException {
        try {
            String uri = vimURI + "/ap/" + id;

            JSONObject jsobj = rest.json(uri).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("getAggPointInfo FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    /**
     * Equivalent of: curl -X POST http://localhost:8888/ap/?apID=aaa&routerID=rrr
     *
     * Returns JSONObject: {"ap": 10, "routerID": 5}
     */
    public JSONObject setAggPoint(int apID, int routerID) throws JSONException {
        try {
            String uri = vimURI + "/ap/?apID=" + apID + "&routerID=" + routerID;

            // adding form data causes a POST
            JSONObject jsobj = rest.json(uri, form("")).toObject();

            return jsobj;

        } catch (IOException ioe) {
            throw new JSONException("createRouter FAILED" + " IOException: " + ioe.getMessage());
        }
    }

}
