package net.luis;

import net.luis.utils.io.network.IpEndpoint;
import net.luis.utils.io.network.address.IpAddresses;
import net.luis.utils.io.network.address.ipv4.Ipv4Address;
import org.apache.logging.log4j.*;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 *
 * @author Luis-St
 *
 */

public class Main {
	
	public static final Logger LOGGER = LogManager.getLogger(Main.class);
	public static final IpEndpoint DEFAULT_ENDPOINT = new IpEndpoint(Ipv4Address.LOOPBACK, 8080);
	
	void main(String @NonNull [] args) {
		if (args.length == 0) {
			LOGGER.error("No arguments provided. Please specify '--server' or '--client <clientId>'.");
			return;
		}
		
		String mode = args[0];
		if ("--server".equals(mode)) {
			Server.run(DEFAULT_ENDPOINT);
		} else if ("--client".equals(mode)) {
			if (args.length < 2) {
				LOGGER.error("No client id provided. Please specify a client id after '--client'.");
				return;
			}
			
			try {
				UUID clientId = UUID.fromString(args[1]);
				ThreadContext.put("client.id", clientId.toString());
				
				Client.run(clientId, DEFAULT_ENDPOINT);
			} catch (IllegalArgumentException e) {
				LOGGER.error("Invalid client id format. Please provide a valid uuid.");
			}
		} else {
			LOGGER.error("Invalid argument: {}. Please specify '--server' or '--client <clientId>'.", mode);
		}
	}
}
