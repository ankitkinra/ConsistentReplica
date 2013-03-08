package org.umn.distributed.consistent.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class LoggingUtils {
	private static Logger logger;
	static {
		final String LOG_FILE = "log4j.properties";
		Properties logProp = new Properties();
		
		try {
			logProp.load(new FileInputStream(LOG_FILE));
			PropertyConfigurator.configure(logProp);
			logger = Logger.getLogger(LoggingUtils.class);
			logger.info("Logging enabled");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Logging not enabled");
		}
	}

	public static void testLog(String string) {
		logger.info(string);
		
	}
}
