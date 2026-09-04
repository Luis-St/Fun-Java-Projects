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

import net.luis.utils.crypto.*;
import net.luis.utils.crypto.algorithm.*;
import net.luis.utils.crypto.exception.CryptoException;
import net.luis.utils.crypto.key.Secret;
import net.luis.utils.crypto.util.CryptoBytes;
import net.luis.utils.function.throwable.ThrowableConsumer;
import net.luis.utils.function.throwable.ThrowableSupplier;
import net.luis.utils.io.network.connection.exception.NetworkConnectionException;
import net.luis.utils.io.network.connection.tcp.TcpClient;
import net.luis.utils.io.network.connection.tcp.TcpConnection;
import org.apache.logging.log4j.*;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import javax.crypto.SecretKey;
import java.security.*;

import static java.nio.charset.StandardCharsets.*;

/**
 *
 * @author Luis-St
 *
 */

public class PQKE {
	
	private static final Logger LOGGER = LogManager.getLogger(PQKE.class);
	
	private static final KemAlgorithm KEM_ALG = KemAlgorithm.ML_KEM_768;
	private static final int TAG_BITS = 128;
	private static final int NONCE_LEN = 12;
	
	private static byte @NotNull [] transcript(byte @NotNull [] publicKey, byte @NotNull [] encapsulation) {
		return CryptoBytes.concat(publicKey, encapsulation);
	}
	
	private static @NonNull SecretKey derive(@NonNull Secret shared, byte @NonNull [] transcript, @NonNull String label) {
		return Kdf.deriveKey(KdfAlgorithm.HKDF_SHA_256, shared.material(), transcript, label.getBytes(UTF_8), AeadAlgorithm.AES_256_GCM);
	}
	
	private static byte @NonNull [] finished(@NonNull Secret shared, byte @NonNull [] transcript, @NonNull String label) {
		byte[] info = (label + "-finished").getBytes(UTF_8);
		try (Secret key = Kdf.derive(KdfAlgorithm.HKDF_SHA_256, shared.material(), transcript, info, MacAlgorithm.HMAC_SHA_256.recommendedKeyLength())) {
			return Macs.mac(MacAlgorithm.HMAC_SHA_256, key.material(), transcript);
		}
	}
	
	private static byte @NotNull [] nonce(long seq) {
		return CryptoBytes.concat(CryptoBytes.of(0), CryptoBytes.of(seq));
	}
	
	private static void wrap(PacketRegistry.@NotNull BinaryMessagePacket packet, @NotNull ThrowableConsumer<PacketRegistry.BinaryMessagePacket, NetworkConnectionException> supplier) throws NetworkConnectionException {
		try {
			ThreadContext.put("pqke", "PQKE-Session");
			supplier.accept(packet);
		} catch (Exception e) {
			throw e;
		} finally {
			ThreadContext.remove("pqke");
		}
	}
	
	private static <T> @NotNull T wrap(@NotNull ThrowableSupplier<T, NetworkConnectionException> supplier) throws NetworkConnectionException {
		try {
			ThreadContext.put("pqke", "PQKE-Session");
			return supplier.get();
		} catch (Exception e) {
			throw e;
		} finally {
			ThreadContext.remove("pqke");
		}
	}
	
	public static void beginServerExchange(@NotNull TcpConnection connection, @NotNull Marker marker) {
		ThreadContext.put("pqke", "PQKE-Session");
		
		LOGGER.info(marker, "Initializing PQKE handshake with client...");
		try {
			KeyPair kp = Kems.generateKeyPair(KEM_ALG);
			byte[] publicKey = kp.getPublic().getEncoded();
			
			connection.send(PacketRegistry.write(new PacketRegistry.PQKEPublicKeyPacket(publicKey)));
			LOGGER.info(marker, "Sent PQKE public key to client");
			
			connection.context().set("pqke", new PendingHandshake(kp, publicKey));
		} catch (NetworkConnectionException | CryptoException e) {
			LOGGER.error(marker, "Failed to establish PQKE session with client {}: {}", connection.remoteEndpoint(), e.getMessage(), e);
		} finally {
			ThreadContext.remove("pqke");
		}
	}
	
	public static @NotNull Session completeServerExchange(@NotNull TcpConnection connection, byte @NotNull [] encapsulation, @NotNull Marker marker) throws Exception {
		if (!connection.context().contains("pqke")) {
			throw new IllegalStateException("PQKE handshake not initialized for this connection");
		}
		ThreadContext.put("pqke", "PQKE-Session");
		
		PendingHandshake pending = connection.context().get("pqke", PendingHandshake.class).orElseThrow();
		try {
			Secret shared = Kems.decapsulate(KEM_ALG, pending.keyPair.getPrivate(), encapsulation);
			
			LOGGER.info(marker, "PQKE session established with client: {}", connection.remoteEndpoint());
			byte[] transcript = transcript(pending.publicKey, encapsulation);
			Session session = new Session(
				packet -> wrap(packet, p -> connection.send(PacketRegistry.write(p))),
				() -> wrap(() -> PacketRegistry.readExpected(connection.receive(), PacketRegistry.BinaryMessagePacket.class)),
				derive(shared, transcript, "s2c"),
				derive(shared, transcript, "c2s")
			);
			
			try {
				session.send(finished(shared, transcript, "s2c"));
				if (!MessageDigest.isEqual(session.receive(), finished(shared, transcript, "c2s"))) {
					throw new SecurityException("Client key confirmation failed");
				}
				
				LOGGER.info(marker, "PQKE session established and verified with client: {}", connection.remoteEndpoint());
				return session;
			} catch (Exception e) {
				LOGGER.error(marker, "Failed to establish PQKE session with client {}: {}", connection.remoteEndpoint(), e.getMessage(), e);
				session.close();
				ThreadContext.remove("pqke");
				throw e;
			}
		} catch (Exception e) {
			LOGGER.error(marker, "Failed to establish PQKE session with client {}: {}", connection.remoteEndpoint(), e.getMessage(), e);
			ThreadContext.remove("pqke");
			throw e;
		} finally {
			connection.context().remove("pqke");
		}
	}
	
