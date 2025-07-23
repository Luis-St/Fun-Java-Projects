package net.luis;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.luis.utils.logging.*;
import org.apache.logging.log4j.*;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 *
 * @author Luis-St
 *
 */

public class Main {
	
	private static final Logger LOGGER;
	
	public static void main(String[] args) {
		try (StructuredConcurrency sc = new StructuredConcurrency()) {
			
			Supplier<Integer> result1 = sc.fork(() -> {
				LOGGER.info("Running task 1");
				Thread.sleep(1000);
				LOGGER.info("Task 1 completed");
				return 1;
			});
			Supplier<Integer> result2 = sc.fork(() -> {
				LOGGER.info("Running task 2");
				Thread.sleep(5000);
				LOGGER.info("Task 2 completed");
				throw new RuntimeException("Simulated error in task 2");
			});
			Supplier<Integer> result3 = sc.fork(() -> {
				LOGGER.info("Running task 3");
				Thread.sleep(3000);
				LOGGER.info("Task 3 completed");
				return 3;
			});
			
			LOGGER.info("Waiting for tasks to complete...");
			sc.join();
			LOGGER.info("All tasks completed");
			try {
				LOGGER.info("Result of task 1: {}", result1.get());
				LOGGER.info("Result of task 2: {}", result2.get());
				LOGGER.info("Result of task 3: {}", result3.get());
			} catch (Exception e) {
				LOGGER.error("An error occurred while getting the results", e);
			}
			
		}
	}
	
	static {
		System.setProperty("reflection.exceptions.throw", "true");
		LoggingUtils.initialize(LoggerConfiguration.DEFAULT.disableLogging(LoggingType.FILE).addDefaultLogger(LoggingType.CONSOLE, Level.DEBUG));
		LOGGER = LogManager.getLogger(Main.class);
	}
}
