if [ $JAVA_HOME ]
then
echo "Running Client"
java -classpath ../lib/log4j-1.2.17.jar:. -Djava.rmi.server.codebase=file:. -Djava.security.policy=file:./server.policy Client
else
echo "JAVA_HOME not found"
fi
