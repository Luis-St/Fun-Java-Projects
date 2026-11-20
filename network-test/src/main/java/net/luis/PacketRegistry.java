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
import net.luis.utils.io.codec.provider.BinaryTypeProvider;
import net.luis.utils.io.data.InputProvider;
import net.luis.utils.io.data.OutputProvider;
import net.luis.utils.io.data.binary.*;
import net.luis.utils.io.data.binary.exception.BinarySyntaxException;
import net.luis.utils.util.Either;
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
	
	private static final List<Class<? extends Packet>> PACKETS = List.of(
		// PQKE Packets
		PQKEInitializationPacket.class,
		PQKEPublicKeyPacket.class,
		PQKEEncapsulationPacket.class,
		PQKEAbortPacket.class,
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
		PQKEAbortPacket.CODEC,
		// General Packets
		ClientConnectPacket.CODEC,
		HelloClientPacket.CODEC,
		MessagePacket.CODEC,
		BinaryMessagePacket.CODEC,
		ClientDisconnectPacket.CODEC,
		ByeClientPacket.CODEC
	);
	
	@SuppressWarnings("unchecked")
	private static <T extends Packet> @NonNull BinaryElement encode(@NonNull Codec<T> codec, @NonNull Packet packet) throws EncoderException {
		return codec.encode(BinaryTypeProvider.COMPACT, (T) packet);
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
		
		try (ByteArrayOutputStream os = new ByteArrayOutputStream(); DataOutputStream stream = new DataOutputStream(os)) {
			stream.writeInt(packetIndex);
			
			BinaryElement element;
			try {
				element = encode(codec, packet);
			} catch (EncoderException e) {
				throw new RuntimeException("Failed to encode packet: " + packet.getClass().getName(), e);
			}
			
			try (BinaryWriter writer = new BinaryWriter(new OutputProvider(stream), BinaryConfig.COMPRESSED)) {
				writer.writeBinary(element);
			} catch (Exception e) {
				throw new RuntimeException("Failed to write packet data to stream", e);
			}
			stream.flush();
			return os.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to write packet data", e);
		}
	}
	
	public static @NonNull Packet read(byte @NonNull [] bytes) {
		try (DataInputStream stream = new DataInputStream(new ByteArrayInputStream(bytes))) {
			int packetIndex = stream.readInt();
			if (packetIndex < 0 || packetIndex >= PACKETS.size()) {
				throw new RuntimeException("Invalid packet index: " + packetIndex);
			}
			
			Codec<? extends Packet> codec = CODECS.get(packetIndex);
			if (codec == null) {
				throw new RuntimeException("Codec not find registered for packet index: " + packetIndex);
			}
			
			try (BinaryReader reader = new BinaryReader(new InputProvider(stream), BinaryConfig.COMPRESSED)) {
				BinaryElement element = reader.readBinary();
				return codec.decode(BinaryTypeProvider.COMPACT, element);
			} catch (BinarySyntaxException e) {
				throw new RuntimeException("Failed to read packet data from stream", e);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read packet data", e);
		}
	}
	
	public static <P extends Packet> @NonNull P readPacket(byte @NonNull [] bytes, @NonNull Class<P> packetClass) {
		return readRequest(bytes, PacketRequest.of(packetClass)).leftOrThrow();
	}
	
	@SuppressWarnings("unchecked")
	public static <P extends Packet, E extends Packet> @NonNull Either<P, E> readRequest(byte @NonNull [] bytes, @NonNull PacketRequest<P, E> request) {
		Packet packet = read(bytes);
		if (request.packetClass().isInstance(packet)) {
			return Either.left((P) packet);
		} else if (request.hasError() && request.errorPacket().orElseThrow().isInstance(packet)) {
			return Either.right((E) packet);
		} else {
			throw new IllegalStateException("Unexpected packet type received: " + packet.getClass().getSimpleName() + ", expected: " + request);
		}
	}
	
	public interface Packet {}
	
	public interface ClientToServerPacket extends Packet {}
	
	public interface ServerToClientPacket extends Packet {}
	
	public record PQKEInitializationPacket() implements ClientToServerPacket {
		
		public static final Codec<PQKEInitializationPacket> CODEC = Codecs.unit(PQKEInitializationPacket::new);
	}
	
	public record PQKEPublicKeyPacket(byte @NonNull [] publicKey, byte @NonNull [] signature) implements ServerToClientPacket {
		
		public static final Codec<PQKEPublicKeyPacket> CODEC = CodecBuilder.of(
			Codecs.BYTE_ARRAY.length(builder -> builder.equalTo(1206)).fieldOf("publicKey", "key", PQKEPublicKeyPacket::publicKey),
			Codecs.BYTE_ARRAY.length(builder -> builder.equalTo(3309)).fieldOf("signature", "sig", PQKEPublicKeyPacket::signature)
		).create(PQKEPublicKeyPacket::new);
	}
	
	public record PQKEEncapsulationPacket(byte @NonNull [] encapsulation) implements ClientToServerPacket {
		
		public static final Codec<PQKEEncapsulationPacket> CODEC = CodecBuilder.of(
			Codecs.BYTE_ARRAY.length(builder -> builder.equalTo(1088)).fieldOf("encapsulation", "encap", PQKEEncapsulationPacket::encapsulation)
		).create(PQKEEncapsulationPacket::new);
	}
	
	public record PQKEAbortPacket() implements Packet {
		
		public static final Codec<PQKEAbortPacket> CODEC = Codecs.unit(PQKEAbortPacket::new);
	}
	
	public record ClientConnectPacket(@NonNull UUID clientId) implements ClientToServerPacket {
		
		public static final Codec<ClientConnectPacket> CODEC = CodecBuilder.of(
			Codecs.UUID.version(b -> b.in(List.of(0, 4))).fieldOf("clientId", "id", ClientConnectPacket::clientId)
		).create(ClientConnectPacket::new);
	}
	
	public record HelloClientPacket() implements ServerToClientPacket {
		
		public static final Codec<HelloClientPacket> CODEC = Codecs.unit(HelloClientPacket::new);
	}
	
	public record MessagePacket(@NonNull String message) implements Packet {
		
		public static final Codec<MessagePacket> CODEC = CodecBuilder.of(
			Codecs.STRING.length(LengthConstraint::notEmpty).fieldOf("message", "msg", MessagePacket::message)
		).create(MessagePacket::new);
	}
	
	public record BinaryMessagePacket(byte @NonNull [] message) implements Packet {
		
		public static final Codec<BinaryMessagePacket> CODEC = CodecBuilder.of(
			Codecs.BYTE_ARRAY.length(LengthConstraint::notEmpty).fieldOf("message", "msg", BinaryMessagePacket::message)
		).create(BinaryMessagePacket::new);
	}
	
	public record ClientDisconnectPacket(boolean serverConfirmation) implements ClientToServerPacket {
		
		public static final Codec<ClientDisconnectPacket> CODEC = CodecBuilder.of(
			Codecs.BOOLEAN.fieldOf("serverConfirmation", "confirmation", ClientDisconnectPacket::serverConfirmation)
		).create(ClientDisconnectPacket::new);
	}
	
	public record ByeClientPacket() implements ServerToClientPacket {
		
		public static final Codec<ByeClientPacket> CODEC = Codecs.unit(ByeClientPacket::new);
	}
}
