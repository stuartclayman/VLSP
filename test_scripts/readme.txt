All scripts are started with

java usr.globalcontroller.GlobalController script.xml

Appropriate classpath must be set first, including the directory
with the class files and the .jar libraries

This command starts simulation mode test:
java usr.globalcontroller.GlobalController simulation_test.xml

This command starts emulation mode test:
java usr.globalcontroller.GlobalController emulation_test.xml

This command starts an empty controller:
java usr.globalcontroller.GlobalController start_global_controller.xml

In any emulation mode you can communicate with the global controller
using curl e.g.

# Start router 1
curl -X POST http://localhost:8888/router/
# Start router 2
curl -X POST http://localhost:8888/router/
# Link routers
curl -X POST http://localhost:8888/link/?router1=1\&router2=2
# Start app on router 2
curl -X POST http://localhost:8888/router/2/app/?className=usr.applications.Recv\&args=4000
# Delete router 1
curl -X DELETE http://localhost:8888/router/1
# Shut down controller
curl -X POST http://localhost:8888/command/SHUT_DOWN
