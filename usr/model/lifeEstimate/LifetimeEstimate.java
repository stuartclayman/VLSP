package usr.model.lifeEstimate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import rgc.mathfunctions.MathFunctions;
import rgc.probdistributions.ProbElement;
import usr.common.Pair;
import usr.logging.Logger;
import usr.logging.USR;
import usr.router.RouterOptions;
/** Produces estimates of life spans given information about node births
   and deaths*/

public class LifetimeEstimate {
    ArrayList<Integer> deaths_;     // lifespan of nodes which are dead
    HashMap<Integer, Long> births_;    // birth time of nodes which are alive
    ArrayList<Integer> APDeaths_;     // Same for Agg Points
    HashMap<Integer, Long> APBirths_;
    RouterOptions options_ = null;     // Options for simulations
    ArrayList<Integer> KMTime_ = null;
    ArrayList<Double> KMProb_ = null;
    int T_;
    double mu_ = 0.0;      // Parameters of lognormal tail fit
    double sigma_ = 0.0;

    static LifetimeEstimate mainLSE_= null;

    static final int MIN_DEATHS = 5;
    static final int MIN_LIVE = 0;

    static final int MIN_TAIL_FIT = 10;
    static final double TAIL_PERCENT = 0.2;      // Percentage of estiamted
    // K-M estimator points to be replaced with tail estimate
    static final double TAIL_FIT_PERCENT = 0.2;    // Per centage of KM
    // estimator points to be used to fit lognormal parms
    static final double TAIL_MISS_PERCENT = 0.0;    // Per centage of KM
    // estimator points to be used to fit lognormal parms

    private LifetimeEstimate(RouterOptions o) {
        options_ = o;
        commonInit();
    }

    private LifetimeEstimate() {
        options_ = null;
        commonInit();
    }

    /**
     * Wrapper for constructor allows user to get a new lifespan estimator or reuse an existing one
     * @param RouterOptions
     * @return LifeSpanEstimate
     */
    public static LifetimeEstimate getLifetimeEstimate(RouterOptions o) {
    	if (mainLSE_ == null) {
    		mainLSE_= new LifetimeEstimate(o);
    	} else {
    		mainLSE_.setOptions(o);
    	}
    	return mainLSE_;
    }

    /**
     *
     * @return true if a lifetime estimator is being used
     */
    public static boolean usingLifetimeEstimate()
    {
    	return (mainLSE_.options_ != null); // sclayman 20131231 (mainLSE_ != null);
    }

    private void setOptions(RouterOptions o)
    {
    	options_= o;
    }

    /**
     * Wrapper for constructor allows user to get a new lifespan estimator or reuse an existing one
     * @param RouterOptions
     * @return LifeSpanEstimate
     */
    public static LifetimeEstimate getLifetimeEstimate() {
    	if (mainLSE_ == null) {
    		mainLSE_= new LifetimeEstimate();
    	}
    	return mainLSE_;
    }

    private void commonInit() {
        deaths_ = new ArrayList<Integer>();
        births_ = new HashMap<Integer, Long>();

        APDeaths_ = new ArrayList<Integer>();
        APBirths_ = new HashMap<Integer, Long>();
    }

    public double getAPLifeBias()
    {
    	return options_.getAPLifeBias();
    }

    /** Update lifespan estimates as result of latest data
     *
     */
    public void updateEstimates(long time)
    {
    	sortDeaths();
    	updateKMEstimate(time);
    	fitTail();
    }

    /** Plot (return co-ords of a graph of the Kaplan--Meier Estimator */
    public ArrayList<Pair<Integer, Double> > plotKMGraph(long time) {
        updateKMEstimate(time);

        if (KMTime_ == null) {
            Logger.getLogger("log").logln(USR.STDOUT,
                                          "Insuffient data for KM estimate");
            return null;
        }
        ArrayList<Pair<Integer, Double> > graph = new ArrayList<Pair<Integer, Double> >();

        for (int i = 0; i < KMTime_.size(); i++) {
            //double Fx= 0.5*erfc(-(Math.log(KMTime_.get(i)) - 4.0)/(2.0*Math.sqrt(2.0)));
            Pair<Integer, Double> p = new Pair<Integer, Double>(KMTime_.get(i), KMProb_.get(i));
            graph.add(p);
        }
        return graph;
    }

