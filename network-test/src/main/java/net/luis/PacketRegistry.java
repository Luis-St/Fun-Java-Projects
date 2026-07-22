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

import net.luis.utils.io.codec.*;
import net.luis.utils.io.codec.constraint.core.LengthConstraint;
import net.luis.utils.io.codec.encoder.EncoderException;
import net.luis.utils.io.codec.provider.ToonTypeProvider;
import net.luis.utils.io.codec.types.struct.UnitCodec;
import net.luis.utils.io.data.toon.*;
import net.luis.utils.io.data.toon.exception.ToonSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.*;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Luis-St
 *
 */

public class PacketRegistry {
	
	public interface Packet {}
	
	record ClientConnectPacket(@NonNull UUID clientId) implements Packet {
		
		public static final Codec<ClientConnectPacket> CODEC = CodecBuilder.of(
			Codecs.UUID.version4().fieldOf("clientId", "id", ClientConnectPacket::clientId)
		).create(ClientConnectPacket::new);
	}
	
	record HelloClientPacket() implements Packet {
		
		public static final Codec<HelloClientPacket> CODEC = Codecs.unit(HelloClientPacket::new);
	}
	
	record MessagePacket(@NotNull String message) implements Packet {
		
		public static final Codec<MessagePacket> CODEC = CodecBuilder.of(
			Codecs.STRING.length(LengthConstraint::notEmpty).fieldOf("message", "msg", MessagePacket::message)
		).create(MessagePacket::new);
	}
	
	record ClientDisconnectPacket(@NonNull UUID clientId, boolean serverConfirmation) implements Packet {
		
		public static final Codec<ClientDisconnectPacket> CODEC = CodecBuilder.of(
			Codecs.UUID.version4().fieldOf("clientId", "id", ClientDisconnectPacket::clientId),
			Codecs.BOOLEAN.fieldOf("serverConfirmation", "confirmation", ClientDisconnectPacket::serverConfirmation)
		).create(ClientDisconnectPacket::new);
	}
	
	record ByeClientPacket() implements Packet {
		
		public static final Codec<ByeClientPacket> CODEC = Codecs.unit(ByeClientPacket::new);
	}
	
	private static final List<Class<? extends Packet>> PACKETS = List.of(
		ClientConnectPacket.class,
		HelloClientPacket.class,
		MessagePacket.class,
		ClientDisconnectPacket.class,
		ByeClientPacket.class
	);
	private static final List<Codec<? extends Packet>> CODECS = List.of(
		ClientConnectPacket.CODEC,
		HelloClientPacket.CODEC,
		MessagePacket.CODEC,
		ClientDisconnectPacket.CODEC,
		ByeClientPacket.CODEC
	);
	
	@SuppressWarnings("unchecked")
	private static <T extends Packet> @NotNull ToonElement encode(@NonNull Codec<T> codec, @NonNull Packet packet) throws EncoderException {
		return codec.encode(ToonTypeProvider.INSTANCE, (T) packet);
	}
	
	// Issue: Packages with no content fail when reading toon data
	
	public static byte @NonNull [] write(@NonNull Packet packet) {
		int packetIndex = PACKETS.indexOf(packet.getClass());
		if (packetIndex == -1) {
			throw new RuntimeException("Packet class not registered: " + packet.getClass().getName());
		}
		
		Codec<? extends Packet> codec = CODECS.get(packetIndex);
		if (codec == null) {
			throw new RuntimeException("Codec not registered for packet class: " + packet.getClass().getName());
		}
		
		try (ByteArrayOutputStream os = new ByteArrayOutputStream(); ObjectOutputStream stream = new ObjectOutputStream(os)) {
			stream.writeInt(packetIndex);
			
			if (codec instanceof UnitCodec<? extends Packet>) {
				stream.writeBoolean(true);
				return os.toByteArray();
			}
			
			try {
				ToonElement element = encode(codec, packet);
				
				stream.writeBoolean(false);
				stream.writeUTF(element.toString(ToonConfig.COMPACT));
				return os.toByteArray();
			} catch (EncoderException e) {
				throw new RuntimeException("Failed to encode packet: " + packet.getClass().getName(), e);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to write packet data", e);
		}
	}
	
	public static @NonNull Packet read(byte @NonNull [] bytes) {
		try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			int packetIndex = stream.readInt();
			if (packetIndex < 0 || packetIndex >= PACKETS.size()) {
				throw new RuntimeException("Invalid packet index: " + packetIndex);
			}
			
			Codec<? extends Packet> codec = CODECS.get(packetIndex);
			if (codec == null) {
				throw new RuntimeException("Codec not find registered for packet index: " + packetIndex);
			}
			
			if (stream.readBoolean()) {
				return codec.decode(ToonTypeProvider.INSTANCE, ToonNull.INSTANCE);
			}
			
			String value = stream.readUTF();
			try (ToonReader reader = new ToonReader(value, ToonConfig.COMPACT)) {
				ToonElement element = reader.readToon();
				return codec.decode(ToonTypeProvider.INSTANCE, element);
			} catch (ToonSyntaxException e) {
				throw new RuntimeException("Failed to read packet data: " + value, e);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read packet data", e);
		}
	}
}
