package usr.globalcontroller;

import usr.output.*;
import java.io.PrintStream;
import java.util.*;

/** Class provides a thread to get traffic estimates from global controller*/
public class TrafficEstimateThread implements Runnable {
    
    OutputType o_= null;
    long time_= 0;
    PrintStream s_= null;
    GlobalController gCont_= null;
    
    /** Constructor */
    TrafficEstimateThread(PrintStream s, OutputType o, long time, 
      GlobalController g) {
        o_= o;
        time_= time;
        gCont_= g;
        s_= s;
    }

    public void run() {
         List<String> out= gCont_.getRouterStats();
         if (out == null) 
            return;
         if (o_.getParameter().equals("Local")) {
             outputLocal(out);
         } else if (o_.getParameter().equals("Aggregate")) {
             outputAggregate(out);
         } else if (o_.getParameter().equals("Raw")) {
             for (String s: out) {
                s_.println(s);
             } 
         } else {
             outputSeparate(out);
         }
    }

    void outputLocal(List <String> out) 
    {
            for (String s: out) {
            String []args= s.split("\\s+");
            if (o_.isFirst()) {
                o_.setFirst(false);
                s_.print("Time r_no name ");
                for (int i= 2; i < args.length;i++) {
                    s_.print(args[i].split("=")[0]);
                    s_.print(" ");
                }
                s_.println();
            }
            if (args.length < 2)
                continue;
            if (!args[1].equals("localnet"))
                continue;
            s_.print(time_+" ");
            s_.print(args[0]+" "+args[1]);
            for (int i= 2; i < args.length;i++) {
                s_.print(args[i].split("=")[1]);
                s_.print(" ");
            }
            s_.println();
        }
    }
    
    void outputAggregate (List <String> out) 
    {
        Hashtable<Integer,Boolean> routers= new Hashtable<Integer,Boolean>();       
        if (out.size() < 1) 
            return;
        int nField= out.get(0).split("\\s+").length;
        if (nField <= 0)
            return;
        int []count= new int [nField];
        for (int i= 0; i < nField; i++) {
            count[i]= 0;
        } 
        for (String s: out) {
            String []args= s.split("\\s+");
            if (o_.isFirst()) {
                o_.setFirst(false);
                s_.print("Time nRouters nLinks*2");
                for (int i= 2; i < args.length;i++) {
                    s_.print(args[i].split("=")[0]);
                    s_.print(" ");
                }
                s_.println();
            }
            if (args.length < 2)
                continue;
            if (args[1].equals("localnet"))
                continue;
            int router= Integer.parseInt(args[0]);
            //System.err.println(args[1]+" "+Rname);
            if (!routers.containsKey(router)){
                count[0]++;
                routers.put(router,true);
            }
            count[1]++;
            for (int i= 2; i < args.length;i++) {
                count[i]+=Integer.parseInt(args[i].split("=")[1]);
            }
        }
        s_.print(time_+" ");
        for (int i= 0; i < nField; i++) {
            s_.print(count[i]+" ");
        }
        s_.println();
    }
    
    void outputSeparate (List <String> out) 
    {
        if (out == null) 
            return;
        for (String s: out) {
            String []args= s.split("\\s+");
            if (o_.isFirst()) {
                o_.setFirst(false);
                s_.print("Time r_no name ");
                for (int i= 2; i < args.length;i++) {
                    s_.print(args[i].split("=")[0]);
                    s_.print(" ");
                }
                s_.println();
            }
            if (args.length < 2)
                continue;
            if (args[1].equals("localnet"))
                continue;
            s_.print(time_+" ");
            s_.print(args[0]+" "+args[1]);
            for (int i= 2; i < args.length;i++) {
                s_.print(args[i].split("=")[1]);
                s_.print(" ");
            }
            s_.println();
        }
    }

}



