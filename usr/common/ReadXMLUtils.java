package usr.common;


import usr.logging.*;

import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 
import usr.engine.*;
import usr.common.*;

public class ReadXMLUtils {

        // Parse tags to get boolean
    public static boolean parseSingleBool(Node node, String tag, String parent, boolean optional) 
        throws SAXException, XMLNoTagException
    { 
        String str= parseSingleString(node,tag,parent,optional);
        if (str.equals("true"))
          return true;
        if (str.equals("false"))
          return false;
        throw new SAXException ("Tag "+tag+" parent "+parent+" is not boolean "+str);
    }
    
    // Parse tags to get int
    public static int parseSingleInt(Node node, String tag, String parent, boolean optional) 
        throws SAXException, XMLNoTagException
    { 
        String str= parseSingleString(node,tag,parent,optional);
        int i= Integer.parseInt(str);
        return i;
    }
    
    public static String [] parseArrayString(Node node, String tag, String parent) 
        throws SAXException
        {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new SAXException("Expecting node element with tag "+tag);
        }
        Element el = (Element)node;
        NodeList hNameList = el.getElementsByTagName(tag);
        if (hNameList == null) {
            return new String [0];
        }
        int nStrings= hNameList.getLength();
        
        String []strings= new String[nStrings];
        for (int i= 0; i < nStrings; i++) {
            Element hElement = (Element)hNameList.item(i);
            NodeList textFNList = hElement.getChildNodes();
            strings[i]= ((Node)textFNList.item(0)).getNodeValue().trim();
        }
        return strings;
    }
    
    public static double[] parseArrayDouble(Node node, String tag, String parent) 
        throws SAXException
    {
        String []strs= parseArrayString(node,tag,parent);
        double []dbls= new double[strs.length];
        for (int i= 0; i < strs.length; i++) {
            dbls[i]= Double.parseDouble(strs[i]);
        }
        return dbls;
    }
    
        
    // Parse tags to get double
    public static double parseSingleDouble(Node node, String tag, String parent, boolean optional) 
        throws SAXException, XMLNoTagException
    { 
        String str= parseSingleString(node,tag,parent,optional);
        double d= Double.parseDouble(str);
        return d;
    }
    
    // Parse tags to get text
    public static String parseSingleString(Node node, String tag, String parent, 
        boolean optional) 
        throws SAXException, XMLNoTagException
    {
    
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new SAXException("Expecting node element with tag "+tag);
        }
        Element el = (Element)node;
        NodeList hNameList = el.getElementsByTagName(tag);
        if (hNameList.getLength() !=1 && optional == false) {
            throw new SAXException(parent+" element requires exactly one tag "+
              tag);
        }
        if (hNameList.getLength() > 1 ) {
            throw new SAXException(parent + " element can only have one tag "+
              tag);
        }
        if (hNameList.getLength() == 0) {
            throw new XMLNoTagException();
        }
        Element hElement = (Element)hNameList.item(0);
        NodeList textFNList = hElement.getChildNodes();
        return ((Node)textFNList.item(0)).getNodeValue().trim();
    }
    
    /** Remove from the dom an element which is definitely present and
    singular */
    public static void removeNode(Node node, String tag, String parent)
        throws SAXException
    {
    
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new SAXException("Expecting node element with tag "+tag);
        }
        Element el = (Element)node;
        NodeList hNameList = el.getElementsByTagName(tag);
        if (hNameList.getLength() !=1) {
            throw new SAXException(parent+" element requires exactly one tag "+
              tag);
        }
        Node n = hNameList.item(0);
        node.removeChild(n);
    }
    
    /** Remove from the dom an element which may be present and multiple */
    public static void removeNodes(Node node, String tag, String parent)
        throws SAXException
    {
    
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new SAXException("Expecting node element with tag "+tag);
        }
        Element el = (Element)node;
        NodeList hNameList = el.getElementsByTagName(tag);
        
        while (hNameList.getLength() != 0) {
            Node n = hNameList.item(0);
            node.removeChild(n);
        }
    }
    
     static public ProbDistribution parseProbDist(NodeList n, String tagname) 
      throws SAXException, ProbException, XMLNoTagException{
        if (n.getLength() == 0) 
            return null;
        if (n.getLength() > 1) {
            throw new SAXException("Only one tag of type "+tagname+
              " allowed");
        }
        ProbDistribution d= new ProbDistribution();
        
        Element el= (Element)n.item(0);
        
        NodeList els= el.getElementsByTagName("ProbElement");
        if (els.getLength() == 0) {
            throw new SAXException("Must have at least one element of "+
              "probability distribution in "+tagname);
        }  
        for (int i= 0; i < els.getLength(); i++) {
           Node no= els.item(i);
           ProbElement e= parseProbElement(no,tagname);
           d.addPart(e);
        }
        try {
           d.checkParts();
        } catch (ProbException e) {
            Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
            System.exit(-1);
        }
        return d;
    }
      
   static public ProbElement parseProbElement(Node n, String tagname) 
      throws SAXException, ProbException, XMLNoTagException {
        
        String distName= ReadXMLUtils.parseSingleString(n,"Type",tagname,true);
        double weight= ReadXMLUtils.parseSingleDouble(n,"Weight",tagname,true);
        double []parms= ReadXMLUtils.parseArrayDouble(n,"Parameter",tagname);
        ProbElement e= new ProbElement(distName,weight,parms); 
       
        return e;
    }

}
