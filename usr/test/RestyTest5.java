package usr.test;

import us.monoid.json.JSONObject;

/**
 * Test some calls to GlobalController using Resty
 */
class RestyTest5 extends RestyTest {

    public static void main(String[] args) {
        int port = 8850;
        int ingressCount = 1;

        // process args
        if (args.length > 0) {

            // try and process extra args
            for (int extra=0; extra < args.length; extra++) {
                String thisArg = args[extra];

                // check if its a flag
                if (thisArg.charAt(0) == '-') {
                    // get option
                    char option = thisArg.charAt(1);

                    switch (option) {
                    case 'p': {
                        // get next arg
                        String argValue = args[++extra];

                        try {
                            port = Integer.parseInt(argValue);
                        } catch (Exception e) {
                            System.err.println("Bad port " + argValue);
                        }
                        break;
                    }

                    case 'c': {
                        // get next arg
                        String argValue = args[++extra];

                        try {
                            ingressCount = Integer.parseInt(argValue);
                        } catch (Exception e) {
                            System.err.println("Bad ingress count " + argValue);
                        }
                        break;
                    }


                    default:
                        System.err.println("Bad option " + option);
                    }
                }

            }
        }


        try {
            RestyTest5 test = new RestyTest5();

            JSONObject r1 = test.createRouter();
            int router1 = (Integer)r1.get("routerID");
            System.out.println("r1 = " + r1);

            JSONObject r2 = test.createRouter();
            int router2 = (Integer)r2.get("routerID");
            System.out.println("r2 = " + r2);




            JSONObject l1 = test.createLink(router1, router2, 10);
            l1.get("linkID");
            System.out.println("l1 = " + l1);

            // let the routing tables propogate
            Thread.sleep(1000);


            // on router2, Egress on port 4000 send to UDP localhost:8856
            JSONObject a1 = test.createApp(router2, "demo_usr.paths.Egress", "4000 localhost:9860 -v");
            System.out.println("a1 = " + a1);
            Thread.sleep(500);

            for (int ingressInstance=0; ingressInstance < ingressCount; ingressInstance++) {
                // listen on UDP port 8850
                // send from router1 to router2 on port 4000
                JSONObject a2 = test.createApp(router1, "demo_usr.paths.Ingress", (port + ingressInstance) + " " + router2 + ":4000 " + " -b 64 -v"); 
                System.out.println("a2 = " + a2);

                Thread.sleep(250);
            }


            /*
            Thread.sleep(300000);

            JSONObject r1D = test.deleteRouter(router1);

            JSONObject r2D = test.deleteRouter(router2);
            */

        } catch (Exception e) {
        }
    }

}