    /** Plot (return co-ords of a graph of the Kaplan--Meier Estimator with tail*/
    public ArrayList<Pair<Integer, Double> > plotKMGraphTail(long time) {
        updateKMEstimate(time);

        if (KMTime_ == null) {
            Logger.getLogger("log").logln(USR.STDOUT,
                                          "Insuffient data for KM estimate");
            return null;
        }
        fitTail();

        if (mu_ == 0 && sigma_ == 0) {   // Insufficient points for tail plot
            return plotKMGraph(time);
        }
        ArrayList<Pair<Integer, Double> > graph = new ArrayList<Pair<Integer, Double> >();


        Pair<Integer, Double> p;

        for (int i = 0; i < KMTime_.size(); i++) {
            //double Fx= 0.5*erfc(-(Math.log(KMTime_.get(i)) - 4.0)/(2.0*Math.sqrt(2.0)));
            if (KMTime_.get(i) < T_) {
                p = new Pair<Integer, Double>(KMTime_.get(i), KMProb_.get(i));
            } else {
                p = new Pair<Integer, Double>(KMTime_.get(i),
                                              getTailProb(KMTime_.get(i)));
            }
            graph.add(p);
        }
        return graph;
    }

    /** Fit the lognormal tail */
    public void fitTail() {
        /** Fit the parameters of the lognormal distribution */
        if (KMTime_ == null || KMTime_.size() * TAIL_FIT_PERCENT < MIN_TAIL_FIT) {
            mu_ = 0;
            sigma_ = 0;
            T_ = 0;
            return;
        }
        // Get time series of the distribution tail
        // log versus erfcinv (see paper)

        int startpos = (int)((KMTime_.size()-1.0) * (1.0 - TAIL_FIT_PERCENT-TAIL_MISS_PERCENT));
        int endpos = (int)((KMTime_.size()-1.0) * (1.0 - TAIL_MISS_PERCENT));
        int len = 1+endpos-startpos;
        double [] xi = new double[len];
        double [] yi = new double[len];

        for (int i = 0; i < len; i++) {
            xi[i] = Math.log(KMTime_.get(i+startpos));
            yi[i] = MathFunctions.inverfc(2.0*(1.0-KMProb_.get(i+startpos)));
        }
        // Now do a least squares fit.
        double xsum = 0.0;
        double ysum = 0.0;
        double xysum = 0.0;
        double xsqsum = 0.0;

        for (int i = 0; i < len; i++) {
            xsum += xi[i];
            ysum += yi[i];
            xysum += xi[i]*yi[i];
            xsqsum += xi[i]*xi[i];
        }
        double m = (xysum - xsum*ysum/len)/(xsqsum - xsum*xsum/len);
        double c = (ysum - m*xsum)/len;
        sigma_ = -1.0/(m*Math.sqrt(2.0));
        mu_ = sigma_*c*(Math.sqrt(2.0));
        //System.out.println("mu= "+mu_+" sigma= "+sigma_);
        //Calculate point where we are considered "in tail" for
        // purpose of calcs
        T_ = KMTime_.get((int)(KMTime_.size()*(1.0-TAIL_PERCENT)));

    }

    /** */
    public double getKMTailProb(int t) {
        if (KMTime_ == null) {
            return 0.0;
        }

        if (t < T_ || mu_ == 0 && sigma_ == 0) {
            return getKMProb(t);
        }
        return getTailProb(t);
    }

    /** returns the Kaplan-Meir estimate for time t */
    public double getKMProb(int t) {
        if (KMTime_ == null) {
            return 0.0;
        }

        for (int i = 0; i < KMTime_.size(); i++) {
            if (t <= KMTime_.get(i)) {
                return KMProb_.get(i);
            }
        }
        return 0.0;
    }

    /** Return the lognormal probability for a fitted tail */
    public double getTailProb(int t) {
        double rawProb = (1.0 -ProbElement.logNormalDist(t, mu_, sigma_));
        double fact = getKMProb(T_)/
            (1.0 -ProbElement.logNormalDist(T_, mu_, sigma_));
        return rawProb*fact;

    }

    /** A node is born at a given time -- register this */
    public void newNode(long time, int gid) {
        births_.put(gid, time);
    }

    /** A node dies -- register this */
    public void nodeDeath(long time, int gid) {
        int lifeTime = (int)(time - births_.get(gid) );
        births_.remove(gid);

        // Keep list of deaths ordered
        for (int i = 0; i < deaths_.size(); i++) {
            if (deaths_.get(i) >= lifeTime) {
                deaths_.add(i, lifeTime);
                return;
            }
        }
        deaths_.add(lifeTime);
    }

