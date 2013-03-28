package org.umn.distributed.consistent.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Props {
	private static final String DEFAULT_SERVER_THREAD = "10";

	private static final String DEFAULT_MAX_PSEUDO_NW_DELAY = "1000";

	public static String ENCODING;

	public static int WRITER_SERVER_THREADS;
	public static int READER_SERVER_THREADS;

	public static int SERVER_EXTERNAL_PORT;
	public static int EXTERNAL_SERVER_THREADS;

	public static int SERVER_INTERNAL_PORT;
	public static int INTERNAL_SERVER_THREADS;

	public static int COORDINATOR_PORT;
	public static int COORDINATOR_SERVER_THREADS;

	public static long HEARTBEAT_INTERVAL;
	public static int REMOVE_INTERVAL;

	public static int NETWORK_TIMEOUT;

	public static int QUORUM_SYNC_TIME_MILLIS = 5000;

	public static String TEST_ARTICLES_TO_POPULATE;

	public static int percentIncreaseWriteQuorum = 0;

	public static int maxPseudoNetworkDelay;

	// static{
	// loadProperties("config.properties");
	// }
	//
	public static void loadProperties(String propertyFile) {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(propertyFile));
			ENCODING = prop.getProperty("encoding", "UTF8");

			// TODO: validate the port numbers before using

			READER_SERVER_THREADS = Integer.parseInt(prop.getProperty(
					"serverWriterThreads", DEFAULT_SERVER_THREAD));
			WRITER_SERVER_THREADS = Integer.parseInt(prop.getProperty(
					"serverReaderThreads", DEFAULT_SERVER_THREAD));

			SERVER_EXTERNAL_PORT = Integer.parseInt(prop
					.getProperty("serverExternalPort"));
			EXTERNAL_SERVER_THREADS = Integer.parseInt(prop.getProperty(
					"externalServerThreads", DEFAULT_SERVER_THREAD));

			SERVER_INTERNAL_PORT = Integer.parseInt(prop
					.getProperty("serverInternalPort"));
			INTERNAL_SERVER_THREADS = Integer.parseInt(prop.getProperty(
					"internalServerThreads", DEFAULT_SERVER_THREAD));

			COORDINATOR_SERVER_THREADS = Integer.parseInt(prop.getProperty(
					"coordinatorServerThreads", DEFAULT_SERVER_THREAD));
			COORDINATOR_PORT = Integer.parseInt(prop
					.getProperty("coordinatorPort"));

			HEARTBEAT_INTERVAL = Long.parseLong(prop
					.getProperty("heartbeatInterval"));
			REMOVE_INTERVAL = Integer.parseInt(prop
					.getProperty("deregisterInterval"));
			NETWORK_TIMEOUT = Integer.parseInt(prop
					.getProperty("totalNetworkTimeout"));
			QUORUM_SYNC_TIME_MILLIS = Integer.parseInt(prop.getProperty(
					"quorumSyncTimeout", "300000"));
			TEST_ARTICLES_TO_POPULATE = prop
					.getProperty("testArticlesToPublish");
			percentIncreaseWriteQuorum = Integer.parseInt(prop.getProperty(
					"percentIncreaseWrite", "0"));
			if (percentIncreaseWriteQuorum < 0
					|| percentIncreaseWriteQuorum >= 100) {
				percentIncreaseWriteQuorum = 0;
			}
			/**
			 * making this in increments of 10 only
			 */
			percentIncreaseWriteQuorum = (percentIncreaseWriteQuorum / 10) * 10;
			
			maxPseudoNetworkDelay = Integer.parseInt(prop.getProperty(
					"maxPseudoNetworkDelay", DEFAULT_MAX_PSEUDO_NW_DELAY));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
