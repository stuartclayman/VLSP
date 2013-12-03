/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package plugins_usr.tftp.com.globalros.tftp.server;

import usr.net.*;

/**
 * @author marco
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public interface EventListener extends java.util.EventListener
{
   public void onAfterDownload(Address addr, int port, String fileName, boolean ok);
   public void onAfterUpload(Address addr, int port, String fileName, boolean ok);
}
