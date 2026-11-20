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

/**
 *
 * @author Luis-St
 *
 */

public class PQKE {
	
/*	private static final Logger LOGGER = LogManager.getLogger(PQKE.class);
	
	private static final KemAlgorithm KEM_ALG = KemAlgorithm.ML_KEM_768;
	public static final NativeSignatureAlgorithm SIGNATURE_ALG = SignatureAlgorithm.ML_DSA_65;
	private static final byte[] SIGNATURE_CONTEXT = "PQKE-v1 signature".getBytes(UTF_8);
	
	private static byte @NonNull [] transcript(byte @NonNull [] publicKey, byte @NonNull [] encapsulation) {
		return CryptoBytes.concat(publicKey, encapsulation);
	}
	
	private static @NonNull SecretKey derive(@NonNull Secret shared, byte @NonNull [] transcript, @NonNull String label) {
		return Kdfs.deriveKey(KdfAlgorithm.HKDF_SHA_256, shared.material(), transcript, label.getBytes(UTF_8), AeadAlgorithm.AES_256_GCM);
	}
	
	private static byte @NonNull [] finished(@NonNull Secret shared, byte @NonNull [] transcript, @NonNull String label) {
		byte[] info = (label + "-finished").getBytes(UTF_8);
		try (Secret key = Kdfs.derive(KdfAlgorithm.HKDF_SHA_256, shared.material(), transcript, info, MacAlgorithm.HMAC_SHA_256.recommendedKeyLength())) {
			return Macs.mac(MacAlgorithm.HMAC_SHA_256, key.material(), transcript);
		}
	}
	
	private static byte @NonNull [] sign(@NonNull PrivateKey identity, byte @NonNull [] publicKey) {
		return Signatures.signer(SIGNATURE_ALG, identity).update(SIGNATURE_CONTEXT).update(publicKey).sign();
	}
	
	private static boolean verify(@NonNull PublicKey identity, byte @NonNull [] publicKey, byte @NonNull [] signature) {
		return Signatures.verifier(SIGNATURE_ALG, identity).update(SIGNATURE_CONTEXT).update(publicKey).verify(signature);
	}
	
	public static void beginServerExchange(@NonNull Marker marker, @NonNull TcpConnection connection, @NonNull PrivateKey identity) {
		LOGGER.info(marker, "Initializing PQKE handshake with client...");
		try {
			KeyPair kp = Kems.generateKeyPair(KEM_ALG);
			byte[] publicKey = kp.getPublic().getEncoded();
			
			connection.send(PacketRegistry.write(new PacketRegistry.PQKEPublicKeyPacket(publicKey, sign(identity, publicKey))));
			LOGGER.info(marker, "Sent PQKE public key to client");
			
			connection.context().set("pqke", new PendingHandshake(kp, publicKey));
		} catch (NetworkConnectionException | CryptoException e) {
			LOGGER.error(marker, "Failed to establish PQKE session with client {}: {}", connection.remoteEndpoint(), e.getMessage(), e);
		}
	}
	
	public static @NonNull Session completeServerExchange(@NonNull Marker marker, @NonNull TcpConnection connection, byte @NonNull [] encapsulation) throws Exception {
		if (!connection.context().contains("pqke")) {
			throw new IllegalStateException("PQKE handshake not initialized for this connection");
		}
		
		PendingHandshake pending = connection.context().get("pqke", PendingHandshake.class).orElseThrow();
		try {
			Secret shared = Kems.decapsulate(KEM_ALG, pending.keyPair.getPrivate(), encapsulation);
			
			LOGGER.info(marker, "PQKE session established with client: {}", connection.remoteEndpoint());
			byte[] transcript = transcript(pending.publicKey, encapsulation);
			Session session = new Session(
				marker,
				packet -> connection.send(PacketRegistry.write(packet)),
				() -> PacketRegistry.readExpected(connection.receive(), PacketRegistry.BinaryMessagePacket.class),
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
				throw e;
			}
		} catch (Exception e) {
			LOGGER.error(marker, "Failed to establish PQKE session with client {}: {}", connection.remoteEndpoint(), e.getMessage(), e);
			throw e;
		} finally {
			connection.context().remove("pqke");
		}
	}
	
	public static @NonNull Session exchangeClient(@NonNull Marker marker, @NonNull TcpClient client, @NonNull PublicKey identity) throws Exception {
		LOGGER.info(marker, "Initializing PQKE handshake with server...");
		try {
			client.send(PacketRegistry.write(new PacketRegistry.PQKEInitializationPacket()));
			
			PacketRegistry.PQKEPublicKeyPacket publicKeyPacket = PacketRegistry.readExpected(client.receive(), PacketRegistry.PQKEPublicKeyPacket.class);
			byte[] publicKey = publicKeyPacket.publicKey();
			
			if (!verify(identity, publicKey, publicKeyPacket.signature())) {
				throw new NetworkConnectionException("Signature of public key does not match");
			}
			
			PublicKey peer = CryptoKeys.publicKey(KEM_ALG, publicKey);
			LOGGER.info(marker, "Received PQKE public key from server");
			
			try (Kems.Encapsulation enc = Kems.encapsulate(KEM_ALG, peer)) {
				byte[] encapsulation = enc.encapsulation();
				Secret shared = enc.sharedSecret();
				
				client.send(PacketRegistry.write(new PacketRegistry.PQKEEncapsulationPacket(encapsulation)));
				
				LOGGER.info(marker, "Established PQKE session with server: {}", client.remoteEndpoint());
				byte[] transcript = transcript(publicKey, encapsulation);
				Session session = new Session(
					marker,
					packet -> client.send(PacketRegistry.write(packet)),
					() -> PacketRegistry.readExpected(client.receive(), PacketRegistry.BinaryMessagePacket.class),
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
					throw e;
				}
			}
		} catch (NetworkConnectionException e) {
			LOGGER.error(marker, "Failed to establish PQKE session with server {}: {}", client.remoteEndpoint(), e.getMessage(), e);
			throw e;
		}
	}
	
	public record PendingHandshake(@NonNull KeyPair keyPair, byte @NonNull [] publicKey) {}
	
	public static class Session implements AutoCloseable {
		
		private final Marker marker;
		private final ThrowableConsumer<PacketRegistry.BinaryMessagePacket, Exception> send;
		private final ThrowableSupplier<PacketRegistry.BinaryMessagePacket, Exception> receive;
		private final SecretKey sendKey;
		private final SecretKey recvKey;
		private long sendSeq;
		private long recvSeq;
		private boolean broken;
		
		public Session(
			@NonNull Marker marker,
			@NonNull ThrowableConsumer<PacketRegistry.BinaryMessagePacket, Exception> send,
			@NonNull ThrowableSupplier<PacketRegistry.BinaryMessagePacket, Exception> receive,
			@NonNull SecretKey sendKey,
			@NonNull SecretKey recvKey
		) {
			this.marker = marker;
			this.send = send;
			this.receive = receive;
			this.sendKey = sendKey;
			this.recvKey = recvKey;
		}
		
		private static byte @NonNull [] nonce(long seq) {
			return CryptoBytes.concat(CryptoBytes.of(0), CryptoBytes.of(seq));
		}
		
		private void checkUsable() throws Exception {
			if (this.broken) {
				throw new Exception("Session is broken with the last receive or send operation");
			}
		}
		
		public void send(byte @NonNull [] plaintext) throws Exception {
			this.checkUsable();
			
			byte[] sealed = Aeads.encrypt(AeadAlgorithm.AES_256_GCM, this.sendKey, nonce(this.sendSeq), plaintext, null);
			try {
				this.send.accept(new PacketRegistry.BinaryMessagePacket(sealed));
			} catch (CryptoException e) {
				LOGGER.error("Failed to send encrypted message: {}", e.getMessage(), e);
				this.broken = true;
				throw e;
			}
			this.sendSeq++;
		}
		
		public byte @NonNull [] receive() throws Exception {
			this.checkUsable();
			
			try {
				byte[] plaintext = Aeads.decrypt(AeadAlgorithm.AES_256_GCM, this.recvKey, nonce(this.recvSeq), this.receive.get().message(), null);
				this.recvSeq++;
				return plaintext;
			} catch (CryptoException e) {
				LOGGER.error(this.marker, "Failed to receive encrypted message: {}", e.getMessage(), e);
				this.broken = true;
				throw e;
			}
		}
		
		@Override
		public void close() {
			this.broken = true;
			LOGGER.info(this.marker, "Closed pqke session");
			
			try {
				this.sendKey.destroy();
			} catch (Exception _) {}
			
			try {
				this.recvKey.destroy();
			} catch (Exception _) {}
		}
	}*/
}
