package net.luis;

import net.luis.utils.logging.*;
import org.apache.logging.log4j.*;

/**
 *
 * @author Luis-St
 *
 */

public class Main {
	
	private static final Logger LOGGER;
	
	public static void main(String[] args) {
	
	
	
	
	
	}
	
	static {
		System.setProperty("reflection.exceptions.throw", "true");
		LoggingUtils.initialize(LoggerConfiguration.DEFAULT.disableLogging(LoggingType.FILE).addDefaultLogger(LoggingType.CONSOLE, Level.DEBUG));
		LOGGER = LogManager.getLogger(Main.class);
	}
}
