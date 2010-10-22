package usr.test;

import usr.common.*;
import usr.logging.*;

import usr.APcontroller.*;

import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 
import java.io.*;


public class LifeEstimateTest {
    public static void main(String[] args) {
        int i;
        int noTests;
        ProbDistribution dist= null;
        if (args.length != 2) {
            System.out.println("Need arguments --  distribution file and no tests");
            System.exit(-1);
        }
              Logger logger = Logger.getLogger("log");
      // tell it to output to stdout
      // and tell it what to pick up
      // it will actually output things where the log has bit 
      // USR.STDOUT set
      
      // tell it to output to stderr
      // and tell it what to pick up
      // it will actually output things where the log has bit
      // USR.ERROR set
      logger.addOutput(System.err, new BitMask(USR.ERROR));
      logger.addOutput(System.out, new BitMask(USR.STDOUT));
        
      try { DocumentBuilderFactory docBuilderFactory = 
          DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse (new File(args[0]));

        // normalize text representation
        doc.getDocumentElement ().normalize ();
        String basenode= doc.getDocumentElement().getNodeName();
        if (!basenode.equals("LifeEstimateTest")) {
            throw new SAXException("Base tag should be LifeEstimateTest");
        }
        NodeList td= doc.getElementsByTagName("TestDist");
        dist= ReadXMLUtils.parseProbDist(td,"TestDist");
        if (dist == null) {
            throw new SAXException ("Must specify TestDist");
        }
          
    } catch (java.io.FileNotFoundException e) {
          System.err.println("Cannot find file "+args[0]);
          System.exit(-1);
      }catch (SAXParseException err) {
          System.err.println ("** Parsing error" + ", line " 
             + err.getLineNumber () + ", uri " + err.getSystemId ());
          System.err.println(" " + err.getMessage ());
          System.exit(-1);

      }catch (SAXException e) {
          System.err.println("Exception in SAX XML parser.");
          System.err.println(e.getMessage());
          System.exit(-1);
          
      }catch (Throwable t) {
          t.printStackTrace ();
          System.exit(-1);
      }
        
        LifeSpanEstimate e= new LifeSpanEstimate();
        //double y= e.erf(0.9);
        //System.out.println("Erf 0.5 ="+ y);
        //System.out.println("Inverse ="+ e.inverf(y));
        //y= e.erfc(0.9);
        //System.out.println("Erfc 0.5 ="+ y);
        //System.out.println("Inverse ="+ e.inverfc(y));
        noTests= Integer.parseInt(args[1]);
        int []lifeSpans= new int[noTests];
        int max= 0;
        for (i= 0; i < noTests; i++) {
            lifeSpans[i]= (int)dist.getIntVariate();
            //System.out.println("Life is "+lifeSpans[i]);
            if (lifeSpans[i] > max) 
                max= lifeSpans[i];
        }
        int endTime= (int)(max/2);
        int lifeStep= endTime/noTests;
        int time= 0;
        for (i= 0; i < noTests; i++) {
            e.newNode(time,i);
            if (time + lifeSpans[i] < endTime) {
                e.nodeDeath(time + lifeSpans[i],i);
               // System.out.println("Death at time "+time+" life span "+lifeSpans[i]+ 
                //  " end Time "+endTime);
            }
            time+= lifeStep;
        }
        e.plotKMGraph(time);
        
    }

}
