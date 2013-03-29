Project 2:
CSCI 5105: Ashutosh Dhiman (dhima005) & Ankit Kinra (kinra003)

Building:
Navigate to $ConsistentReplicas/src/ directory and execute: ./bulid.sh. Make sure $JAVA_HOME is set.

Run:
In order to run you need to first start the coordinator node and then start other replicas.

Coordinator
./startreplica.sh coordinator [s|q|r] <config file path>

eg.
For sequential
./startreplica.sh coordinator s server.properties
For quorum
./startreplica.sh coordinator q server.properties
For read-your-write
./startreplica.sh coordinator r server.properties

	Once the server starts you can see the information for about port on which its running by:
	showinfo

Replica Servers
./startreplica.sh [s|q|r] <Coordinator Ip> <Coordinator Port> <config file path>

eg.
For sequential
./startreplica.sh s localhost 2002 server.properties
For quorum
./startreplica.sh q localhost 2002 server.properties
For read-your-write
./startreplica.sh r localhost 2002 server.properties

Once the server starts you can see the information for about port on which its running by:
	showinfo

Client
Start:
./startclient.sh <coordinatorIp> <coordinatorPort> <config file path>
	Post Article (notice the quotes), replica id is optional:
post "<post title>" "<post content>" [<replica id>]

Reply (notice the quotes), replica id is optional:
reply <post id to reply> "<post title>" "<post content>" [<replica id>]
Read Article list List, replica id is optional: 
readlist [<replica id>]

Once it shows the article list use n, p and q to navigate to next, privious page. To go back 
to the previous menu press q

Read Article details, replica id is optional:
read <post id> [<replica id>]

Exit Client:
stop

Config files:
config.properties:
encoding: encoding type in which data is send over UDP. Default UTF-8 is used
serverWriterThreads: number of server writer threads. Default is 5
serverReaderThreads: number of server reader threads. Default is 5
serverExternalPort: external port where client can connect: Default is 2000. If this port is not 
        free server will find a the next free port.
externalServerThreads: number of server threads that will handle the connection from clients. 
   Default is 5
serverInternalPort: internal port where replicas can connect. Default is 2001.If this port is not 
        free server will find a the next free port.
internalServerThreads: number of thread for internal server operations: Default is 5
coordinatorPort: Coordinator port. Default is 2002.If this port is not free server will find a the next
   free port.
coordinatorServerThreads: Number of threads in coordinator. Default is 5
heartbeatInterval: Heardbeat internal in millliseconds. Default is 20000
totalNetworkTimeout: Total network timeout for an operation to be timed out. Default is 100000
percentIncreaseWrite: Percentage of servers to be added to servers in the Write Quorum. Default is 50%
logFilePath: Path of the log file. Default is log4j.properties

log4j.properties
properties file for logging. Contains location of the files where it generates the logs. In order to change it. Change the following two lines:
log4j.appender.fileDebug.File=logs/debug.log
log4j.appender.fileInfo.File=logs/info.log

In case you want to change the log level change the following
log4j.rootLogger=debug, stdoutDebug, fileDebug
to
log4j.rootLogger= info , stdout, fileInfo


