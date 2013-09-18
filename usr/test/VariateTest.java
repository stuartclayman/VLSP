package usr.test;


import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import rgc.probdistributions.ProbDistribution;

public class VariateTest {
    public static void main(String[] args) {
        int i;
        int noTests;
        //System.out.println(MathFunctions.incompleteGamma(1.0,0.5));
        ProbDistribution dist = null;

        if (args.length != 2) {
            System.out.println("Need arguments --  distribution file and no tests");
            return;
        }

        try { DocumentBuilderFactory docBuilderFactory =
                  DocumentBuilderFactory.newInstance();
              DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
              Document doc = docBuilder.parse (new File(args[0]));

              // normalize text representation
              doc.getDocumentElement ().normalize ();
              String basenode = doc.getDocumentElement().getNodeName();

              if (!basenode.equals("VariateTest")) {
                  throw new SAXException("Base tag should be VariateTest");
              }
              NodeList td = doc.getElementsByTagName("TestDist");
              dist = ProbDistribution.parseProbDist(td, "TestDist");

              if (dist == null) {
                  throw new SAXException ("Must specific TestDist");
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

        noTests = Integer.parseInt(args[1]);

        for (i = 0; i < noTests; i++) {
            try {
                System.out.println(dist.getVariate());
            } catch (Exception exc) {
                System.out.println("ERror in getVariate");
            }
        }
    }

}