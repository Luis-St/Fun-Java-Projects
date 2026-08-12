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
import java.security.spec.NamedParameterSpec;
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
	
	public static @NotNull Session exchangeServer(@NotNull TcpConnection connection, @NotNull Marker marker) throws Exception {
		ThreadContext.put("pqke", "PQKE-Session");
		
		LOGGER.info(marker, "Initializing PQKE handshake with client...");
		try {
			KeyPair kp = KeyPairGenerator.getInstance(KEM_ALG).generateKeyPair();
			byte[] publicKey = kp.getPublic().getEncoded();
			connection.send(PacketRegistry.write(new PacketRegistry.PQKEPublicKeyPacket(publicKey)));
			LOGGER.info(marker, "Sent PQKE public key to client");
			
			PacketRegistry.PQKEEncapsulationPacket encapsulationPacket = PacketRegistry.readExpected(connection.receive(), PacketRegistry.PQKEEncapsulationPacket.class);
			byte[] encapsulation = encapsulationPacket.encapsulation();
			
			SecretKey shared = KEM.getInstance(KEM_ALG)
				.newDecapsulator(kp.getPrivate())
				.decapsulate(encapsulation);
			
			LOGGER.info(marker, "PQKE session established with client: {}", connection.remoteEndpoint());
			byte[] transcript = transcript(publicKey, encapsulation);
			Session session = new Session(
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
		} catch (NetworkConnectionException | GeneralSecurityException e) {
			LOGGER.error(marker, "Failed to establish PQKE session with client {}: {}", connection.remoteEndpoint(), e.getMessage(), e);
			ThreadContext.remove("pqke");
			throw e;
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
				packet -> client.send(PacketRegistry.write(packet)),
				() -> PacketRegistry.readExpected(client.receive(), PacketRegistry.BinaryMessagePacket.class),
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
				throw e;
			}
		} catch (NetworkConnectionException | GeneralSecurityException e) {
			LOGGER.error(marker, "Failed to establish PQKE session with server {}: {}", client.remoteEndpoint(), e.getMessage(), e);
			ThreadContext.remove("pqke");
			throw e;
		}
	}
	
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
	
	/*public static class KemSession implements AutoCloseable {
		
		private static final int MAX_FRAME = 1 << 20;
		private static final int MAX_BLOB = 1 << 16;
		
		private final Socket socket;
		private final DataInputStream in;
		private final DataOutputStream out;
		private final SecretKey sendKey;
		private final SecretKey recvKey;
		
		private long sendSeq;
		private long recvSeq;
		private boolean broken;
		
		private KemSession(Socket socket, DataInputStream in, DataOutputStream out, SecretKey sendKey, SecretKey recvKey) {
			this.socket = socket;
			this.in = in;
			this.out = out;
			this.sendKey = sendKey;
			this.recvKey = recvKey;
		}
		
		// ---------- handshake ----------
		
		public static KemSession accept(Socket socket, KeyPair kp) throws IOException, GeneralSecurityException {
			
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			
			byte[] ek = kp.getPublic().getEncoded();
			writeBlob(out, ek);
			
			byte[] ct = readBlob(in);
			SecretKey shared = KEM.getInstance(KEM_ALG)
				.newDecapsulator(kp.getPrivate())
				.decapsulate(ct);
			
			return new KemSession(socket, in, out, derive(shared, ek, ct, "s2c"), derive(shared, ek, ct, "c2s"));
		}
		
		public static KemSession connect(Socket socket) throws IOException, GeneralSecurityException {
			
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			
			byte[] ek = readBlob(in);
			PublicKey peer = KeyFactory.getInstance(KEM_ALG).generatePublic(new X509EncodedKeySpec(ek));
			
			KEM.Encapsulated enc = KEM.getInstance(KEM_ALG)
				.newEncapsulator(peer)
				.encapsulate();
			
			byte[] ct = enc.encapsulation();
			writeBlob(out, ct);
			
			return new KemSession(socket, in, out, derive(enc.key(), ek, ct, "c2s"), derive(enc.key(), ek, ct, "s2c"));
		}
		
		// ---------- record layer ----------
		
		public void send(byte[] plaintext) throws IOException, GeneralSecurityException {
			this.checkUsable();
			Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
			c.init(Cipher.ENCRYPT_MODE, this.sendKey, new GCMParameterSpec(TAG_BITS, nonce(this.sendSeq)));
			byte[] sealed = c.doFinal(plaintext);
			this.sendSeq++;
			
			this.out.writeInt(sealed.length);
			this.out.write(sealed);
			this.out.flush();
		}
		
		public void send(String text) throws IOException, GeneralSecurityException {
			this.send(text.getBytes(UTF_8));
		}
		
		public byte[] receive() throws IOException, GeneralSecurityException {
			this.checkUsable();
			int len = this.in.readInt();
			if (len < TAG_BYTES || len > MAX_FRAME) {
				this.broken = true;
				throw new IOException("invalid frame length: " + len);
			}
			byte[] sealed = new byte[len];
			this.in.readFully(sealed);
			
			Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
			c.init(Cipher.DECRYPT_MODE, this.recvKey, new GCMParameterSpec(TAG_BITS, nonce(this.recvSeq)));
			try {
				byte[] plaintext = c.doFinal(sealed);
				this.recvSeq++;
				return plaintext;
			} catch (AEADBadTagException e) {
				this.broken = true;
				throw e;
			}
		}
		
		public String receiveString() throws IOException, GeneralSecurityException {
			return new String(this.receive(), UTF_8);
		}
		
		@Override
		public void close() throws IOException {
			this.socket.close();
		}
		
		// ---------- internals ----------
		
		private void checkUsable() throws IOException {
			if (this.broken) {
				throw new IOException("session is broken");
			}
			if (this.socket.isClosed()) {
				throw new IOException("session is closed");
			}
		}
		
		private static byte[] nonce(long seq) {
			return ByteBuffer.allocate(NONCE_LEN).putInt(0).putLong(seq).array();
		}
		
		private static SecretKey derive(SecretKey shared, byte[] ek, byte[] ct, String label) throws GeneralSecurityException {
			byte[] transcript = ByteBuffer.allocate(ek.length + ct.length).put(ek).put(ct).array();
			
			return KDF.getInstance("HKDF-SHA256").deriveKey("AES",
				HKDFParameterSpec.ofExtract()
					.addIKM(shared)
					.addSalt(transcript)
					.thenExpand(label.getBytes(UTF_8), 32));
		}
		
		private static void writeBlob(DataOutputStream out, byte[] b) throws IOException {
			out.writeInt(b.length);
			out.write(b);
			out.flush();
		}
		
		private static byte[] readBlob(DataInputStream in) throws IOException {
			int n = in.readInt();
			if (n < 0 || n > MAX_BLOB) throw new IOException("invalid blob length: " + n);
			byte[] b = new byte[n];
			in.readFully(b);
			return b;
		}
	}
	
	public static final class KemServer {
		
		public static void main(String[] args) throws Exception {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-KEM");
			kpg.initialize(NamedParameterSpec.ML_KEM_768);
			KeyPair kp = kpg.generateKeyPair();
			
			try (ServerSocket server = new ServerSocket(9443)) {
				while (true) {
					Socket client = server.accept();
					Thread.ofVirtual().start(() -> handle(client, kp));
				}
			}
		}
		
		private static void handle(Socket socket, KeyPair kp) {
			try (KemSession session = KemSession.accept(socket, kp)) {
				while (true) {
					String request;
					try {
						request = session.receiveString();
					} catch (EOFException e) {
						return;
					}
					session.send("echo: " + request);
				}
			} catch (Exception e) {
				System.err.println("session ended: " + e);
			}
		}
	}
	
	public static final class KemClient {
		
		public static void main(String[] args) throws Exception {
			try (Socket socket = new Socket("localhost", 9443);
			     KemSession session = KemSession.connect(socket)) {
				
				session.send("hello");
				System.out.println(session.receiveString());
				
				session.send("second message");
				System.out.println(session.receiveString());
			}
		}
	}*/
}
