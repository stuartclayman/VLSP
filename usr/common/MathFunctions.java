package usr.common;
/**  Special mathematical functions useful in code */

public class MathFunctions {

    public static double gammaLn(double s)
    /* Return the ln Gamma(s) for s > 0  -- taken from
       Numerical recipies in C*/
    {
	double x,y,tmp,ser;
	double [] cof={76.18009172947146,
	               -86.50532032941677,
	               24.01409824083091,
	               -1.231739572450155,
	               0.1208650973866179e-2,
	               -0.5395239384953e-5};
	y=x=s;
	tmp=x+5.5;
	tmp-= (x+0.5)*Math.log(tmp);
	ser= 1.000000000190015;
	for (int j= 0; j < 6; j++) {
	    y++;
	    ser+=cof[j]/y;
	}
	return -tmp+Math.log(2.5066282746310005*ser/x);
    }

    /* Return the complete gamma fucntion*/
    public static double completeGamma(double s)
    {
	return Math.exp(gammaLn(s));
    }

    /* Return the upper incomplete gamma fucntion a is shape, x scale*/
    public static double incompleteLowerGamma(double a, double x)
    {
	if (x < a+1.0) {
	    return incompleteLowerGammaER(a,x);
	} else {
	    return completeGamma(a)-incompleteUpperGammaCF(a,x);
	}
    }

    /** Calculate the (Upper) incomplete gamma function -- from num rec in C*/
    /** a = shape, x is range */
    public static double incompleteUpperGamma(double a, double x)
    {
	if (x < a+1.0) {
	    return completeGamma(a)-incompleteLowerGammaER(a,x);
	} else {
	    return incompleteUpperGammaCF(a,x);
	}
    }

    /** incomplete lower gamma by series expansion */
    public static double incompleteLowerGammaER(double a, double x)
    {
	final int ITMAX= 100;
	final double EPS= 3.0e-7;
	final double FPMIN= 1.0e-30;

	double sum,del,ap;
	ap= a;
	del=sum=1.0/a;
	for (int n= 1; n<=ITMAX; n++) {
	    ap++;
	    del*= x/ap;
	    sum+= del;
	    if (Math.abs(del) < Math.abs(sum)*EPS) {
		return sum*Math.exp(-x+a*Math.log(x));
	    }
	}
	System.err.println("incompleteLowerGammaER failed to converge");
	return 0.0;
    }


    /** incomplete upper gamma by continued fraction */
    public static double incompleteUpperGammaCF(double a, double x)
    {
	final int ITMAX= 100;
	final double EPS= 3.0e-7;
	final double FPMIN= 1.0e-30;

	double an, b,c,d,del,h;
	//double gln= gammaLn(s);
	b= x+1.0-a;
	c= 1.0/FPMIN;
	d=1.0/b;
	h=d;
	for (int i=1; i <=ITMAX; i++) {
	    an= -i*(i-a);
	    b+= 2.0;
	    d=an*d+b;
	    if (Math.abs(d) < FPMIN)
		d= FPMIN;
	    c= b+an/c;
	    if (Math.abs(c) < FPMIN)
		c= FPMIN;
	    d= 1.0/d;
	    del=d*c;
	    h*=del;
	    if (Math.abs(del-1.0) < EPS)
		break;

	}
	return Math.exp(-x+a*Math.log(x))*h;

    }

/**Calculate inverse erf function -- see wikipedia for formula
      We shoudl be good with small number of iter since y is small.
 */

    public static double inverf(double y)
    {
	final int MAX_ERFITR= 200;
	double x= Math.sqrt(Math.PI)*y/2.0;
	double [] cn= new double[MAX_ERFITR];
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
	if (z >= 0) return ans;
	else return -ans;
    }

    // fractional error less than x.xx * 10 ^ -4.
    // Algorithm 26.2.17 in Abromowitz and Stegun, Handbook of Mathematical.
    public static double erf2(double z) {
	double t = 1.0 / (1.0 + 0.47047 * Math.abs(z));
	double poly = t * (0.3480242 + t * (-0.0958798 + t * (0.7478556)));
	double ans = 1.0 - poly * Math.exp(-z*z);
	if (z >= 0) return ans;
	else return -ans;
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
