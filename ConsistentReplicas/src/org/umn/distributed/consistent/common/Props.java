package org.umn.distributed.consistent.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Props {
	public static String ENCODING;
	public static int GETLIST_REQUEST_INTERVAL;
	public static int SERVER_UDP_PORT;
	public static String PING_PORT;
	public static String FREE_PORT_LIST;

	public static void loadProperties() {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("config.properties"));
			ENCODING = prop.getProperty("encoding");
			GETLIST_REQUEST_INTERVAL = Integer.parseInt(prop.getProperty("serverListUpdateInterval"));
			SERVER_UDP_PORT = Integer.parseInt(prop.getProperty("serverUdpPort"));
			PING_PORT = prop.getProperty("serverPingPort");
			FREE_PORT_LIST = prop.getProperty("freePortList");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
