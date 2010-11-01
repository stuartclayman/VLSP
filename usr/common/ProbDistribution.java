package usr.common;

import java.util.*;
import usr.logging.*;

public class ProbDistribution 
{
    private ArrayList <ProbElement> distParts_= null;
    
    public ProbDistribution() {
        distParts_= new ArrayList<ProbElement>();
    }
    
    public void addPart(String type, double weight,double []parms) 
        throws ProbException
    { 
        ProbElement el= null;
        try {
            el = new ProbElement(type, weight, parms);
        } catch (ProbException e) {
            throw e;
        }
        distParts_.add(el);
    }
    
    public void addPart(ProbElement e)
    { 
        distParts_.add(e);
    }
    
    public int getIntVariate() {
        return (int)Math.round(getVariate()); 
    }
    
    public double getVariate() {
        double r= Math.random();
        ProbElement finalel= null;
        try {
          for (ProbElement e: distParts_) {
              if (r < e.getWeight()) {
                  return e.getVariate();
              }
              finalel=e;
          }
          return finalel.getVariate();
        } catch (ProbException e) {
          Logger.getLogger("log").logln(USR.ERROR, "Error in getVariate");
          Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
          return 0.0;
        }
    }
    
    /**Return the *cumulative* distribution function value at x*/
    public double getCumulativeDistribution(double x)
    {
        double distribution= 0.0;
        double range= 0.0;
        try {
            for (ProbElement e: distParts_) {
                distribution+= (e.getWeight() - range)*e.getCumulativeDistribution(x);
                range= e.getWeight();
            }
        } catch (ProbException e) {
            Logger.getLogger("log").logln(USR.ERROR, "Error in getVariate");
          Logger.getLogger("log").logln(USR.ERROR, e.getMessage());
          return 0.0;
        }
        return distribution;
    
    }
    
    public void checkParts() throws ProbException {
        double prevWeight= 0.0;
        double newWeight;
        for (int i= 0; i < distParts_.size(); i++) {
            newWeight= distParts_.get(i).getWeight();
            if (newWeight <= 0.0 || newWeight > 1.0) {
                throw new ProbException ("Weight not in (0.0, 1.0)");
            }
            if (newWeight <= prevWeight) {
                throw new ProbException
                   ("Weight in distribution must be larger than previous");
            }
            if (i == distParts_.size() -1 && newWeight != 1.0) {
                throw new ProbException
                    ("Final weight for distribution part must be 1.0");
            }
            prevWeight= newWeight;
        }
    }
    


    
}
