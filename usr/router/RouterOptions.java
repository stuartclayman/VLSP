/** This class contains the options used for a router
 */
package usr.router;

import java.io.*;
import usr.engine.*;
import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException; 
import usr.common.*;


public class RouterOptions {
   
    Router router_;
    
    /** Constructor for router Options */
    
    public RouterOptions (Router router) {
        router_= router;
        init();
    }   
   
    /** init function sets up defaults and basic information */
    void init () {
        
    }
    
    public void setOptionsFromFile(String fName) throws java.io.FileNotFoundException,
        SAXParseException, SAXException, javax.xml.parsers.ParserConfigurationException,
        java.io.IOException
    {

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse (new File(fName));

            // normalize text representation
            doc.getDocumentElement ().normalize ();
            
            parseXML(doc);
            //System.err.println("Read options from string");
    }
    
    public void setOptionsFromString(String XMLString) throws java.io.FileNotFoundException,
        SAXParseException, SAXException, javax.xml.parsers.ParserConfigurationException,
        java.io.IOException
    {
           //System.err.println("Options string "+XMLString);
           
          DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            StringReader sr= new StringReader(XMLString);
            InputSource is= new InputSource(sr);
            Document doc = docBuilder.parse (is);

            // normalize text representation
            doc.getDocumentElement ().normalize ();
            
            parseXML(doc);

    }
    
    public void parseXML(Document doc) throws java.io.FileNotFoundException,
        SAXParseException, SAXException
    {
        String basenode= doc.getDocumentElement().getNodeName();
        if (!basenode.equals("RouterOptions")) {
            throw new SAXException("Base tag should be RouterOptions");
        }
        //NodeList n= doc.getElementsByTagName("Manager");
        
    }
    
       
    
    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String RO = "RO: ";
        RouterController controller = router_.getRouterController();

        return controller.getName() + " " + RO;
    }
}


