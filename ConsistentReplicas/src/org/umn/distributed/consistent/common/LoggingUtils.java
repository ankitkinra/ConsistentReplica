package org.umn.distributed.consistent.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class LoggingUtils {
	public static final String DEFAULT_DEBUG_PATTERN = "%d{dd-mm-yyyy HH:mm:ss,SSS} %5p %c{1}:%F:%L - %m%n";
	public static final Level DEFAULT_LOG_LEVEL = Level.INFO;
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

	public static void addSpecificFileAppender(Class classInitLogger,
			String logFileDirectory, String uniqueSuffix, Level level) {
		addSpecificFileAppender(classInitLogger, logFileDirectory,
				uniqueSuffix, level, DEFAULT_DEBUG_PATTERN);
	}

	public static void addSpecificFleAppender(Class classInitLogger,
			String logFileDirectory, String uniqueSuffix) {
		addSpecificFileAppender(classInitLogger, logFileDirectory,
				uniqueSuffix, DEFAULT_LOG_LEVEL, DEFAULT_DEBUG_PATTERN);
	}

	public static void addSpecificFileAppender(Class classInitLogger,
			String logFileDirectory, String uniqueSuffix, Level level,
			String pattern) {
		FileAppender appender = new FileAppender();
		appender.setName(classInitLogger.getCanonicalName());
		appender.setLayout(new PatternLayout(pattern));
		appender.setFile(logFileDirectory + classInitLogger.getSimpleName()
				+ "_" + uniqueSuffix + ".log");
		appender.setAppend(true);
		appender.setThreshold(level);
		appender.activateOptions();
		Logger.getRootLogger().addAppender(appender);
	}

	public static void logDebug(Logger log, String format, Object... args) {
		if (log.isDebugEnabled()) {
			log.debug(String.format(format, args));
		}
	}

	public static void logMessage(Level level, Logger log, String format,
			Object... args) {

		if (level.equals(Level.DEBUG)) {
			logDebug(log, format, args);
		} else {
			logInfo(log, format, args);
		}

		// error handled differently
	}

	public static void logInfo(Logger log, String format, Object... args) {
		if (log.isInfoEnabled()) {
			log.info(String.format(format, args));
		}

	}
	

	public static void logError(Logger log, Exception e, String format,
			Object... args) {
		log.error(String.format(format, args), e);
	}
	
	public static void logError(Logger log, Exception e, String message) {
		log.error(message, e);
	}

}
