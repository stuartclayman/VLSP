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
	usr/controllers/GlobalController.java \
	usr/controllers/LocalController.java \
	usr/controllers/ControlOptions.java \
	usr/controllers/LocalHostInfo.java \
	usr/controllers/LocalControllerListener.java \
	usr/controllers/GlobalControllerListener.java \
	usr/controllers/GlobalSocketController.java \
	usr/controllers/PayLoad.java \
	usr/controllers/SimEvent.java \
	usr/controllers/EventScheduler.java \
	usr/router/ManagementConsole.java \
	usr/router/Router.java \
	usr/router/QuitCommand.java \
	usr/router/SimpleRouterFabric.java \
	usr/router/AbstractCommand.java \
	usr/router/CreateConnection.java \
	usr/router/RouterConnections.java \
  	usr/router/RouterController.java \
	usr/router/NetIF.java \
	usr/router/RouterPort.java \
	usr/router/RouterFabric.java \
	usr/router/Request.java \
	usr/router/TCPNetIF.java \
	usr/router/Command.java \
	usr/net/Datagram.java \
	usr/net/DatagramConnection.java \
	usr/net/DatagramPatch.java \
	usr/net/DatagramQueueingConnection.java \
	usr/net/IPV4Datagram.java \
	usr/net/Address.java \
	usr/net/IPV4Address.java \
	usr/test/RouterTest1.java \
	usr/test/StubClient.java \
	usr/test/StubServer.java


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
	$(RM) *.class

