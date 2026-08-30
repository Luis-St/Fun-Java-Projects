package net.luis;

import io.javalin.Javalin;
import org.apache.logging.log4j.*;
import org.jspecify.annotations.NonNull;

/**
 *
 * @author Luis-St
 *
 */

public class Main {
	
	public static final Logger LOGGER = LogManager.getLogger(Main.class);
	public static final int DEFAULT_PORT = 3000;
	
	void main(String @NonNull [] args) {
		Javalin app = Javalin.create(config -> {
			config.routes.get("/", context -> context.result("memory-db"));
		});
		app.start(DEFAULT_PORT);
		
		LOGGER.info("Memory database listening on port {}", DEFAULT_PORT);
	}
}
