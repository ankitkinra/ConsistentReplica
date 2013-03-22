package org.umn.distributed.consistent.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Props {
	public static String ENCODING;

	public static int SERVER_EXTERNAL_PORT;
	public static int EXTERNAL_SERVER_THREADS;

	public static int SERVER_INTERNAL_PORT;
	public static int INTERNAL_SERVER_THREADS;

	public static int COORDINATOR_PORT;
	public static int COORDINATOR_SERVER_THREADS;

	public static int HEARTBEAT_INTERVAL;
	public static int REMOVE_INTERVAL;

	public static void loadProperties() {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("config.properties"));
			ENCODING = prop.getProperty("encoding");

			// TODO: validate the port numbers before using
			SERVER_EXTERNAL_PORT = Integer.parseInt(prop
					.getProperty("serverExternalPort"));
			EXTERNAL_SERVER_THREADS = Integer.parseInt(prop
					.getProperty("externalServerThreads"));

			SERVER_INTERNAL_PORT = Integer.parseInt(prop
					.getProperty("serverInternalPort"));
			INTERNAL_SERVER_THREADS = Integer.parseInt(prop
					.getProperty("internalServerThreads"));

			COORDINATOR_SERVER_THREADS = Integer.parseInt(prop
					.getProperty("coordinatorServerThreads"));
			COORDINATOR_PORT = Integer.parseInt(prop
					.getProperty("coordinatorPort"));

			HEARTBEAT_INTERVAL = Integer.parseInt(prop
					.getProperty("heartbeatInterval"));
			REMOVE_INTERVAL = Integer.parseInt(prop
					.getProperty("deregisterInterval"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