    /** A node is born at a given time -- register this */
    public void newAP(long time, int gid) {
        APBirths_.put(gid, time);
    }

    /** Add warm up (not real) node*/
    public void addWarmUpNode(long time) {
        newNode(time, (int)(time / 1000));
    }

    /** Remove warm up (not real) node */
    public void removeWarmUpNode(long startTime, long endTime) {
        nodeDeath(endTime, (int)(startTime / 1000));
    }

    public int getNodeLife(int id, long time) {
        Long birth = births_.get(id);

        if (birth == null) {
            return 0;
        }
        return (int)(time-birth);
    }

    /** An AP dies -- register this */
    public void APDeath(long time, int gid) {
        int lifeTime = (int)(time - APBirths_.get(gid) );
        APBirths_.remove(gid);

        // Keep list of deaths ordered
        for (int i = 0; i < APDeaths_.size(); i++) {
            if (APDeaths_.get(i) >= lifeTime) {
                APDeaths_.add(i, lifeTime);
                return;
            }
        }
        APDeaths_.add(lifeTime);
    }

    /** Return the mean life of a node -- this only includes
     * nodes which have died
     */
    public double meanNodeLife() {
        if (deaths_.size() == 0) {
            return 0.0;
        }
        double totLife = 0;

        for (int l : deaths_) {
            totLife += l;
        }
        return totLife/(1000.0*deaths_.size());
    }

    /** Return the mean life of an AP -- this only includes APs which have
       died*/
    public double meanAPLife() {
        double totLife = 0;

        if (APDeaths_.size() == 0) {
            return 0.0;
        }

        for (int l : APDeaths_) {
            totLife += l;
        }
        return totLife/(1000.0*APDeaths_.size());
    }

    /** Return the mean life of an AP -- includes all*/
    public double meanAPLifeSoFar(long time) {
        double totLife = 0;

        if (APDeaths_.size() == 0) {
            return 0.0;
        }
        int totAP = 0;

        for (int l : APDeaths_) {
            totLife += l;
            totAP++;
        }

        for (Long l : APBirths_.values()) {
            totLife += time-l;
            totAP++;
        }
        return totLife/(1000.0*totAP);
    }

    /** Get an estimate of remaining lifespan using KM estimator plus tail*/
    public long getKMTailLifeEst(int life) {
        if (KMTime_ == null) {   // No parameters, return life
            return life*2;
        }

        if (T_ <= 0) {       // Not enough data for tail fit.
            return getKMLifeEst(life);
        }

        int l = KMTime_.size();

        //System.err.println("Estimate in tail from "+l+" readings T_ = "+T_);
        for (int i = 0; i < KMTime_.size(); i++) {
            //System.err.println(life+" "+KMTime_.get(i));
            if (KMTime_.get(i) > life) {
                l = i;
                break;
            }
        }

        if (l >= KMTime_.size()-1) {
            l = KMTime_.size()-1;
        }

        int h = KMTime_.size()-2;

        for (int i = 0; i < KMTime_.size()-2; i++) {
            if (KMTime_.get(i) > T_) {
                h = i-1;
                break;
            }
        }

        //System.err.println(life+" "+l);
        double estlife = 0.0;

        // System.err.println("life  "+life+" T_"+ T_);
        if (life >= T_) {  // Estimate is from in tail
            estlife = ProbElement.logNormalCondExp(life, mu_, sigma_);
            //System.err.println("cond exp is "+estlife/getKMTailProb(life));
            return (long)(estlife);
        }

        for (int i = l; i < h-1; i++) {
            estlife += (KMProb_.get(i)-KMProb_.get(i+1))*
                (KMTime_.get(i)+KMTime_.get(i+1))/2.0;
        }
        estlife += (KMProb_.get(l-1)-KMProb_.get(l))*
            (life+KMTime_.get(l) )/2.0;
        estlife += ProbElement.logNormalCondExp(KMTime_.get(h), mu_, sigma_) *
            (1.0-ProbElement.logNormalDist(KMTime_.get(h), mu_, sigma_))/
            (1.0-ProbElement.logNormalDist(life, mu_, sigma_))
            *getKMTailProb(life);


        return (long)(estlife/getKMTailProb(life));

    }

