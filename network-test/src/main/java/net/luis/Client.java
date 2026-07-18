/*
 * Fun-Java-Projects
 * Copyright (C) 2026 Luis Staudt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.luis;

import net.luis.utils.io.network.IpEndpoint;
import net.luis.utils.io.network.connection.tcp.*;
import org.apache.logging.log4j.*;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author Luis-St
 *
 */

public class Client {
	
	public static final Logger LOGGER = LogManager.getLogger(Client.class);
	public static final Marker CLIENT = MarkerManager.getMarker("Client");
	
	// ToDo: Add simple packet layer with packet id -> codec
	
	static void run(@NonNull UUID clientId, @NonNull IpEndpoint endpoint) {
		TcpClientConfig config = TcpClientConfig.builder().build();
		
		try (TcpClient client = new TcpClient(config)) {
			LOGGER.info(CLIENT, "Starting client...");
			client.connect(endpoint);
			LOGGER.info(CLIENT, "Client connected to {}:{}", endpoint.address(), endpoint.port());
			
			client.send(("Hello from client " + clientId + "!").getBytes(StandardCharsets.UTF_8));
			byte[] response = client.receive();
			if (response.length == 0) {
				LOGGER.warn(CLIENT, "Connection unexpectedly closed by server.");
			}
			LOGGER.info(CLIENT, "Received response from server: {}", new String(response, StandardCharsets.UTF_8));
			
			client.send("close".getBytes(StandardCharsets.UTF_8));
			if (client.receive().length == 0) {
				LOGGER.warn(CLIENT, "Connection closed by server.");
			} else {
				LOGGER.warn(CLIENT, "Unexpected response from server after sending 'close' command.");
			}
			
			LOGGER.info(CLIENT, "Disconnecting client...");
			client.close();
			LOGGER.info(CLIENT, "Client disconnected.");
		} catch (Exception e) {
			LOGGER.error(CLIENT, "Client error: {}", e.getMessage());
		}
	}
}
