if [ $JAVA_HOME ]
then
echo "Starting build..."
javac -classpath ../lib/log4j-1.2.17.jar org/umn/distributed/consistent/common/*.java org/umn/distributed/consistent/common/client/*.java org/umn/distributed/consistent/server/*.java org/umn/distributed/consistent/server/coordinator/*.java org/umn/distributed/consistent/server/quorum/*.java org/umn/distributed/consistent/server/ryw/*.java org/umn/distributed/consistent/server/sequential/*.java 
echo "Build Successful"
else
echo "JAVA_HOME not found"
fi
