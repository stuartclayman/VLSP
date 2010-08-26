package usr.common;

import java.util.*;

public class ProbDistribution 
{
    private ArrayList <ProbElement> distParts_= null;
    
    public ProbDistribution() {
        distParts_= new ArrayList<ProbElement>();
    }
    
    public void addPart(String type, double weight,double parm1, double parm2, double parm3) 
        throws ProbException
    { 
        ProbElement el= null;
        try {
            el = new ProbElement(type, weight, parm1, parm2, parm3);
        } catch (ProbException e) {
            throw e;
        }
        distParts_.add(el);
    }
    
    public void checkParts() throws ProbException {
        double prevWeight= 0.0;
        double newWeight;
        for (int i= 0; i < distParts_.size(); i++) {
            newWeight= distParts_.get(i).getWeight();
            if (newWeight <= 0.0 || newWeight >= 1.0) {
                throw new ProbException ("Weight not in (0.0, 1.0)");
            }
            if (newWeight <= prevWeight) {
                throw new ProbException
                   ("Weight in distribution must be less than previous");
            }
            if (i == distParts_.size() -1 && newWeight != 1.0) {
                throw new ProbException
                    ("Final weight for distribution part must be 1.0");
            }
            prevWeight= newWeight;
        }
    }
    


    
}
