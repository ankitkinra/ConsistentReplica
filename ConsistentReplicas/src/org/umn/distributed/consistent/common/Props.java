package org.umn.distributed.consistent.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Props {
	public static String ENCODING;
	public static int SERVER_EXTERNAL_PORT;
	public static int SERVER_INTERNAL_PORT;
	public static int INTERNAL_SERVER_THREADS;
	public static int EXTERNAL_SERVER_THREADS;
	public static int COORDINATOR_SERVER_THREADS; 
	
	public static void loadProperties() {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("config.properties"));
			ENCODING = prop.getProperty("encoding");
			//TODO: validate the port numbers before using
			SERVER_EXTERNAL_PORT = Integer.parseInt(prop.getProperty("serverExternalPort"));
			SERVER_INTERNAL_PORT = Integer.parseInt(prop.getProperty("serverInternalPort"));
			INTERNAL_SERVER_THREADS = Integer.parseInt(prop.getProperty("internalServerThreads"));
			EXTERNAL_SERVER_THREADS = Integer.parseInt(prop.getProperty("externalServerThreads"));
			COORDINATOR_SERVER_THREADS = Integer.parseInt(prop.getProperty("coordinatorServerThreads"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