	public static @NotNull Session exchangeClient(@NotNull TcpClient client, @NotNull Marker marker) throws Exception {
		ThreadContext.put("pqke", "PQKE-Session");
		
		LOGGER.info(marker, "Initializing PQKE handshake with server...");
		try {
			client.send(PacketRegistry.write(new PacketRegistry.PQKEInitializationPacket()));
			
			PacketRegistry.PQKEPublicKeyPacket publicKeyPacket = PacketRegistry.readExpected(client.receive(), PacketRegistry.PQKEPublicKeyPacket.class);
			byte[] publicKey = publicKeyPacket.publicKey();
			
			PublicKey peer = CryptoKeys.publicKey(KEM_ALG, publicKey);
			LOGGER.info(marker, "Received PQKE public key from server");
			
			try (Kems.Encapsulation enc = Kems.encapsulate(KEM_ALG, peer)) {
				byte[] encapsulation = enc.encapsulation();
				Secret shared = enc.sharedSecret();
				
				client.send(PacketRegistry.write(new PacketRegistry.PQKEEncapsulationPacket(encapsulation)));
				
				LOGGER.info(marker, "Established PQKE session with server: {}", client.remoteEndpoint());
				byte[] transcript = transcript(publicKey, encapsulation);
				Session session = new Session(
					packet -> wrap(packet, p -> client.send(PacketRegistry.write(p))),
					() -> wrap(() -> PacketRegistry.readExpected(client.receive(), PacketRegistry.BinaryMessagePacket.class)),
					derive(shared, transcript, "c2s"),
					derive(shared, transcript, "s2c")
				);
				
				try {
					if (!MessageDigest.isEqual(session.receive(), finished(shared, transcript, "s2c"))) {
						throw new SecurityException("Server key confirmation failed");
					}
					session.send(finished(shared, transcript, "c2s"));
					
					LOGGER.info(marker, "PQKE session established and verified with server: {}", client.remoteEndpoint());
					return session;
				} catch (Exception e) {
					LOGGER.error(marker, "Failed to verify PQKE session with server {}: {}", client.remoteEndpoint(), e.getMessage(), e);
					session.close();
					ThreadContext.remove("pqke");
					throw e;
				}
			}
		} catch (NetworkConnectionException e) {
			LOGGER.error(marker, "Failed to establish PQKE session with server {}: {}", client.remoteEndpoint(), e.getMessage(), e);
			ThreadContext.remove("pqke");
			throw e;
		}
	}
	
	public record PendingHandshake(@NotNull KeyPair keyPair, byte @NotNull [] publicKey) {}
	
	public static class Session implements AutoCloseable {
		
		private final ThrowableConsumer<PacketRegistry.BinaryMessagePacket, Exception> send;
		private final ThrowableSupplier<PacketRegistry.BinaryMessagePacket, Exception> receive;
		private final SecretKey sendKey;
		private final SecretKey recvKey;
		private long sendSeq;
		private long recvSeq;
		private boolean broken;
		
		public Session(
			@NotNull ThrowableConsumer<PacketRegistry.BinaryMessagePacket, Exception> send,
			@NotNull ThrowableSupplier<PacketRegistry.BinaryMessagePacket, Exception> receive,
			@NotNull SecretKey sendKey,
			@NotNull SecretKey recvKey
		) {
			this.send = send;
			this.receive = receive;
			this.sendKey = sendKey;
			this.recvKey = recvKey;
		}
		
		private void checkUsable() throws Exception {
			if (this.broken) {
				throw new Exception("Session is broken with the last receive or send operation");
			}
		}
		
		public void send(byte @NotNull [] plaintext) throws Exception {
			this.checkUsable();
			
			byte[] sealed = Aead.encrypt(AeadAlgorithm.AES_256_GCM, this.sendKey, nonce(this.sendSeq), plaintext, null);
			try {
				this.send.accept(new PacketRegistry.BinaryMessagePacket(sealed));
			} catch (CryptoException e) {
				LOGGER.error("Failed to send encrypted message: {}", e.getMessage(), e);
				this.broken = true;
				throw e;
			}
			this.sendSeq++;
		}
		
		public byte @NotNull [] receive() throws Exception {
			this.checkUsable();
			
			try {
				byte[] plaintext = Aead.decrypt(AeadAlgorithm.AES_256_GCM, this.recvKey, nonce(this.recvSeq), this.receive.get().message(), null);
				this.recvSeq++;
				return plaintext;
			} catch (CryptoException e) {
				LOGGER.error("Failed to receive encrypted message: {}", e.getMessage(), e);
				this.broken = true;
				throw e;
			}
		}
		
		@Override
		public void close() {
			this.broken = true;
			ThreadContext.remove("pqke");
			
			try {
				this.sendKey.destroy();
			} catch (Exception _) {}
			
			try {
				this.recvKey.destroy();
			} catch (Exception _) {}
		}
	}
}
