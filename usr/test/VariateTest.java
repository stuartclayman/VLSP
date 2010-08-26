package usr.test;

import usr.common.ProbElement;
import usr.common.ProbException;



public class VariateTest {
    public static void main(String[] args) {
        int i;
        int noTests;
        String distType;
        double parm1= 0.0 , parm2= 0.0, parm3= 0.0;
        if (args.length < 3) {
            System.out.println("Need arguments --  no_variates Type(string), parm 1, parm2, parm3");
        }
        distType= args[1];
        noTests= Integer.parseInt(args[0]);
        parm1= Double.parseDouble(args[2]);
        if (args.length > 3)
            parm2= Double.parseDouble(args[3]);
        if (args.length > 4)
            parm3= Double.parseDouble(args[4]);
        try {
            ProbElement el= new ProbElement(distType, parm1, parm2, parm3);
            for (i= 0; i < noTests; i++) {
            
                System.out.println(el.getVariate());
            } 
        } catch (ProbException e){
                System.out.println(e.getMessage());
                System.exit(-1);
        }
    }

}
