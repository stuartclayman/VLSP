package usr.test;

import rgc.xmlparse.*;
import rgc.probdistributions.*;
import usr.logging.*;
import java.util.*;

import usr.APcontroller.*;

import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import java.io.*;


public class LifeDistributionTest {
    public static void main(String[] args) {
        int i;
        int noTests;
        ProbDistribution dist = null;

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
              String basenode = doc.getDocumentElement().getNodeName();

              if (!basenode.equals("LifeEstimateTest")) {
                  throw new SAXException("Base tag should be LifeEstimateTest");
              }
              NodeList td = doc.getElementsByTagName("TestDist");
              dist = ProbDistribution.parseProbDist(td, "TestDist");

              if (dist == null) {
                  throw new SAXException ("Must specify TestDist");
              }
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Cannot find file "+args[0]);
            System.exit(-1);
        } catch (SAXParseException err) {
            System.err.println ("** Parsing error" + ", line "
                                + err.getLineNumber () + ", uri " + err.getSystemId ());
            System.err.println(" " + err.getMessage ());
            System.exit(-1);

        } catch (SAXException e) {
            System.err.println("Exception in SAX XML parser.");
            System.err.println(e.getMessage());
            System.exit(-1);

        } catch (Throwable t) {
            t.printStackTrace ();
            System.exit(-1);
        }

        LifeSpanEstimate e = new LifeSpanEstimate();
        //double y= e.erf(0.9);
        //System.out.println("Erf 0.5 ="+ y);
        //System.out.println("Inverse ="+ e.inverf(y));
        //y= e.erfc(0.9);
        //System.out.println("Erfc 0.5 ="+ y);
        //System.out.println("Inverse ="+ e.inverfc(y));
        noTests = Integer.parseInt(args[1]);
        int [] lifeSpans = new int[noTests];
        double tot = 0;
        int maxL = 0;

        for (i = 0; i < noTests; i++) {
            try {
                lifeSpans[i] = Math.max(1, (int)(dist.getVariate()*1000));
            } catch (Exception x) {
                System.err.println("getVariate threw error");
            }

            //   System.err.println("Life is "+lifeSpans[i]);
            maxL = Math.max(maxL, lifeSpans[i]);

        }
        //System.out.println(tot/noTests);
        int endTime = (int)((1.0+Math.random())*maxL); // Run a random proportion of
        // max life -- with births all through period
        // System.err.println("MaxL="+maxL+" end time "+endTime);
        int lifeStep = endTime/noTests;
        int time = 0;
        int alive = 0;
        int outlast = 0;
        int deaths = 0;

        for (i = 0; i < noTests; i++) {
            e.newNode(time, i);

            if (lifeSpans[i] > endTime) {
                outlast += 1;
            }

            if (time + lifeSpans[i] < endTime) {
                e.nodeDeath(time + lifeSpans[i], i);
                //  System.err.println("Death at time "+time+" life span "+lifeSpans[i]+
                //    " end Time "+endTime);
                deaths++;
            } else {
                //   System.err.println("Node born at time "+time+" life span "+lifeSpans[i]+
                //   " outlasts end Time "+endTime);
                alive += 1;
            }
            time += lifeStep;
        }
        int noPoints = 100;
        e.sortDeaths();
        e.updateKMEstimate(endTime);
        e.fitTail();
        double startx = 1000.0;
        double xmult = Math.pow(maxL*10.0/startx, 1.0/noPoints);
        double dx = startx;

        for (i = 1; i < noPoints; i++) {

            int x = (int)dx;
            try {
                System.out.println(x/1000.0+" "+e.getKMProb(x)+" "+
                                   e.getKMTailProb(x)+" "+(1.0 - dist.getCumulativeDistribution((double)x/1000)));
            } catch (Throwable t) {
                System.err.println("Prob dist threw error");
            }

            dx *= xmult;
        }



    }

}