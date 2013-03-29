if [ $JAVA_HOME ]
then
echo "Starting build...."
javac -classpath ../lib/log4j-1.2.17.jar:../lib/xmlpull-1.1.3.1.jar:../lib/xpp3_min-1.1.4c.jar:../lib/xstream-1.4.4.jar org/umn/distributed/consistent/common/*.java org/umn/distributed/consistent/common/client/*.java org/umn/distributed/consistent/common/client/testfrmwk/*.java org/umn/distributed/consistent/server/*.java org/umn/distributed/consistent/server/coordinator/*.java org/umn/distributed/consistent/server/quorum/*.java org/umn/distributed/consistent/server/ryw/*.java org/umn/distributed/consistent/server/sequential/*.java
else
echo "JAVA_HOME not defined"
fi

