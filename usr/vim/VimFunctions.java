package usr.vim;

import us.monoid.json.JSONObject;
import us.monoid.json.JSONException;

public interface VimFunctions {

    public JSONObject createRouter() throws JSONException;
    public JSONObject createRouter(String parameters) throws JSONException;
    public JSONObject createRouter(String name, String address) throws JSONException;
    public JSONObject createRouter(String name, String address, String parameters) throws JSONException;
    public JSONObject createRouterWithName(String name) throws JSONException;
    public JSONObject createRouterWithAddress(String address) throws JSONException;
    public JSONObject deleteRouter(int routerID) throws JSONException;
    public JSONObject listRouters() throws JSONException;
    public JSONObject listRouters(String arg) throws JSONException;
    public JSONObject listRemovedRouters() throws JSONException;
    public JSONObject getRouterInfo(int id) throws JSONException;
    public JSONObject getRouterLinkStats(int id) throws JSONException;
    public JSONObject getRouterLinkStats(int id, int dstID) throws JSONException;
    public JSONObject getRouterCount() throws JSONException;
    public JSONObject getMaxRouterID() throws JSONException;
    public JSONObject createLink(int routerID1, int routerID2) throws JSONException;
    public JSONObject createLink(int routerID1, int routerID2, int weight) throws JSONException;
    public JSONObject createLink(int routerID1, int routerID2, int weight, String linkName) throws JSONException;
    public JSONObject deleteLink(int linkID) throws JSONException;
    public JSONObject listLinks() throws JSONException;
    public JSONObject listLinks(String detail) throws JSONException;
    public JSONObject getLinkInfo(int id) throws JSONException;
    public JSONObject setLinkWeight(int linkID, int weight) throws JSONException;
    public JSONObject getLinkCount() throws JSONException;
    public JSONObject listRouterLinks(int routerID) throws JSONException;
    public JSONObject listRouterLinks(int rid, String attr) throws JSONException;
    public JSONObject getRouterLinkInfo(int routerID, int linkID) throws JSONException;
    public JSONObject createApp(int routerID, String className, String args) throws JSONException;
    public JSONObject stopApp(int routerID, int appID) throws JSONException;
    public JSONObject listApps(int routerID) throws JSONException;
    public JSONObject getAppInfo(int routerID, int appID) throws JSONException;
    public JSONObject listAggPoints() throws JSONException;
    public JSONObject getAggPointInfo(int id) throws JSONException;
    public JSONObject setAggPoint(int apID, int routerID) throws JSONException;
}
