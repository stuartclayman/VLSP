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
  usr/router/command/RouterCommand.java \
  usr/router/SimpleRouterFabric.java \
  usr/router/CreateConnection.java \
  usr/router/RouterConnections.java \
  usr/router/RouterController.java \
  usr/router/NetIF.java \
  usr/router/RouterPort.java \
  usr/router/RouterFabric.java \
  usr/router/TCPNetIF.java \
  usr/router/RouterManagementConsole.java \
  usr/net/Datagram.java \
  usr/net/DatagramConnection.java \
  usr/net/DatagramPatch.java \
  usr/net/DatagramQueueingConnection.java \
  usr/net/IPV4Datagram.java \
  usr/net/Address.java \
  usr/net/IPV4Address.java \
  usr/interactor/MCRPException.java \
  usr/interactor/MCRPResponse.java \
  usr/interactor/MCRPEvent.java \
  usr/interactor/MCRPNotReadyException.java \
  usr/interactor/MCRPEventListener.java \
  usr/interactor/ID.java \
  usr/interactor/MCRPInteractor.java \
  usr/interactor/InputHandler.java \
  usr/interactor/MCRPNoConnectionException.java \
  usr/interactor/package-info.java \
  usr/interactor/RouterInteractor.java \
  usr/interactor/GlobalControllerInteractor.java \
  usr/interactor/LocalControllerInteractor.java \
  usr/common/LocalHostInfo.java \
  usr/common/ProbDistribution.java \
  usr/common/ProbElement.java \
  usr/common/ProbException.java \
  usr/common/Pair.java \
  usr/protocol/MCRP.java \
  usr/protocol/Protocol.java \
  usr/protocol/package-info.java \
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
  usr/globalcontroller/command/package-info.java \
  usr/globalcontroller/command/UnknownCommand.java \
  usr/globalcontroller/EventScheduler.java \
  usr/globalcontroller/GlobalController.java \
  usr/globalcontroller/GlobalControllerManagementConsole.java \
  usr/globalcontroller/package-info.java \
  usr/globalcontroller/XMLNoTagException.java \
  usr/globalcontroller/SimEvent.java \
  usr/globalcontroller/ControlOptions.java \
  usr/localcontroller/command/QuitCommand.java \
  usr/localcontroller/command/LocalCheckCommand.java \
  usr/localcontroller/command/LocalCommand.java \
  usr/localcontroller/command/NewRouterCommand.java \
  usr/localcontroller/command/ShutDownCommand.java \
  usr/localcontroller/command/UnknownCommand.java \
  usr/localcontroller/LocalControllerInfo.java \
  usr/localcontroller/BasicRouterInfo.java \
  usr/localcontroller/LocalController.java \
  usr/localcontroller/LocalControllerManagementConsole.java



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

