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
  usr/common/LocalHostInfo.java \
  usr/controllers/EventScheduler.java \
  usr/controllers/GlobalController.java \
  usr/controllers/GlobalControllerManagementConsole.java \
  usr/controllers/SimEvent.java \
  usr/controllers/ControlOptions.java \
  usr/controllers/LocalController.java \
  usr/controllers/LocalControllerManagementConsole.java \
  usr/controllers/localcommand/QuitCommand.java \
  usr/controllers/localcommand/UnknownCommand.java \
  usr/controllers/localcommand/AbstractCommand.java \
  usr/controllers/localcommand/LocalCheckCommand.java \
  usr/controllers/localcommand/ShutDownCommand.java \
  usr/controllers/localcommand/LocalCommand.java \
  usr/controllers/globalcommand/QuitCommand.java \
  usr/controllers/globalcommand/AbstractCommand.java \
  usr/controllers/globalcommand/GlobalCommand.java \
  usr/controllers/globalcommand/UnknownCommand.java \
  usr/controllers/globalcommand/LocalOKCommand.java \
  usr/controllers/LocalControllerInfo.java \
  usr/controllers/XMLNoTagException.java \
  usr/test/RouterTest1.java \
  usr/test/StubServer.java \
  usr/test/StubClient.java \
  usr/router/command/QuitCommand.java \
  usr/router/command/AbstractCommand.java \
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
  usr/router/RouterManagementConsole.java \
  usr/router/Router.java \
  usr/router/SimpleRouterFabric.java \
  usr/router/CreateConnection.java \
  usr/router/RouterConnections.java \
  usr/router/RouterController.java \
  usr/router/NetIF.java \
  usr/router/RouterPort.java \
  usr/router/RouterFabric.java \
  usr/router/TCPNetIF.java \
  usr/net/Datagram.java \
  usr/net/DatagramConnection.java \
  usr/net/DatagramPatch.java \
  usr/net/DatagramQueueingConnection.java \
  usr/net/IPV4Datagram.java \
  usr/net/Address.java \
  usr/net/IPV4Address.java \
  usr/interactor/ChannelResponder.java \
  usr/interactor/Command.java \
  usr/interactor/MCRP.java \
  usr/interactor/ManagementConsole.java \
  usr/interactor/MCRPException.java \
  usr/interactor/MCRPResponse.java \
  usr/interactor/MCRPEvent.java \
  usr/interactor/MCRPNotReadyException.java \
  usr/interactor/MCRPEventListener.java \
  usr/interactor/ID.java \
  usr/interactor/MCRPInteractor.java \
  usr/interactor/RouterInteractor.java \
  usr/interactor/GlobalControllerInteractor.java \
  usr/interactor/LocalControllerInteractor.java \
  usr/interactor/InputHandler.java \
  usr/interactor/Request.java \
  usr/interactor/MCRPNoConnectionException.java



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

