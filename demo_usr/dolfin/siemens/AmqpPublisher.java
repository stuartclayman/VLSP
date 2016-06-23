/*
 * Adapted for UCL VLSP
 */


package demo_usr.dolfin.siemens;

import java.io.IOException;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import usr.logging.Logger;
import usr.logging.USR;

import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * The publisher client for AMQP Broker. Made with amqp client. 
 * 
 * This class holds the publish method for the AMQP client.
 * 
 * Also, the Container gets the connection parameters trough AmqpConnection connection field.
 * 
 * @author Tudor Codrea, tudor.codrea@siemens.com
 * @since 		13-08-2014
 * @version 	1.0
 * 
 */

public class AmqpPublisher {

    private static Logger logger = Logger.getLogger("log"); // WAS LoggerFactory.getLogger(AmqpPublisher.class);	
	private AmqpConnection connection;

    /**
     * Constructor
     */
    public AmqpPublisher(AmqpConnection conn) {
        connection = conn;
    }
	
	/**
	 * This method publishes the message to the AMQP broker
	 * @param exchangeName			- same as topic name
	 * @param routingKey			
	 * @param messageProperties		- type of message and its persistence 
	 * @param message
	 * @param connectionDurability	- type of session (true if durable false if not)
	 * @throws IOException
	 */
	public void publish(String exchangeName, String routingKey,
			BasicProperties messageProperties, String message,
			boolean connectionDurability) throws IOException {
		
		Channel channel = connection.getChannel();
		
		channel.exchangeDeclare(exchangeName, "direct", connectionDurability);	
		
		byte[] messageBodyBytes = message.getBytes();	
		channel.basicPublish(exchangeName, routingKey,true, messageProperties, messageBodyBytes);
		logger.logln(USR.STDOUT, "Message published ... DONE");
		
	}
	
	/**
	 * @return a connection from AmqpConnection object
	 */
	public AmqpConnection getConnection() {
		return connection;
	}

	/**
	 * @param connection to fill in the AmqpConnection field
	 */
	public void setConnection(AmqpConnection connection) {
		this.connection = connection;
	}


    /**
     * Publish a message over an AmqpConnection
     * @author sclayman
     */
    public void publish(String exchangeName, String routingKey, String message) throws IOException {
        BasicProperties messageProperties = connection.getBasicProperties();
        
	Channel channel = connection.getChannel();
		
        channel.exchangeDeclare(exchangeName, "direct", connection.isConnectionDurability());	
		
        byte[] messageBodyBytes = message.getBytes();	
        channel.basicPublish(exchangeName, routingKey,true, messageProperties, messageBodyBytes);
        logger.logln(USR.STDOUT, "Publishing on: topic: " + exchangeName
                                                  + " /with RoutingKey: " + routingKey
                                                  + " /on channel: "
                                                  + channel.toString());

    }        
}
