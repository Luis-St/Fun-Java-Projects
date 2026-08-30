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

import net.luis.utils.function.throwable.ThrowableConsumer;
import net.luis.utils.function.throwable.ThrowableSupplier;
import net.luis.utils.io.network.connection.exception.NetworkConnectionException;
import net.luis.utils.io.network.connection.tcp.TcpClient;
import net.luis.utils.io.network.connection.tcp.TcpConnection;
import org.apache.logging.log4j.*;
import org.jetbrains.annotations.NotNull;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

import static java.nio.charset.StandardCharsets.*;

/**
 *
 * @author Luis-St
 *
 */

public class PQKE {
	
	private static final Logger LOGGER = LogManager.getLogger(PQKE.class);
	
	private static final String KEM_ALG = "ML-KEM-768";
	private static final int TAG_BITS = 128;
	private static final int NONCE_LEN = 12;
	
	private static byte @NotNull [] transcript(byte @NotNull [] publicKey, byte @NotNull [] encapsulation) {
		return ByteBuffer.allocate(publicKey.length + encapsulation.length).put(publicKey).put(encapsulation).array();
	}
	
	private static @NotNull SecretKey derive(@NotNull SecretKey shared, byte @NotNull [] transcript, @NotNull String label) throws GeneralSecurityException {
		return KDF.getInstance("HKDF-SHA256").deriveKey(
			"AES", HKDFParameterSpec.ofExtract().addIKM(shared).addSalt(transcript).thenExpand(label.getBytes(UTF_8), 32)
		);
	}
	
	private static byte @NotNull [] finished(@NotNull SecretKey shared, byte @NotNull [] transcript, @NotNull String label) throws GeneralSecurityException {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(derive(shared, transcript, label + "-finished"));
		return mac.doFinal(transcript);
	}
	
	private static byte @NotNull [] nonce(long seq) {
		return ByteBuffer.allocate(NONCE_LEN).putInt(0).putLong(seq).array();
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
			KeyPair kp = KeyPairGenerator.getInstance(KEM_ALG).generateKeyPair();
			byte[] publicKey = kp.getPublic().getEncoded();
			
			connection.send(PacketRegistry.write(new PacketRegistry.PQKEPublicKeyPacket(publicKey)));
			LOGGER.info(marker, "Sent PQKE public key to client");
			
			connection.context().set("pqke", new PendingHandshake(kp, publicKey));
		} catch (NetworkConnectionException | GeneralSecurityException e) {
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
			SecretKey shared = KEM.getInstance(KEM_ALG)
				.newDecapsulator(pending.keyPair.getPrivate())
				.decapsulate(encapsulation);
			
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
			PublicKey peer = KeyFactory.getInstance(KEM_ALG).generatePublic(new X509EncodedKeySpec(publicKey));
			LOGGER.info(marker, "Received PQKE public key from server");
			
			KEM.Encapsulated enc = KEM.getInstance(KEM_ALG)
				.newEncapsulator(peer)
				.encapsulate();
			byte[] encapsulation = enc.encapsulation();
			
			client.send(PacketRegistry.write(new PacketRegistry.PQKEEncapsulationPacket(encapsulation)));
			
			LOGGER.info(marker, "Established PQKE session with server: {}", client.remoteEndpoint());
			byte[] transcript = transcript(publicKey, encapsulation);
			Session session = new Session(
				packet -> wrap(packet, p -> client.send(PacketRegistry.write(p))),
				() -> wrap(() -> PacketRegistry.readExpected(client.receive(), PacketRegistry.BinaryMessagePacket.class)),
				derive(enc.key(), transcript, "c2s"),
				derive(enc.key(), transcript, "s2c")
			);
			
			try {
				if (!MessageDigest.isEqual(session.receive(), finished(enc.key(), transcript, "s2c"))) {
					throw new SecurityException("Server key confirmation failed");
				}
				session.send(finished(enc.key(), transcript, "c2s"));
				
				LOGGER.info(marker, "PQKE session established and verified with server: {}", client.remoteEndpoint());
				return session;
			} catch (Exception e) {
				LOGGER.error(marker, "Failed to verify PQKE session with server {}: {}", client.remoteEndpoint(), e.getMessage(), e);
				session.close();
				ThreadContext.remove("pqke");
				throw e;
			}
		} catch (NetworkConnectionException | GeneralSecurityException e) {
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
			
			Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
			c.init(Cipher.ENCRYPT_MODE, this.sendKey, new GCMParameterSpec(TAG_BITS, nonce(this.sendSeq)));
			byte[] sealed = c.doFinal(plaintext);
			
			try {
				this.send.accept(new PacketRegistry.BinaryMessagePacket(sealed));
			} catch (Exception e) {
				LOGGER.error("Failed to send encrypted message: {}", e.getMessage(), e);
				this.broken = true;
				throw e;
			}
			this.sendSeq++;
		}
		
		public byte @NotNull [] receive() throws Exception {
			this.checkUsable();
			
			Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
			c.init(Cipher.DECRYPT_MODE, this.recvKey, new GCMParameterSpec(TAG_BITS, nonce(this.recvSeq)));
			
			try {
				byte[] plaintext = c.doFinal(this.receive.get().message());
				this.recvSeq++;
				return plaintext;
			} catch (Exception e) {
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
