if [ $JAVA_HOME ]
then
echo "Starting Server"
java -classpath ../lib/log4j-1.2.17.jar:. -Djava.security.policy=file:./server.policy ConsistentReplica $1 $2 $3
else
echo "JAVA_HOME not found"
fi
