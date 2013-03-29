package org.umn.distributed.consistent.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ClientProps {
	private static final String DEFAULT_MAX_PSEUDO_NW_DELAY = "1000";

	public static String ENCODING;

	public static int NETWORK_TIMEOUT;

	public static int QUORUM_SYNC_TIME_MILLIS = 5000;
	public static int maxPseudoNetworkDelay;
	public static String logFilePath;

	public static void loadProperties(String propertyFile) {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(propertyFile));
			ENCODING = prop.getProperty("encoding", "UTF8");
			NETWORK_TIMEOUT = Integer.parseInt(prop
					.getProperty("totalNetworkTimeout"));
			QUORUM_SYNC_TIME_MILLIS = Integer.parseInt(prop.getProperty(
					"quorumSyncTimeout", "300000"));

			maxPseudoNetworkDelay = Integer.parseInt(prop.getProperty(
					"maxPseudoNetworkDelay", DEFAULT_MAX_PSEUDO_NW_DELAY));
			logFilePath = prop.getProperty("logFilePath");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
