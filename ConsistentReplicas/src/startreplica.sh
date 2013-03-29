if [ $JAVA_HOME ]
then
echo "Starting server...."
java -classpath ../lib/log4j-1.2.17.jar:. org/umn/distributed/consistent/common/ConsistentReplica $1 $2 $3
else
echo "JAVA_HOME not defined"
fi
