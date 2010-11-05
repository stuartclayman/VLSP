package usr.common;
/**  Special mathematical functions useful in code */

public class MathFunctions {

      /** Calculate the (lower) incomplete gamma function -- using Gauss continued fraction*/
      public static double incompleteGamma(double s, double z) 
      // See http://en.wikipedia.org/wiki/Incomplete_gamma_function
      // Continued fractions iterations method was invented by J. Wallis in 1655 (!),
      {
          final int MIN_ITER=10;
          final int MAX_ITER=1000;
          final double ACC_PERC=1e-06;
          double denom= 1.0+z-s;
          double value= Math.pow(z,s)*Math.exp(-z)/denom;
          double oldValue= 0.0;

          double Ajm1= Math.pow(z,s)*Math.exp(-z);
          double Ajm2= 0.0;
          double Bjm1= 1.0+z-s;         
          double Bjm2= 1.0;
          double Aj,Bj,aj,bj;
          for (int i=2; i < MAX_ITER+2;i++) {
               aj= (s-(i-1.0))*(i-1.0);
               bj= 2.0*i-1.0+z-s;
               Aj= bj*Ajm1 + aj*Ajm2;
               Bj= bj*Bjm1 + aj*Bjm2;
               value= Aj/Bj;
               if (Math.abs(value-oldValue)/value < ACC_PERC && i >= MIN_ITER)
                  break;
               oldValue=value;
               Bjm2=Bjm1;
               Bjm1= Bj;
               Ajm2=Ajm1;
               Ajm1=Aj;
     
          }
          return 1.0-value;
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
