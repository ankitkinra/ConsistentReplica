if [ $JAVA_HOME ]
then
echo "Running PublisherServer"
java -classpath ../lib/log4j-1.2.17.jar:. org/umn/distributed/consistent/common/client/testfrmwk/TestClient $1 $2 $3 $4
else
echo "JAVA_HOME not found"
fi
