package demo_usr.ikms.client.utils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

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
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int port = k.getPort();
		return port;
	}
	
	public static int ExtractNodeAddressFromURI (String uri) {
		URL k=null;
		try {
			k = new URL(uri);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int address = k.getPort() - 10000;
		return address;
	}
}
