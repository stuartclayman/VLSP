/*
 * Adapted for UCL VLSP
 */


package demo_usr.dolfin.siemens;

import java.io.IOException;

import java.util.concurrent.TimeoutException;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import usr.logging.Logger;
import usr.logging.USR;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.AMQP.BasicProperties;
//import com.siemens.ct.ro.propertiesloader.PropertiesLoader;

/**
 * The Connection class for the AMQP connection and channel opening.
 * <p>
 * It holds the main parameters for an AMQP connection to be created. It's constructor initializes the parameters
 * and creates the connection.
 * 
 * @author Tudor Codrea, tudor.codrea@s, codrea_tudor@yahoo.com
 * @since 		13-08-2014
 * @version 	1.0
 * 
 */

public class AmqpConnection {

    static final Logger log = Logger.getLogger("log");
	
    private static PropertiesLoader propertiesLoader;
	
    private String brokerIP;
    private int port;
    private String virtualHost;
    private String userName;
    private String password;
    private boolean connectionDurability;
	
    private Channel channel;
    private Connection connection;

    private BasicProperties messageProperties;
	
    public AmqpConnection() {
        initConnectionParameters();			
        createAmqpConnection();
    }
	
    /**
     * Initializes the fields of a new AmqpConnection with the ones loaded from the configuration file
     */
    private void initConnectionParameters(){

        if ( propertiesLoader == null ) propertiesLoader = PropertiesLoader.getInstanceFromFile();

        brokerIP = propertiesLoader.getBrokerIP();
        port = propertiesLoader.getPort();
        virtualHost = propertiesLoader.getVirtualHost();
        userName = propertiesLoader.getUserName();
        password = propertiesLoader.getPassword();
        connectionDurability = propertiesLoader.isConnectionDurability();
        messageProperties = propertiesLoader.getMessageProperties();
			
        log.logln(USR.STDOUT, "Initializing AMQP Connection [brokerIP=" + brokerIP
                  + ", port=" + port + ", virtualHost=" + virtualHost
                  + ", userName=" + userName + ", password=" + password
                  + ", connectionDurability=" + connectionDurability
                  + ", messageProperties=" + messageProperties + "]");		
    }
	
    /**
     * Uses the above parameters to create an AMQP connection and a channel.
     * After the channel and connection are made they are set to their specific fields of an AmqpConnection
     * object as they are required in further operations down the publishing process.
     */
    private void createAmqpConnection(){
        ConnectionFactory factory = new ConnectionFactory(); 					// configuring the connection with broker
        factory.setUsername(userName); 																			
        factory.setPassword(password); 											
        factory.setVirtualHost(virtualHost);
        factory.setHost(brokerIP);
        factory.setPort(port);
		
        Connection newConnection;
        try {
            // creating the connection
            newConnection = factory.newConnection();
            setConnection(newConnection);
            Channel newChannel = newConnection.createChannel();
            setChannel(newChannel);
        } catch (TimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
		
        } catch (IOException ioe) {
            log.logln(USR.ERROR, "IOexception: " + ioe);
        }
    }
		
	
    /**
     * Is used for clearing up the AMQP resources.
     */
    public void closeConnection( ) {
        log.logln(USR.STDOUT, "AmqpConnection Closing.");
		
        try {
            //log.logln(USR.STDOUT, "Closing the channel.");
            try {
                channel.close();
            } catch (TimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } // clearing up the resources

            //log.logln(USR.STDOUT, "Closing the connection.");
            connection.close();
			
        } catch (IOException ioe) {
            log.logln (USR.ERROR,"IOexception: " + ioe);
        } 	
        log.logln(USR.STDOUT, "AmqpConnection closed.");
    }
	
    /**
     * 
     * Setters and Getters for the members
     * 
     */
	
    /**
     * @return channel after is is instanced
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Sets the channel after its created
     * @param channel
     */
    private void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * @return connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Sets the connection after its created
     * @param connection
     */
    private void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * @return brokerIP from the properties object
     */
    public String getBrokerIP() {
        return brokerIP;
    }

    /**
     * @return port from the properties object
     */
    public int getPort() {
        return port;
    }

    /**
     * @return virtualHost from the properties object
     */
    public String getVirtualHost() {
        return virtualHost;
    }

    /**
     * @return userName from the properties object
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return password from the properties object
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return connectionDurability from the properties object
     */
    public boolean isConnectionDurability() {
        return connectionDurability;
    }


    /**
     * Get BasicProperties
     * @author sclayman
     */
    public BasicProperties getBasicProperties() {
        return messageProperties;
    }

}
