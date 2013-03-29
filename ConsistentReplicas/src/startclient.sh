if [ $JAVA_HOME ]
then
echo "Running client...."
java -classpath ../lib/log4j-1.2.17.jar:. org/umn/distributed/consistent/common/client/Client $1 $2 $3
else
echo "JAVA_HOME not defined"
fi
