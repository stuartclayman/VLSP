package usr.APcontroller;

import java.util.*;
import usr.logging.*;
import usr.router.Router;
import usr.globalcontroller.GlobalController;
import usr.router.RouterOptions;

/** Produces estimates of life spans given information about node births 
and deaths*/

public class LifeSpanEstimate {
    ArrayList <Integer> deaths_;    // lifespan of nodes which are dead
    HashMap <Integer,Long> births_;    // birth time of nodes which are alive
    RouterOptions options_;     // Options for simulations
    ArrayList <Integer> KMTime_= null;
    ArrayList <Double> KMProb_= null;
    
    static final int MIN_DEATHS= 5;
    static final int MIN_LIVE= 5;
    
    LifeSpanEstimate(RouterOptions o) 
    {
        options_= o;
        deaths_= new ArrayList <Integer>();
        births_= new HashMap <Integer,Long>();
    }    

    /** A node is born at a given time -- register this */
    public void newNode(long time, int gid) 
    {
        births_.put(gid,time);
    }
    
    /** A node dies -- register this */
    public void nodeDeath(long time, int gid)
    {
        int lifeTime= (int)(time - births_.get(gid) );
        births_.remove(gid);
        for (int i= 0; i < births_.size();i++) { // Keep list of deaths ordered
            if (deaths_.get(i) >= lifeTime) {
                deaths_.add(lifeTime,i);
                return;
            }
        }
        deaths_.add(lifeTime);
    }

    /** Get an estimate of remaining lifespan using KM estimator */
    
    public int getKMEstimate(int life, int T) 
    { 
        if (KMTime_ == null)    // No parameters, return life
            return life*2;
        int l= KMTime_.size();
        for (int i= 0; i < KMTime_.size(); i++) {
            if (KMTime_.get(i) < life) {
                l= i;
                break;
            }
        }
        if (l >= KMTime_.size()) {
            return life*2;
        }
        int h= KMTime_.size()-1;
        if (T > 0) {
            for (int i= KMTime_.size()-1; i >= 0; i--) {
                if (KMTime_.get(i) < T) {
                    h= i;
                    break;
                }
            }
        }
        int estlife= 0;
        for (int i= l; i < h; i++) {
            estlife+=KMProb_.get(i)*(KMTime_.get(i) + KMTime_.get(i+1))/2;
        }    
        estlife-= KMProb_.get(l)*(KMTime_.get(l)+ life)/2;
        if (T > 0) {
            estlife-= KMProb_.get(h)*(KMTime_.get(h+1) + T)/2;
        }
        return (int)(estlife/KMProb_.get(l));
    }
    
    public int getTailEstimate(int life, int T, int KMT, double mu, double sig) {
        
        return 0;
    }

    /** Update tables for estimators using Kaplan--Meier procedure */
    public void updateKMEstimate(long time) {
        if (deaths_.size() < MIN_DEATHS || births_.size() < MIN_LIVE) {
             KMTime_= null;
             KMProb_= null;
             return; 
        }
        KMTime_= new ArrayList<Integer>();
        KMProb_= new ArrayList<Double>();
        double KMEst=1.0;
        KMTime_.add(0);
        KMProb_.add(1.0);
        int ni= deaths_.size() + births_.size();
        
        int deathCount= 0;
        int lifeCount= 0;
        int di= 0;
        
        int nextDeathTime= deaths_.get(0);
        int nextLifeTime= (int)(time - births_.get(0));
        
        while (true) {
            if (nextDeathTime == -1) {
                break;
            }
            if (nextDeathTime < nextLifeTime || nextLifeTime == -1 || nextDeathTime == -1) {
                int deathTime= nextDeathTime;
                while (nextDeathTime == deathTime) {
                    di+= 1;
                    deathCount+= 1;
                    if (deathCount >= deaths_.size()) {
                        nextDeathTime= -1;
                    } else {
                        nextDeathTime= deaths_.get(deathCount);
                    }
                }
                KMEst*=( (double)ni - di)/ni;
                ni-= di;
                KMTime_.add(deathTime);
                KMProb_.add(KMEst);
                di= 0;
                continue;
            }
            if (nextLifeTime == -1) {
                continue;
            }
            ni-= 1;
            lifeCount+= 1;
            if (lifeCount >= births_.size()) {
                nextLifeTime= -1;
            } else {
                nextLifeTime= (int) (time - births_.get(lifeCount));
            }
       }
   }    
  
  
      /**Calculate inverse erf function -- see wikipedia for formula
      We shoudl be good with small number of iter since y is small.
      */
      
      public static double inverf(double y) 
      {
          final int MAX_ERFITR= 20;
          double x= Math.sqrt(Math.PI)*y/2.0;
          double []cn= new double[MAX_ERFITR];
          cn[0]= 1.0;
          double inverf= x;
          for (int k= 1; k<MAX_ERFITR; k++) {
              cn[k]= 0.0;
              for (int m= 0; m <k; m++) {
                  cn[k]+= cn[m]*cn[k-1-m]/((m+1)*(m+2));
              }
              inverf+= cn[k]/(2*k+1)*(Math.pow(x,(2*k+1)));
          }
          return inverf;
      }
      
      /** Calculate inverse of erfc (1-erf)
      */
      public static double inverfc(double y) 
      {
          return inverf(1.0-y);
      }
      
  
     /** Calculate erf function 
      * see http://www.cs.princeton.edu/introcs/21function/
      * fractional error in math formula less than 1.2 * 10 ^ -7.
      * although subject to catastrophic cancellation when z in very close to 0
      * from Chebyshev fitting formula for erf(z) from Numerical Recipes, 6.2 */
      
    public static double erf(double z) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

        // use Horner's method
        double ans = 1 - t * Math.exp( -z*z   -   1.26551223 +
                                            t * ( 1.00002368 +
                                            t * ( 0.37409196 + 
                                            t * ( 0.09678418 + 
                                            t * (-0.18628806 + 
                                            t * ( 0.27886807 + 
                                            t * (-1.13520398 + 
                                            t * ( 1.48851587 + 
                                            t * (-0.82215223 + 
                                            t * ( 0.17087277))))))))));
        if (z >= 0) return  ans;
        else        return -ans;
    }

    // fractional error less than x.xx * 10 ^ -4.
    // Algorithm 26.2.17 in Abromowitz and Stegun, Handbook of Mathematical.
    public static double erf2(double z) {
        double t = 1.0 / (1.0 + 0.47047 * Math.abs(z));
        double poly = t * (0.3480242 + t * (-0.0958798 + t * (0.7478556)));
        double ans = 1.0 - poly * Math.exp(-z*z);
        if (z >= 0) return  ans;
        else        return -ans;
    }

    // cumulative normal distribution
    // See Gaussia.java for a better way to compute Phi(z)
    public static double Phi(double z) {
        return 0.5 * (1.0 + erf(z / (Math.sqrt(2.0))));
    }
    public static double erfc(double x) 
    {
        return 1-erf(x);
    }

}
