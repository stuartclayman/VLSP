# VLSP User Space Routing

The VLSP framework - sometimes called _User Space Routing_ as it does everything in user-space. We have designed a testbed called the _Very Lightweight Network & Service Platform_ based on this framework. It uses a set of Virtual Routers and Virtual Network Connections to create a test environment which can easily accommodate (i) fast setup and teardown of a Virtual Router, and (ii) fast setup and teardown of a Virtual Connection. 

Each Virtual Router can run small programs and service elements.
The platform consists of a number of virtual routers running as Java virtual machines (JVMs) across a number of physical machines. The routers are logically independent software entities which, as with real routers, communicate with each other via network interfaces. The network traffic is made up of datagrams, which are send to and from each router. The datagrams are not real UDP datagrams, but are our own virtual datagrams (called USR datagrams) which are used.

The platform has three components, the major one of which is the “Router” itself. They are complemented by a lightweight “Local Controller” which is similar to a hypervisor and has the role of sending instructions to start up or shutdown routers on the local machine and for routers to setup or tear-down connections with other virtual routers. The whole testbed is supervised by a “Global Controller”. This software entity is like a combined Virtual Infrastructure Management (VIM) / Orchestrator, and has the role of a co-ordinator. That is to say, it informs Local Controllers when to start up and shut down routers and when to connect them to each other or disconnect them from each other.

_What is VLSP good for?_  

- Testing and evaluating alternative SDN / NFV scenarios
- Mid scale tests of network software written in Java (100s and likely 1000s of virtual
routers).
- Testing software robustness to “unexpected” network conditions (sudden “rude” start up/shut down exposes software deficiencies).
- Testing software robustness to unreliable networks.
- A compromise between simulation (realism questionable) and large testbed (requires many physical machines).
- Comparing simulation with testbed results

_What is VLSP not yet good for?_  

-  It is not optimized for forwarding performance, compared to a real router it routes packets at a slower speed and uses more overhead (i.e. due to the focus on the support of new network management and control features).
- It is difficult to support facilities and protocols that rely on maximum bandwidth calculations, e.g. traffic engineering algorithms estimating the link bandwidth.
-   The direct interaction with or the driving of hardware interfaces is out not currently addressed.

## Build VLSP

If you do not have VLSP yet, you can find the source on github at https://github.com/stuartclayman/VLSP.

If VLSP is not built yet, you can run `ant build` to compile it.  Then it is ready to run.

As the platform is written in Java, we need to set the JAVA HOME environment variable:
`$export JAVA HOME=/usr/java/jdk1.8/`

## Documentation

You can get the documentation at  https://github.com/stuartclayman/VLSP-Doc

## Clients

If you do not have VLSP Client yet, you can find the source on
_gitlab_ at https://gitlab.com/sclayman/vlsp-client



## Starting VLSP



To start the run of the platform, either set the current directory to be where the platform is installed and the relevant environment variables need to be set before everything is started

On the terminal run these commands:

Setup the CLASSPATH  
`$ export CLASSPATH=.:libs/*` 

Run VLSP  
`$ java usr.vim.Vim scripts/control-wait.xml`

This starts the Virtual Infrastructure Manager, using the config file in `scripts/control-wait.xml`.

This platform will produce a lot of output as it starts up.  The last few lines looks similar to this:

```
LeastUsedLoadBalancer: localcontrollers = [localhost:10000]  
192.168.7.104:8888 GC: Setup PlacementEngine: null  
EventScheduler: Adding Event at time: 0 Event StartSimulation: 0  
EventScheduler: Adding Event at time: 50065408 Event EndSimulation 50065408  
EventScheduler: 00:00:00  starting event StartSimulation: 0  
192.168.7.104:8888 GC: Start of simulation  at: 0 1643992328463  
192.168.7.104:8888 GC:  {"msg":"Simulation started","success":true}  
EventScheduler: 00:00:01  finishing event StartSimulation: 0  
EVENT: <0> 11 @ 1643992328464 waiting 50065397  
```

VLSP is now ready and waiting for commands.


### Distributed deployments

VLSP can be executed across a number of servers.

It uses `ssh` to connect to remote machines, and starts the *Local Controllers*.
You need to configure ssh logins with a key and not using a password.
Also the `CLASSPATH` is exported to the remote machines, so it is best
to use absolute path names, so the setup the CLASSPATH is something
like:

`$ export CLASSPATH=/home/sclayman/vlsp/:/home/sclayman/vlsp/:libs/*` 

