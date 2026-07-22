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
import net.luis.utils.io.network.connection.event.*;
import net.luis.utils.io.network.connection.exception.NetworkErrorType;
import net.luis.utils.io.network.connection.tcp.*;
import org.apache.logging.log4j.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author Luis-St
 *
 */

public class Server {
	
	public static final Logger LOGGER = LogManager.getLogger(Server.class);
	public static final Marker SERVER = MarkerManager.getMarker("Server");
	
	public static void run(@NonNull IpEndpoint endpoint) {
		TcpServerConfig config = TcpServerConfig.builder()
			.onClientConnect(Server::onClientConnect)
			.onClientDisconnect(Server::onClientDisconnect)
			.onMessage(Server::onMessage)
			.onError(Server::onError)
			.build();
		
		CountDownLatch latch = new CountDownLatch(1);
		try (TcpServer server = new TcpServer(endpoint, config)) {
			LOGGER.info(SERVER, "Starting server...");
			server.start();
			LOGGER.info(SERVER, "Server started on {}:{}", endpoint.address(), endpoint.port());
			
			try {
				latch.await();
			} catch (InterruptedException e) {
				LOGGER.error(SERVER, "Server interrupted: {}", e.getMessage());
				Thread.currentThread().interrupt();
				return;
			}
			
			LOGGER.info(SERVER, "Stopping server...");
			server.stop();
			LOGGER.info(SERVER, "Server stopped.");
		} catch (Exception e) {
			LOGGER.error(SERVER, "Server error: {}", e.getMessage());
		}
	}
	
	private static void onClientConnect(@NonNull ConnectEvent event) {
		LOGGER.info(SERVER, "Client connected: {}", event.remoteEndpoint()); // ToDo: Add TcpConnection parameter to onClientConnect method
	}
	
	private static void onClientDisconnect(@NonNull DisconnectEvent event) {
		LOGGER.info(SERVER, "Client disconnected: {}", event.remoteEndpoint());
	}
	
	private static void onMessage(@NonNull TcpServer server, @NonNull TcpConnection connection, byte @NonNull [] data) {
		LOGGER.info(SERVER, "Received message from {}", connection.remoteEndpoint());
		
		try {
			PacketRegistry.Packet packet = PacketRegistry.read(data);
			if (packet instanceof PacketRegistry.ClientConnectPacket(UUID clientId)) {
				LOGGER.info(SERVER, "Received ClientConnectPacket from clientId: {}", clientId);
				
				connection.send(PacketRegistry.write(new PacketRegistry.HelloClientPacket()));
			} else if (packet instanceof PacketRegistry.MessagePacket(String message)) {
				LOGGER.info(SERVER, "Received MessagePacket with message: {}", message);
			} else if (packet instanceof PacketRegistry.ClientDisconnectPacket(UUID clientId, boolean serverConfirmation)) {
				LOGGER.info(SERVER, "Received ClientDisconnectPacket from clientId: {}, serverConfirmation: {}", clientId, serverConfirmation);
				
				if (serverConfirmation) {
					connection.send(PacketRegistry.write(new PacketRegistry.ByeClientPacket()));
				}
			}
		} catch (Exception e) {
			LOGGER.error(SERVER, "Error sending response to {}: {}", connection.remoteEndpoint(), e.getMessage(), e);
		}
	}
	
	private static void onError(@NonNull NetworkErrorType errorType, @NonNull String message, @Nullable Throwable cause) {
		LOGGER.error(SERVER, "Network error [{}]: {}", errorType, message, cause); // ToDo: Add Optional<TcpConnection> parameter to onError method to log connection details if available
	}
}
