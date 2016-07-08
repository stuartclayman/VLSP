/*
 * Adapted for UCL VLSP
 */


package demo_usr.dolfin.siemens;

import java.io.IOException;
import java.util.UUID;
 
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import usr.logging.Logger;
import usr.logging.USR;
import usr.logging.BitMask;
 
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
 
public class AmqpSubscriber {
 
    private static Logger logger = Logger.getLogger("log"); // WAS LoggerFactory.getLogger(AmqpPublisher.class);	
    private AmqpConnection connection;

    private Consumer consumer;

    private boolean autoAck = false;


    /**
     * Constructor
     */
    public AmqpSubscriber(AmqpConnection conn) {
        connection = conn;
    }
	

    public String setupQueue(String exchangeName, String routingKey) throws IOException {
        Channel channel = connection.getChannel();

        boolean durable = false;

        String queueName = "queue:"+ exchangeName;

        channel.exchangeDeclare(exchangeName, "direct", durable);

        channel.queueDeclare(queueName, durable, false, false, null);

        Logger.getLogger("log").logln(USR.STDOUT, "Queue Bind: " + queueName + " " + exchangeName + " " + routingKey);

        channel.queueBind(queueName, exchangeName, routingKey); // binding
        
        return queueName;
    }

    public void setConsumer(Consumer c) {
        consumer = c;
    }
    
    /**
     * Consume a message
     * Pass in a DefaultConsumer object
     */
    public void consume(String queueName) throws IOException {
 
        String consumerTag = UUID.randomUUID().toString();
        
        Channel channel = connection.getChannel();

        channel.basicConsume(queueName, autoAck, consumerTag, consumer);
    }


    
    public static void main(String[] args) {
 
        logger.addOutput(System.out, new BitMask(USR.STDOUT));

        // properties holds connection details
        String propertiesLocation = "amqpConfig.properties";
        // properties file location
        PropertiesLoader.setPropertiesLocation(propertiesLocation);
        PropertiesLoader propertiesLoader = PropertiesLoader.getInstanceFromFile();



        // AMQP Connection

        Logger.getLogger("log").logln(USR.STDOUT, "Initializing the AMQP connection ... ");						
        Logger.getLogger("log").logln(USR.STDOUT, "Creating AMQP connection ... DONE ");

        AmqpConnection amqpConnection = new AmqpConnection();   // creating the AMQP connection

        Logger.getLogger("log").logln(USR.STDOUT, "Creating Amqp client ... ");

        AmqpSubscriber amqpSubscriber = new AmqpSubscriber(amqpConnection);       // creating subscriber
        amqpSubscriber.setConsumer(new PrintingConsumer(amqpConnection.getChannel()));


        Logger.getLogger("log").logln(USR.STDOUT, "Creating Amqp client ... DONE ");

 
        try {
            // setup queue
            String queueName = amqpSubscriber.setupQueue("vim", "key.manager");
            amqpSubscriber.consume(queueName);
            queueName = amqpSubscriber.setupQueue("vim", "key.vm");
            amqpSubscriber.consume(queueName);
            queueName = amqpSubscriber.setupQueue("vim", "key.host");
            amqpSubscriber.consume(queueName);
            queueName = amqpSubscriber.setupQueue("vim", "key.host.green");
            amqpSubscriber.consume(queueName);

            /*
              channel.queueBind(queueName, exchangeName, "key.manager");
              channel.queueBind(queueName, exchangeName, "key.vm");
              channel.queueBind(queueName, exchangeName, "key.host");
              channel.queueBind(queueName, exchangeName, "key.host.green");
            */


            Logger.getLogger("log").logln(USR.STDOUT, "queueName = " + queueName);
            
 
            String queueName2 = amqpSubscriber.setupQueue("aggregationResult", "aggregationResult");
            amqpSubscriber.consume(queueName2);

            Logger.getLogger("log").logln(USR.STDOUT, "queueName = " + queueName2);
            
 
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
 
    }
 
}
 
class PrintingConsumer extends DefaultConsumer {
    public PrintingConsumer(Channel channel) {
        super(channel);
    }
        
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body) throws IOException {
 
        String routingKey = envelope.getRoutingKey();
        String topic = envelope.getExchange();
        String contentType = properties.getContentType();
        long deliveryTag = envelope.getDeliveryTag();
 
        System.out.println("Message received: "
                           +  "topic: " + topic 
                           + " routingKey: " + routingKey
                           //+ "content type: " + contentType
                           + "\t" + new String(body) );
        getChannel().basicAck(deliveryTag, false);
 
    }
 
}
