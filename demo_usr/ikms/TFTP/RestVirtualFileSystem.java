/**
 * (c) Melexis Telecom and or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package demo_usr.ikms.TFTP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import plugins_usr.tftp.com.globalros.tftp.common.VirtualFile;
import plugins_usr.tftp.com.globalros.tftp.common.VirtualFileSystem;
import us.monoid.web.Resty;
import usr.logging.Logger;
import demo_usr.ikms.client.utils.Converters;

//import org.apache.log4j.Logger;


/**
 * @author marco
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class RestVirtualFileSystem implements VirtualFileSystem
{
	Resty rest;
	
	/**
	 * logger
	 */
	//private Logger log = Logger.getLogger(FileSystem.class.getName());
	private static Logger log = Logger.getLogger("log");

	/**
	 * TFTP home dir
	 */
	private File home;

	/**
	 * Constructor for FileSystem.
	 */
	public RestVirtualFileSystem()
	{
		// Make a Resty connection
		rest = new Resty();
		
	}

	/**
	 * 
	 * @throws IOException 
	 * @see com.globalros.tftp.common.VirtualFileSystem#getInputStream(VirtualFile)
	 * 
	 */
	public InputStream getInputStream (VirtualFile urifile) throws FileNotFoundException
	{
		String uri = Converters.FileNameToURI (urifile.getFileName());
		try {
			InputStream output = rest.json(uri).getUrlConnection().getInputStream();
			return output;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @see com.globalros.tftp.common.VirtualFileSystem#getOutputStream(VirtualFile)
	 */
	public OutputStream getOutputStream(VirtualFile urifile) throws FileNotFoundException
	{		
		String uri = Converters.FileNameToURI (urifile.getFileName());

		URLConnection urlconnection= null;
		try {
			//urlconnection = rest.json(uri).getUrlConnection();
			//HttpURLConnection httpconnection = (HttpURLConnection) urlconnection;
			//httpconnection.setRequestMethod("PUT");
			//httpconnection.setDoOutput(true);

			URL url = new URL(uri);
			HttpURLConnection httpconnection = (HttpURLConnection) url.openConnection();
			httpconnection.setDoOutput(true);
			httpconnection.setRequestMethod("POST");
			httpconnection.setRequestProperty("Content-Type", "application/json");
			httpconnection.setRequestProperty("Accept", "application/json");
			httpconnection.setChunkedStreamingMode(0);
			OutputStream output = httpconnection.getOutputStream();

			return output;
			//urlconnection.setDoOutput(true);
			//urlconnection.setRequestProperty("Accept-Charset", "UTF-8");
			//urlconnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + "UTF-8");
//			((HttpURLConnection)urlconnection).
//			return urlconnection.getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		//return new FileOutputStream( expand(file.getFileName()));
	}
}
