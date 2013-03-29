if [ $JAVA_HOME ]
then
echo "Running tests...."
java -classpath ../lib/log4j-1.2.17.jar:../lib/xmlpull-1.1.3.1.jar:../lib/xpp3_min-1.1.4c.jar:../lib/xstream-1.4.4.jar:. org/umn/distributed/consistent/common/client/testfrmwk/TestClient $1 $2 $3 $4
else
echo "JAVA_HOME not found"
fi

