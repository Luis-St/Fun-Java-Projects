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
import net.luis.utils.io.network.connection.tcp.TcpClient;
import net.luis.utils.io.network.connection.tcp.TcpClientConfig;
import org.apache.commons.lang3.ThreadUtils;
import org.apache.logging.log4j.*;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 *
 * @author Luis-St
 *
 */

public class Client {
	
	public static final Logger LOGGER = LogManager.getLogger(Client.class);
	public static final Marker CLIENT = MarkerManager.getMarker("Client");
	private static final boolean CONFIRMATION = true;
	
	static void run(@NonNull UUID clientId, @NonNull IpEndpoint endpoint) {
		TcpClientConfig config = TcpClientConfig.builder().build();
		
		try (TcpClient client = new TcpClient(config)) {
			LOGGER.info(CLIENT, "Starting client...");
			client.connect(endpoint);
			LOGGER.info(CLIENT, "Client connected to {}:{}", endpoint.address(), endpoint.port());
			
			client.send(PacketRegistry.write(new PacketRegistry.ClientConnectPacket(clientId)));
			
			PacketRegistry.Packet response = PacketRegistry.read(client.receive());
			if (response instanceof PacketRegistry.HelloClientPacket hello) {
				LOGGER.info(CLIENT, "Received hello packet from server.");
			} else {
				throw new IllegalStateException("Unexpected packet type received from server: " + response.getClass().getSimpleName());
			}
			
			client.send(PacketRegistry.write(new PacketRegistry.MessagePacket("Hello from client " + clientId + "!")));
			
			ThreadUtils.sleep(Duration.ofMillis(1000));
			
			client.send(PacketRegistry.write(new PacketRegistry.ClientDisconnectPacket(clientId, CONFIRMATION)));
			
			if (CONFIRMATION) {
				response = PacketRegistry.read(client.receive());
				if (response instanceof PacketRegistry.ByeClientPacket bye) {
					LOGGER.info(CLIENT, "Received bye packet from server.");
				} else {
					throw new IllegalStateException("Unexpected packet type received from server: " + response.getClass().getSimpleName());
				}
			}
			
			LOGGER.info(CLIENT, "Disconnecting client...");
			client.close();
			LOGGER.info(CLIENT, "Client disconnected.");
		} catch (Exception e) {
			LOGGER.error(CLIENT, "Client error: {}", e.getMessage(), e);
		}
	}
}
