#
# define compiler and compiler flag variables
#
JFLAGS = -g
JC = javac

#
# Clear any default targets for building .class files from .java files; we 
# will provide our own target entry to do this in this makefile.
# make has a set of default targets for different suffixes (like .c.o) 
# Currently, clearing the default for .java.class is not necessary since 
# make does not have a definition for this target, but later versions of 
# make may, so it doesn't hurt to make sure that we clear any default 
# definitions for these
#
.SUFFIXES: .java .class


#
# Here is our target entry for creating .class files from .java files 
# This is a target entry that uses the suffix rule syntax:
#	DSTS:
#		rule
#  'TS' is the suffix of the target file, 'DS' is the suffix of the dependency 
#  file, and 'rule'  is the rule for building a target	
# '$*' is a built-in macro that gets the basename of the current target 
# Remember that there must be a < tab > before the command line ('rule') 
#

.java.class:
	$(JC) $(JFLAGS) $*.java 
CLASSES = \
  usr/test/RouterTest1.java \
  usr/test/StubServer.java \
  usr/test/StubClient.java \
  usr/test/VariateTest.java \
  usr/router/Router.java \
  usr/router/command/QuitCommand.java \
  usr/router/command/GetNameCommand.java \
  usr/router/command/SetAddressCommand.java \
  usr/router/command/CreateConnectionCommand.java \
  usr/router/command/IncomingConnectionCommand.java \
  usr/router/command/GetConnectionPortCommand.java \
  usr/router/command/GetAddressCommand.java \
  usr/router/command/SetNameCommand.java \
  usr/router/command/ListConnectionsCommand.java \
  usr/router/command/UnknownCommand.java \
  usr/router/command/ReadOptionsFileCommand.java \
  usr/router/command/EndLinkCommand.java \
  usr/router/command/ReadOptionsStringCommand.java \
  usr/router/command/RouterCommand.java \
  usr/router/command/ShutDownCommand.java \
  usr/router/command/GetPortRemoteRouterCommand.java \
  usr/router/command/SetWeightCommand.java \
  usr/router/command/GetPortNameCommand.java \
  usr/router/command/GetWeightCommand.java \
  usr/router/command/ListRoutingTableCommand.java \
  usr/router/SimpleRouterFabric.java \
  usr/router/CreateConnection.java \
  usr/router/RouterConnections.java \
  usr/router/RouterController.java \
  usr/router/NetIF.java \
  usr/router/RouterPort.java \
  usr/router/RouterFabric.java \
  usr/router/TCPNetIF.java \
  usr/router/EndLink.java \
  usr/router/RouterManagementConsole.java \
  usr/router/RoutingTable.java \
  usr/router/RoutingTableEntry.java \
  usr/router/RouterOptions.java \
  usr/net/Datagram.java \
  usr/net/TCPEndPointSrc.java \
  usr/net/DatagramPatch.java \
  usr/net/Connection.java \
  usr/net/IPV4Datagram.java \
  usr/net/SimpleDatagram.java \
  usr/net/SimpleAddress.java \
  usr/net/Address.java \
  usr/net/IPV4Address.java \
  usr/net/EndPoint.java \
  usr/net/UDPEndPointSrc.java \
  usr/net/TCPEndPointDst.java \
  usr/net/ConnectionOverTCP.java \
  usr/net/UDPEndPointDst.java \
  usr/net/ConnectionOverUDP.java \
  usr/net/TCPEndPoint.java \
  usr/net/UDPEndPoint.java \
  usr/interactor/MCRPException.java \
  usr/interactor/MCRPResponse.java \
  usr/interactor/MCRPEvent.java \
  usr/interactor/MCRPNotReadyException.java \
  usr/interactor/MCRPEventListener.java \
  usr/interactor/ID.java \
  usr/interactor/MCRPInteractor.java \
  usr/interactor/InputHandler.java \
  usr/interactor/MCRPNoConnectionException.java \
  usr/interactor/GlobalControllerInteractor.java \
  usr/interactor/LocalControllerInteractor.java \
  usr/interactor/RouterInteractor.java \
  usr/common/LocalHostInfo.java \
  usr/common/ProbDistribution.java \
  usr/common/ProbElement.java \
  usr/common/ProbException.java \
  usr/common/Pair.java \
  usr/common/ProcessWrapper.java \
  usr/common/ThreadTools.java \
  usr/common/BasicRouterInfo.java \
  usr/common/PortPool.java \
  usr/common/ReadXMLUtils.java \
  usr/common/XMLNoTagException.java \
  usr/protocol/MCRP.java \
  usr/protocol/Protocol.java \
  usr/console/ManagementConsole.java \
  usr/console/AbstractCommand.java \
  usr/console/AbstractManagementConsole.java \
  usr/console/ComponentController.java \
  usr/console/Request.java \
  usr/console/ChannelResponder.java \
  usr/console/Command.java \
  usr/globalcontroller/command/GlobalCommand.java \
  usr/globalcontroller/command/QuitCommand.java \
  usr/globalcontroller/command/LocalOKCommand.java \
  usr/globalcontroller/command/UnknownCommand.java \
  usr/globalcontroller/EventScheduler.java \
  usr/globalcontroller/GlobalController.java \
  usr/globalcontroller/GlobalControllerManagementConsole.java \
  usr/globalcontroller/SimEvent.java \
  usr/globalcontroller/ControlOptions.java \
  usr/localcontroller/command/QuitCommand.java \
  usr/localcontroller/command/LocalCheckCommand.java \
  usr/localcontroller/command/LocalCommand.java \
  usr/localcontroller/command/NewRouterCommand.java \
  usr/localcontroller/command/ShutDownCommand.java \
  usr/localcontroller/command/UnknownCommand.java \
  usr/localcontroller/command/ConnectRoutersCommand.java \
  usr/localcontroller/command/EndRouterCommand.java \
  usr/localcontroller/command/EndLinkCommand.java \
  usr/localcontroller/command/RouterConfigCommand.java \
  usr/localcontroller/LocalControllerInfo.java \
  usr/localcontroller/LocalController.java \
  usr/localcontroller/LocalControllerManagementConsole.java \
  usr/engine/EventEngine.java \
  usr/engine/EmptyEventEngine.java \
  usr/engine/TestEventEngine.java \
  usr/engine/ScriptEngine.java \
  usr/engine/ProbabilisticEventEngine.java \
  usr/output/OutputType.java

#
# the default make target entry
#

default: classes


# This target entry uses Suffix Replacement within a macro: 
# $(name:string1=string2)
# 	In the words in the macro named 'name' replace 'string1' with 'string2'
# Below we are replacing the suffix .java of all words in the macro CLASSES 
# with the .class suffix
#

classes: $(CLASSES:.java=.class)


#
# RM is a predefined macro in make (RM = rm -f)
#

clean:
	find . -name "*.class" -exec rm {} \;

