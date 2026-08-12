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
import org.apache.logging.log4j.*;
import org.jspecify.annotations.NonNull;

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
			
			PacketRegistry.HelloClientPacket helloClientPacket = PacketRegistry.readExpected(client.receive(), PacketRegistry.HelloClientPacket.class);
			LOGGER.info(CLIENT, "Received hello packet from server.");
			
			try (PQKE.Session session = PQKE.exchangeClient(client, CLIENT)) {
				session.send(PacketRegistry.write(new PacketRegistry.MessagePacket("Encrypted hello from client " + clientId + "!")));
				
				PacketRegistry.MessagePacket messageResponse = PacketRegistry.readExpected(session.receive(), PacketRegistry.MessagePacket.class);
				LOGGER.info(CLIENT, "Received message from server: {}", messageResponse.message());
			}
			
			client.send(PacketRegistry.write(new PacketRegistry.MessagePacket("Unencrypted hello from client " + clientId + "!")));
			
			PacketRegistry.MessagePacket messageResponse = PacketRegistry.readExpected(client.receive(), PacketRegistry.MessagePacket.class);
			LOGGER.info(CLIENT, "Received message from server: {}", messageResponse.message());
			
			client.send(PacketRegistry.write(new PacketRegistry.ClientDisconnectPacket(clientId, CONFIRMATION)));
			if (CONFIRMATION) {
				PacketRegistry.ByeClientPacket byeClientPacket = PacketRegistry.readExpected(client.receive(), PacketRegistry.ByeClientPacket.class);
				LOGGER.info(CLIENT, "Received bye packet from server.");
			}
			
			LOGGER.info(CLIENT, "Disconnecting client...");
			client.close();
			LOGGER.info(CLIENT, "Client disconnected.");
		} catch (Exception e) {
			LOGGER.error(CLIENT, "Client error: {}", e.getMessage(), e);
		}
	}
}
