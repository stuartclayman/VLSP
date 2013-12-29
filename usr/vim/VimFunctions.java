package usr.vim;

import us.monoid.json.JSONObject;

public interface VimFunctions {

     public JSONObject createRouter();
     public JSONObject createRouter(String name, String address);
     public JSONObject createRouterWithName(String name);
     public JSONObject createRouterWithAddress(String address);
     public JSONObject deleteRouter(int routerID);
     public JSONObject listRouters();
     public JSONObject listRouters(String detail);
     public JSONObject listRemovedRouters();
     public JSONObject getRouterInfo(int id);
     public JSONObject getRouterLinkStats(int id);
     public JSONObject getRouterLinkStats(int id, int dstID);
     public JSONObject getRouterCount();
     public JSONObject getMaxRouterID();
     public JSONObject createLink(int routerID1, int routerID2);
     public JSONObject createLink(int routerID1, int routerID2, int weight);
     public JSONObject createLink(int routerID1, int routerID2, int weight, String linkName);
     public JSONObject deleteLink(int linkID);
     public JSONObject listLinks();
     public JSONObject listLinks(String detail);
     public JSONObject getLinkInfo(int id);
     public JSONObject setLinkWeight(int linkID, int weight);
     public JSONObject getLinkCount();
     public JSONObject listRouterLinks(int routerID);
     public JSONObject listRouterLinks(int rid, String attr);
     public JSONObject getRouterLinkInfo(int routerID, int linkID);
     public JSONObject createApp(int routerID, String className, String args);
     public JSONObject stopApp(int routerID, int appID);
     public JSONObject listApps(int routerID);
     public JSONObject getAppInfo(int routerID, String appID);

}