    /** Get an estimate of remaining lifespan using KM estimator plus tail*/
    public long getKMLifeEst(int life) {
        if (KMTime_ == null) {  // No parameters, return life
            return life*2;
        }
        int l = KMTime_.size();

        for (int i = 0; i < KMTime_.size(); i++) {
            //System.err.println(life+" "+KMTime_.get(i));
            if (KMTime_.get(i) > life) {
                l = i;
                break;
            }
        }

        if (l >= KMTime_.size()-MIN_DEATHS) {
            return life*2;
        }

        //System.err.println(life+" "+l);
        double estlife = 0.0;

        for (int i = l; i < KMTime_.size()-1; i++) {
            estlife += (KMProb_.get(i)-KMProb_.get(i+1))*
                (KMTime_.get(i)+KMTime_.get(i+1))/2.0;
        }
        estlife += (KMProb_.get(l)-KMProb_.get(l+1))*
            (life+KMTime_.get(l) )/2.0;
        return (long)(estlife/(KMProb_.get(l)));
    }

    public void sortDeaths() {
        Collections.sort(deaths_);
    }

    /** Update tables for estimators using Kaplan--Meier procedure */
    public void updateKMEstimate(long time) {
        T_ = 0;
        mu_ = 0;
        sigma_ = 0;

        if (deaths_.size() < MIN_DEATHS || births_.size() < MIN_LIVE) {
            KMTime_ = null;
            KMProb_ = null;

            return;
        }
        KMTime_ = new ArrayList<Integer>();
        KMProb_ = new ArrayList<Double>();
        ArrayList<Long> life = new ArrayList<Long> (births_.values());
        //System.out.println("Start "+ni+ " deaths "+deaths_.size()+ " live "+ life.size());
        Collections.sort(life, Collections.reverseOrder());
        //System.out.println("Time "+time);
        //System.out.println(life);
        //System.err.println("deaths "+deaths_);
        //System.err.println("lives "+life);
        int nextDeathTime = deaths_.get(0);    // Times of next death or
        int nextLifeTime;

        if (life.size() == 0) {
            nextLifeTime = -1;
        } else {
            nextLifeTime = (int)(time - life.get(0));    // still alive (but only up to this time)
        }
        int dCount = 0;    // Position in array of births and deaths
        int lCount = 0;
        int ni = deaths_.size() + births_.size();  // No alive in system
        int prevni = ni;       // ni at previous time period
        int prevDCount = 0;
        int prevTime = -1;
        double totProb = 1.0;
        KMTime_.add(0);
        KMProb_.add(1.0);

        while (true) {
            if (ni == 0) {
                break;
            }
            // System.err.println("Next Death Time = "+nextDeathTime+
            //   "Next Life Time "+ nextLifeTime);
            int nextTime = getNextTime(nextDeathTime, nextLifeTime);

            if (nextTime == -1) {

                KMTime_.add(prevTime);
                KMProb_.add(totProb);
                break;
            }

            // Time we are considering has changed -- so move it on
            if (nextTime != prevTime) {

                int di = dCount-prevDCount;

                if (di != 0) {
                    if (prevTime == -1) {
                        KMTime_.add(0);
                    } else {
                        KMTime_.add(prevTime);
                    }
                    totProb *= (double)(prevni-di)/(double)prevni;
                    KMProb_.add(totProb);
                    //System.err.println("di="+di+"ni="+prevni);

                    //totProb= (double)(prevni)/(prevni+dCount);
                    prevDCount = dCount;
                }
                prevni = ni;
                prevTime = nextTime;

            }
            // Time corresponded to a lifespan of something
            // with no death yet observed

            if (nextTime == nextLifeTime) {
                //System.err.println("New life, length"+nextLifeTime);
                ni -= 1;
                lCount++;

                // If there are more "still alive" get the time
                if (lCount < life.size()) {
                    nextLifeTime = (int)(time-life.get(lCount));
                    // System.err.println("Next life "+nextLifeTime);
                } else {
                    nextLifeTime = -1;
                }
            }

            // Time corresponded to a lifespan of something
            // which is now dead
            if (nextTime == nextDeathTime) {
                // System.err.println("New death, length"+nextDeathTime);
                ni -= 1;
                dCount++;

                // If there are more deaths get the time
                if (dCount < deaths_.size()) {
                    nextDeathTime = deaths_.get(dCount);
                } else {
                    nextDeathTime = -1;
                }
            }

        }

    }

    //
    int getNextTime(int dt, int lt) {
        if (dt == -1) {
            return lt;
        }

        if (lt == -1) {
            return dt;
        }
        return Math.min(lt, dt);
    }

    /** accessor function for KMTime_*/
    public List<Integer> getKMTimeList() {
        return KMTime_;
    }

    /** accessor function for KMProb_*/
    public List<Double> getKMProbList() {
        return KMProb_;
    }

}
