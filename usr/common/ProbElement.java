package usr.common;


public class ProbElement
{

    int distType_= 0;
    static final int EXPO_DIST= 1;  // Exponential/Poisson
    static final int GAMMA_DIST= 2;
    static final int WEIBULL_DIST= 3;
    static final int UNIFORM_DIST= 4;
    static final int PARETO_DIST= 5;
    double weight_;
    double parm1_;
    double parm2_;
    double parm3_;
    
    public ProbElement(String typeStr, double parm1, 
      double parm2, double parm3) throws ProbException
    {
        this(typeStr, 1.0, parm1, parm2, parm3);
    }
    public ProbElement(String typeStr, double weight, double parm1, 
      double parm2, double parm3) 
        throws ProbException {
        int type= 0;
        if (typeStr.equals("Exponential")) {
           type= EXPO_DIST;
        } else if (typeStr.equals("Gamma")) {
           type= GAMMA_DIST;
        } else if (typeStr.equals("Weibull")) {
           type= WEIBULL_DIST;
        } else if (typeStr.equals("Uniform")) {
           type= UNIFORM_DIST;
        } else if (typeStr.equals("Pareto")) {
           type= PARETO_DIST;
        }
        if (type == 0) {
           throw new ProbException("Unknown distribution type");
        }
        distType_= type;
        weight_= weight;
        parm1_= parm1;
        parm2_= parm2;
        parm3_= parm3;
           
    }

    public double getVariate() throws ProbException {
        if (distType_ == EXPO_DIST) {
            return exponentialVariate(parm1_);
        }
        if (distType_ == WEIBULL_DIST) {
            return weibullVariate(parm1_,parm2_);
        }
        if (distType_ == GAMMA_DIST) {
            return gammaVariate(parm1_, parm2_);
        }
        if (distType_ == UNIFORM_DIST) {
            return uniformVariate(parm1_, parm2_);
        }
        if (distType_ == PARETO_DIST) {
            return paretoVariate(parm1_, parm2_);
        }
        throw new ProbException("Unknown distribution type");
        
    }

    public double getWeight() {
        return weight_;
    }

    public static double weibullVariate(double lambda, double beta) {
        //Weibulls wobble but they don't fall down
        double power,x;
        double p= Math.random();
        power = 1.0/beta;
        x = lambda*Math.pow(-Math.log(1.0 - p),power);

        return x;

   }
   public static double exponentialVariate(double lambda) {
        //Weibulls wobble but they don't fall down
        double p, x;
        p= Math.random();
        x = - Math.log(1.0 - p)/lambda;

        return x;

   }
   
   /** Generate Gamma variate taken from: JSTOR vol 23 no 3 pp 290-295 --
   there are faster methods but this is simple though it does look mental*/

    public static double gammaVariate (double shape, double scale)
    {
          double a= shape -1.0;
          double b= (shape- 1.0/(6.0*shape))/a;
          double c= 2.0/a;
          double d= c+ 2.0;
          
          double U1, U2,U,W;
          
          while(true) {
             U1= Math.random();
             
             if (scale <= 2.5) {
                 U2= Math.random();
             } else {
                 while(true) {
                    U= Math.random();
                    U2= U1 + (1.0 - 1.86*U)/Math.sqrt(shape);
                    if (U2 < 1.0 && U2 > 0.0) 
                        break;
                 }
             }
             W= b*U1/U2;
             if (c*U2-d+W+1.0/W <= 0.0) 
                 break;
             if (c*Math.log(U2) - Math.log(W)+W-1.0 < 0.0)
                 break;
          }
          return scale*a*W;
    }
   
    
    public static double uniformVariate(double min, double max) 
    {
        double x;
        x= Math.random();
        x= x*(max-min)+min;
        return x;
    }
    
    public static double paretoVariate(double scale, double shape)
    {
        double x, U;
        U= exponentialVariate(1.0/shape);
        x= scale*Math.exp(U);
        return x;
    }
    
}
