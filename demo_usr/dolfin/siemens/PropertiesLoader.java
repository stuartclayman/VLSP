/*
 * Adapted for UCL VLSP
 */


package demo_usr.dolfin.siemens;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import usr.logging.Logger;
import usr.logging.USR;

import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * The part where the custom properties are loaded. Change the myProp.properties
 * path with your path if needed.
 * 
 * @author Tudor Codrea, tudor.codrea@ctbav.ro, codrea_tudor@yahoo.com
 * @since 13-08-2014
 * @version 3.0
 * 
 *          Added custom location part to the properties loader.
 *          <p>
 *          Static properties loading feature.
 * 
 */

public class PropertiesLoader {

	private String brokerIP;
	private int port;
	private String virtualHost;
	private String userName;
	private String password;
	private boolean connectionDurability;

	private BasicProperties messageProperties;
	private String routingKey;
	private String exchangeName;
	private String jobFrequency;

	// getting the message methods
	private String fileMessagePath;
	private String httpRequestUrl;

	private static String propertiesLocation;

	private static Logger logger = Logger.getLogger("log"); // LoggerFactory.getLogger(PropertiesLoader.class);

	private static PropertiesLoader instance = null;

	/**
	 * 
	 * @param loadFromFile
	 *            - set to true if properties are located inside a .ini file
	 */
	private PropertiesLoader(boolean loadFromFile) {

		if (loadFromFile) {

			logger.logln(USR.STDOUT, "Loading the properties from properties file ... ");
			Properties properties = new Properties();

			InputStream inputStream = null;

			try {

				inputStream = new FileInputStream(propertiesLocation);
				properties.load(inputStream);

				// properties for the connection
				brokerIP = properties.getProperty("brokerIP");
				port = Integer.parseInt(properties.getProperty("port"));
				virtualHost = properties.getProperty("virtualHost");
				userName = properties.getProperty("userName");
				password = properties.getProperty("password");
				connectionDurability = Boolean.parseBoolean(properties.getProperty("connectionDurability"));

				// properties for the adapter
				messageProperties = MessageProperties.TEXT_PLAIN;
				routingKey = properties.getProperty("routingKey");
				exchangeName = properties.getProperty("exchangeName");
				jobFrequency = properties.getProperty("jobFrequency");
				fileMessagePath = properties.getProperty("fileMessagePath");
				httpRequestUrl = properties.getProperty("httpRequestUrl");

				logger.logln(USR.STDOUT, "Properties loaded !!!");

			} catch (InvalidPropertiesFormatException e) {
				logger.logln(USR.STDOUT, 
						"Error occured while loading the properties file: InvalidPropertiesFormatException " +
						e);
			} catch (FileNotFoundException fnfe) {
				logger.logln(USR.STDOUT, 
						"Error occured while loading the properties file: InvalidPropertiesFormatException " +
						fnfe);
			} catch (IOException ioe) {
				logger.logln(USR.STDOUT, 
						"Error occured while loading the properties file: InvalidPropertiesFormatException " +
						ioe);
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException ioe) {
						logger.logln(USR.STDOUT, 
								"Error occured while closing the properties file: InvalidPropertiesFormatException " +
								ioe);
					}
				}
			}
		}
	}

	/**
	 * @return a PropertiesLoader object which has all of the fields filled in
	 *         with the desired properties
	 */
	public static PropertiesLoader getInstanceFromFile() {

		if (instance == null) {
			instance = new PropertiesLoader(true);
		}

		return instance;
	}

	/**
	 * @return an empty PropertiesLoader object which must be filled with
	 *         setters
	 */
	public static PropertiesLoader getEmptyInstance() {
		if (instance == null) {
			instance = new PropertiesLoader(false);
		}
		return instance;
	}


    /**
     * Get the instance
     */
    public static PropertiesLoader getInstance() {
        return instance;
    }
    
	/**
	 * Getters to access the properties and Setters to fill them in when using
	 * empty objects of PropertiesLoader
	 */

	/**
	 * @param propertiesLocation - the .properties files location string
	 */
	public static void setPropertiesLocation(String propertiesLocation) {
		PropertiesLoader.propertiesLocation = propertiesLocation;
	}

	/**
	 * @return brokerIP
	 */
	public String getBrokerIP() {
		return brokerIP;
	}

	/**
	 * @param brokerIP
	 *            with the IP of the AMQP broker
	 */
	public void setBrokerIP(String brokerIP) {
		this.brokerIP = brokerIP;
	}

	/**
	 * @param port
	 *            number of the AMQP service
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @param virtualHost
	 *            default "/", name of the host
	 */
	public void setVirtualHost(String virtualHost) {
		this.virtualHost = virtualHost;
	}

	/**
	 * @param userName
	 *            , for anonymous connection use guest
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @param password
	 *            , for anonymous connection use guest
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @param connectionDurability
	 *            is the type of session
	 */
	public void setConnectionDurability(boolean connectionDurability) {
		this.connectionDurability = connectionDurability;
	}

	/**
	 * @param messageProperties
	 *            are the content and persistence of the message
	 */
	public void setMessageProperties(BasicProperties messageProperties) {
		this.messageProperties = messageProperties;
	}

	/**
	 * @param routingKey
	 */
	public void setRoutingKey(String routingKey) {
		this.routingKey = routingKey;
	}

	/**
	 * @param exchangeName
	 *            as known as the topic name
	 */
	public void setExchangeName(String exchangeName) {
		this.exchangeName = exchangeName;
	}

	/**
	 * @param jobFrequency
	 *            is the repeating time of quartz publishing job
	 */
	public void setJobFrequency(String jobFrequency) {
		this.jobFrequency = jobFrequency;
	}

	/**
	 * @param fileMessagePath
	 *            when message is located on HDD
	 */
	public void setFileMessagePath(String fileMessagePath) {
		this.fileMessagePath = fileMessagePath;
	}

	/**
	 * @param httpRequestUrl
	 *            the URL for the request
	 */
	public void setHttpRequestUrl(String httpRequestUrl) {
		this.httpRequestUrl = httpRequestUrl;
	}

	/**
	 * @return port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return virtualHost
	 */
	public String getVirtualHost() {
		return virtualHost;
	}

	/**
	 * @return userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @return connectionDurability
	 */
	public boolean isConnectionDurability() {
		return connectionDurability;
	}

	/**
	 * @return messageProperties
	 */
	public BasicProperties getMessageProperties() {
		return messageProperties;
	}

	/**
	 * @return routingKey
	 */
	public String getRoutingKey() {
		return routingKey;
	}

	/**
	 * @return exchangeName
	 */
	public String getExchangeName() {
		return exchangeName;
	}

	/**
	 * @return jobFrequency
	 */
	public String getJobFrequency() {
		return jobFrequency;
	}

	/**
	 * @return fileMessagePath
	 */
	public String getFileMessagePath() {
		return fileMessagePath;
	}

	/**
	 * @return httpRequestUrl
	 */
	public String getHttpRequestUrl() {
		return httpRequestUrl;
	}
}
