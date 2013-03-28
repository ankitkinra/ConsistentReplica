if [ $JAVA_HOME ]
then
echo "Running PublisherServer"
java -classpath ../lib/log4j-1.2.17.jar:. -Djava.rmi.server.codebase=file:. -Djava.security.policy=file:./server.policy -Djava.rmi.server.hostname=$1 PublisherServer $2 $3 $4
else
echo "JAVA_HOME not found"
fi
