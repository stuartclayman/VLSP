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
    ArrayList <Integer> APDeaths_;    // Same for Agg Points
    HashMap <Integer,Long> APBirths_;
    RouterOptions options_;     // Options for simulations
    ArrayList <Integer> KMTime_= null;
    ArrayList <Double> KMProb_= null;
    double mu_= 0.0;
    double sigma_= 0.0;
    
    static final int MIN_DEATHS= 5;
    static final int MIN_LIVE= 5;
    static final int MIN_TAIL_FIT= 10;
    static final double TAIL_PERCENT= 0.1;
    static final double TAIL_FIT_PERCENT=0.35;
    
    
    public LifeSpanEstimate(RouterOptions o) 
    {
        options_= o;
        deaths_= new ArrayList <Integer>();
        births_= new HashMap <Integer,Long>();
        APDeaths_= new ArrayList <Integer>();
        APBirths_= new HashMap <Integer,Long>();
    }    
    
    public LifeSpanEstimate() 
    {
        options_= null;
        deaths_= new ArrayList <Integer>();
        births_= new HashMap <Integer,Long>();
        
        APDeaths_= new ArrayList <Integer>();
        APBirths_= new HashMap <Integer,Long>();
    }    

    
    
    /** Plot a graph of the Kaplan--Meier Estimator */
    public void plotKMGraph(long time) {
        updateKMEstimate(time);
        if (KMTime_ == null) {
            Logger.getLogger("log").logln(USR.STDOUT, 
                "Insuffient data for KM estimate");
            return;
        }
        for (int i= 0; i < KMTime_.size();i++) {
            double Fx= 0.5*erfc(-(Math.log(KMTime_.get(i)) - 4.0)/(2.0*Math.sqrt(2.0)));
            Logger.getLogger("log").logln(USR.STDOUT, KMTime_.get(i)+" "+ KMProb_.get(i) + " "
                + (1.0 - Fx));
        }
    }

    /** Fit the lognormal tail */
    public void fitTail() {
        /** Fit the parameters of the lognormal distribution */
        if (KMTime_ == null || KMTime_.size() * TAIL_FIT_PERCENT < MIN_TAIL_FIT) {
            mu_= 0;
            sigma_= 0;
            return;
        }
        // Get time series of the distribution tail
        // log versus erfcinv (see paper)
        
        int startpos= (int)(KMTime_
        .size() * (1.0 - TAIL_FIT_PERCENT));
        int len= (KMTime_.size()-10)-startpos;
        double []xi= new double[len];
        double []yi= new double[len];
        for (int i= 0; i < len; i++) {
            xi[i]= (Math.log((KMTime_.get(i+startpos+1)+KMTime_.get(i+startpos))/2));
            yi[i]= (inverfc(2.0*(1.0-KMProb_.get(i+startpos))));
        }    
        // Now do a least squares fit.
        double xsum= 0.0;
        double ysum= 0.0;
        double xysum= 0.0;
        double xsqsum= 0.0;   
        for (int i= 0; i < len;i++) {
            xsum+= xi[i];
            ysum+= yi[i];
            xysum+= xi[i]*yi[i];
            xsqsum+= xi[i]*xi[i];
        }
        double m= (xysum - xsum*ysum/len)/(xsqsum - xsum*xsum/len);
        double c= (ysum - m*xsum)/len;
        sigma_= -1.0/(m*Math.sqrt(2.0));
        mu_=sigma_*c*(Math.sqrt(2.0));
        System.out.println("mu= "+mu_+" sigma= "+sigma_);
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
        for (int i= 0; i < deaths_.size();i++) { // Keep list of deaths ordered
            if (deaths_.get(i) >= lifeTime) {
                deaths_.add(i,lifeTime);
                return;
            }
        }
        deaths_.add(lifeTime);
    }

    /** A node is born at a given time -- register this */
    public void newAP(long time, int gid) 
    {
        APBirths_.put(gid,time);
    }
    
    /** A node dies -- register this */
    public void APDeath(long time, int gid)
    {
        int lifeTime= (int)(time - APBirths_.get(gid) );
        APBirths_.remove(gid);
        for (int i= 0; i < APDeaths_.size();i++) { // Keep list of deaths ordered
            if (APDeaths_.get(i) >= lifeTime) {
                APDeaths_.add(i,lifeTime);
                return;
            }
        }
        APDeaths_.add(lifeTime);
    }
            /** Return the mean life of a node -- this only includes
     nodes which have died*/
    public double meanNodeLife()
    { 
        if (deaths_.size() == 0) {
            return 0.0;
        }
        double totLife= 0;
        for (int l :deaths_) {
            totLife+= l;
        }
        return totLife/(1000.0*deaths_.size());
    }
    
    /** Return the mean life of an AP -- this only includes APs which have
    died*/
    public double meanAPLife() {
        double totLife= 0;
        if (APDeaths_.size() == 0) {
            return 0.0;
        }
        for (int l : APDeaths_) {
            totLife+= l;
        }
        return totLife/(1000.0*APDeaths_.size());
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
    
    public void sortDeaths()
    {
        Collections.sort(deaths_);
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
        int ni= deaths_.size() + births_.size();
        
        ArrayList <Long> life= new ArrayList<Long> (births_.values());
        System.out.println("Start "+ni+ " deaths "+deaths_.size()+ " live "+ life.size());
        Collections.sort(life,Collections.reverseOrder());
        System.out.println("Time "+time);
        System.out.println(life);
        int deathCount= 0;
        int lifeCount= 0;
        int di= 0;
        
        int nextDeathTime= deaths_.get(0);
        int nextLifeTime= (int)(time - life.get(0));
        int totExpire= 0;
        int totD= 0;
        while (true) {
            if (nextDeathTime == -1) {
                break;
            }
            if (nextDeathTime < nextLifeTime || nextLifeTime == -1) {
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
                totD+= di;
                KMTime_.add(deathTime);
                KMProb_.add(KMEst);
                di= 0;
                continue;
            }
           
            ni-= 1;
            totExpire+= 1;
            lifeCount+= 1;
            if (lifeCount >= births_.size()) {
                nextLifeTime= -1;
            } else {
                nextLifeTime= (int) (time - life.get(lifeCount));
                
            }
       }
       System.out.println("Alive at end of KM "+ni+" tot deaths "+totD +
        " tot expire "+totExpire);
   }    
  
  
      /**Calculate inverse erf function -- see wikipedia for formula
      We shoudl be good with small number of iter since y is small.
      */
      
      public static double inverf(double y) 
      {
          final int MAX_ERFITR= 200;
          double x= Math.sqrt(Math.PI)*y/2.0;
          double []cn= new double[MAX_ERFITR];
          cn[0]= 1.0;
          double inverf= x;
          for (int k= 1; k<MAX_ERFITR; k++) {
              cn[k]= 0.0;
              for (int m= 0; m <k; m++) {
                  cn[k]+= cn[m]*cn[k-1-m]/((m+1)*(2*m+1));
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
