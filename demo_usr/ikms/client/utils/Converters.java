package demo_usr.ikms.client.utils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class Converters {

    public static String URIToFileName (String uri) {
        try {
            return URLEncoder.encode(uri,  "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
	
    public static String FileNameToURI (String urifile) {
        try {
            return URLDecoder.decode(urifile.replace("/tmp/http","http"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public static int ExtractPortFromTargetURIFileName (String fileName) {
        URL k=null;
        try {
            k = new URL(FileNameToURI (fileName));
            int port = k.getPort();
            return port;
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e.getMessage());
        }
    }
	
    public static Map<String, String> SplitQuery(String urlString) {
		
        URL url=null;
        try {
            url = new URL (urlString);

            Map<String, String> query_pairs = new LinkedHashMap<String, String>();
            String query = url.getQuery();
            System.out.println ("query:"+query);
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                try {
                    query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return query_pairs;
            
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e.getMessage());
        }
		
    }
	
    /*public static int ExtractNodeAddressFromURI (String uri) {
      URL k=null;
      try {
      k = new URL(uri);
      } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      }
      int address = k.getPort() - 10000;
      return address;
      }*/
}
