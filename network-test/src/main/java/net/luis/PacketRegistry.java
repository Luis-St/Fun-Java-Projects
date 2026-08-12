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
import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class PacketRegistry {
	
	public interface Packet {}
	
	public interface ClientToServerPacket extends Packet {}
	
	public interface ServerToClientPacket extends Packet {}
	
	public record PQKEInitializationPacket() implements ClientToServerPacket {
		
		public static final Codec<PQKEInitializationPacket> CODEC = Codecs.unit(PQKEInitializationPacket::new);
	}
	
	public record PQKEPublicKeyPacket(byte @NonNull [] publicKey) implements ClientToServerPacket {
		
		public static final Codec<PQKEPublicKeyPacket> CODEC = CodecBuilder.of(
			Codecs.BYTE_ARRAY.length(builder -> builder.equalTo(1206)).fieldOf("publicKey", "key",PQKEPublicKeyPacket::publicKey)
		).create(PQKEPublicKeyPacket::new);
	}
	
	public record PQKEEncapsulationPacket(byte @NonNull [] encapsulation) implements ClientToServerPacket {
		
		public static final Codec<PQKEEncapsulationPacket> CODEC = CodecBuilder.of(
			Codecs.BYTE_ARRAY.length(builder -> builder.equalTo(1088)).fieldOf("encapsulation", "encap", PQKEEncapsulationPacket::encapsulation)
		).create(PQKEEncapsulationPacket::new);
	}
	
	public record ClientConnectPacket(@NonNull UUID clientId) implements ClientToServerPacket {
		
		public static final Codec<ClientConnectPacket> CODEC = CodecBuilder.of(
			Codecs.UUID.version(b -> b.in(List.of(0, 4))).fieldOf("clientId", "id", ClientConnectPacket::clientId)
		).create(ClientConnectPacket::new);
	}
	
	public record HelloClientPacket() implements ServerToClientPacket {
		
		public static final Codec<HelloClientPacket> CODEC = Codecs.unit(HelloClientPacket::new);
	}
	
	public record MessagePacket(@NotNull String message) implements Packet {
		
		public static final Codec<MessagePacket> CODEC = CodecBuilder.of(
			Codecs.STRING.length(LengthConstraint::notEmpty).fieldOf("message", "msg", MessagePacket::message)
		).create(MessagePacket::new);
	}
	
	public record BinaryMessagePacket(byte @NonNull [] message) implements Packet {
		
		public static final Codec<BinaryMessagePacket> CODEC = CodecBuilder.of(
			Codecs.BYTE_ARRAY.length(LengthConstraint::notEmpty).fieldOf("message", "msg", BinaryMessagePacket::message)
		).create(BinaryMessagePacket::new);
	}
	
	public record ClientDisconnectPacket(@NonNull UUID clientId, boolean serverConfirmation) implements ClientToServerPacket {
		
		public static final Codec<ClientDisconnectPacket> CODEC = CodecBuilder.of(
			Codecs.UUID.version(b -> b.in(List.of(0, 4))).fieldOf("clientId", "id", ClientDisconnectPacket::clientId),
			Codecs.BOOLEAN.fieldOf("serverConfirmation", "confirmation", ClientDisconnectPacket::serverConfirmation)
		).create(ClientDisconnectPacket::new);
	}
	
	public record ByeClientPacket() implements ServerToClientPacket {
		
		public static final Codec<ByeClientPacket> CODEC = Codecs.unit(ByeClientPacket::new);
	}
	
	private static final List<Class<? extends Packet>> PACKETS = List.of(
		// PQKE Packets
		PQKEInitializationPacket.class,
		PQKEPublicKeyPacket.class,
		PQKEEncapsulationPacket.class,
		// General Packets
		ClientConnectPacket.class,
		HelloClientPacket.class,
		MessagePacket.class,
		BinaryMessagePacket.class,
		ClientDisconnectPacket.class,
		ByeClientPacket.class
	);
	private static final List<Codec<? extends Packet>> CODECS = List.of(
		// PQKE Packets
		PQKEInitializationPacket.CODEC,
		PQKEPublicKeyPacket.CODEC,
		PQKEEncapsulationPacket.CODEC,
		// General Packets
		ClientConnectPacket.CODEC,
		HelloClientPacket.CODEC,
		MessagePacket.CODEC,
		BinaryMessagePacket.CODEC,
		ClientDisconnectPacket.CODEC,
		ByeClientPacket.CODEC
	);
	
	@SuppressWarnings("unchecked")
	private static <T extends Packet> @NotNull ToonElement encode(@NonNull Codec<T> codec, @NonNull Packet packet) throws EncoderException {
		return codec.encode(ToonTypeProvider.INSTANCE, (T) packet);
	}
	
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
				stream.flush();
				return os.toByteArray();
			}
			
			try {
				ToonElement element = encode(codec, packet);
				
				stream.writeBoolean(false);
				stream.writeUTF(element.toString(ToonConfig.COMPACT));
				stream.flush();
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
	
	@SuppressWarnings("unchecked")
	public static <T extends Packet> @NotNull T readExpected(byte @NonNull [] bytes, @NonNull Class<T> expectedClass) {
		Packet packet = read(bytes);
		if (expectedClass.isInstance(packet)) {
			return (T) packet;
		} else {
			throw new RuntimeException("Unexpected packet type received: " + packet.getClass().getSimpleName() + ", expected: " + expectedClass.getSimpleName());
		}
	}
}
