package net.luis.pqke;

import net.luis.PacketRegistry;
import net.luis.PacketRequest;
import net.luis.utils.crypto.*;
import net.luis.utils.crypto.algorithm.AeadAlgorithm;
import net.luis.utils.crypto.key.Secret;
import net.luis.utils.function.throwable.ThrowableConsumer;
import net.luis.utils.function.throwable.ThrowableSupplier;
import net.luis.utils.io.network.connection.exception.NetworkConnectionException;
import net.luis.utils.io.network.connection.tcp.TcpClient;
import net.luis.utils.io.network.connection.tcp.TcpConnection;
import net.luis.utils.util.Either;
import org.apache.logging.log4j.*;
import org.jspecify.annotations.NonNull;

import javax.crypto.SecretKey;
import java.security.*;
import java.util.UUID;

/**
 *
 * @author Luis-St
 *
 */

// ToDo: endpoints are optionals in log messages
public final class PQKESession implements AutoCloseable {
	
	private static final Logger LOGGER = LogManager.getLogger(PQKESession.class);
	private static final PacketRequest<PacketRegistry.PQKEPublicKeyPacket, PacketRegistry.PQKEAbortPacket> KEY_REQUEST = PacketRequest.of(
		PacketRegistry.PQKEPublicKeyPacket.class,
		PacketRegistry.PQKEAbortPacket.class
	);
	private static final PacketRequest<PacketRegistry.PQKEEncapsulationPacket, PacketRegistry.PQKEAbortPacket> ENCAPSULATION_REQUEST = PacketRequest.of(
		PacketRegistry.PQKEEncapsulationPacket.class,
		PacketRegistry.PQKEAbortPacket.class
	);
	private static final PacketRequest<PacketRegistry.BinaryMessagePacket, PacketRegistry.PQKEAbortPacket> VERIFICATION_REQUEST = PacketRequest.of(
		PacketRegistry.BinaryMessagePacket.class,
		PacketRegistry.PQKEAbortPacket.class
	);
	
	private final Marker marker;
	private final ThrowableConsumer<PacketRegistry.BinaryMessagePacket, NetworkConnectionException> send;
	private final ThrowableSupplier<PacketRegistry.BinaryMessagePacket, NetworkConnectionException> receive;
	private final SecretKey sendKey;
	private final SecretKey recvKey;
	private long sendSeq;
	private long recvSeq;
	private boolean broken;
	
	private PQKESession(
		@NonNull Marker marker,
		@NonNull ThrowableConsumer<PacketRegistry.BinaryMessagePacket, NetworkConnectionException> send,
		@NonNull ThrowableSupplier<PacketRegistry.BinaryMessagePacket, NetworkConnectionException> receive,
		@NonNull SecretKey sendKey,
		@NonNull SecretKey recvKey
	) {
		this.marker = marker;
		this.send = send;
		this.receive = receive;
		this.sendKey = sendKey;
		this.recvKey = recvKey;
	}
	
	public static @NonNull PQKESession startServerSession(@NonNull Marker marker, @NonNull TcpConnection connection, @NonNull UUID clientId, @NonNull PrivateKey identity) throws PQKEException {
		LOGGER.info(marker, "Initializing PQKE handshake with client {}", clientId);
		
		KeyPair kp = Kems.generateKeyPair(PQKEHelper.KEM_ALG);
		byte[] publicKey = kp.getPublic().getEncoded();
		
		try {
			connection.send(PacketRegistry.write(new PacketRegistry.PQKEPublicKeyPacket(publicKey, PQKEHelper.sign(identity, publicKey))));
			LOGGER.info(marker, "Sent signed PQKE public key to client");
		} catch (NetworkConnectionException e) {
			LOGGER.error(marker, "Failed to send public key to client {}: {}", clientId, e.getMessage(), e);
			
			PQKEException ex = new PQKEException("Failed to send public key to client " + clientId, e);
			try {
				connection.send(PacketRegistry.write(new PacketRegistry.PQKEAbortPacket()));
			} catch (NetworkConnectionException nested) {
				LOGGER.error(marker, "Failed to send PQKE abort to client {}, closing connection: {}", clientId, nested.getMessage(), nested);
				connection.close();
				
				ex.addSuppressed(nested);
			}
			
			throw ex;
		}
		
		PacketRegistry.PQKEEncapsulationPacket encapsulationPacket;
		try {
			Either<PacketRegistry.PQKEEncapsulationPacket, PacketRegistry.PQKEAbortPacket> encapsulationRequest = PacketRegistry.readRequest(connection.receive(), ENCAPSULATION_REQUEST);
			if (encapsulationRequest.isRight()) {
				LOGGER.error(marker, "Client {} aborted PQKE establishment, this might because of an invalid public key signature or an packet loss", clientId);
				throw new PQKEException("Client " + clientId + " aborted PQKE establishment");
			}
			
			encapsulationPacket = encapsulationRequest.leftOrThrow();
		} catch (NetworkConnectionException e) {
			LOGGER.error(marker, "Failed to receive key encapsulation from client {}: {}", clientId, e.getMessage(), e);
			
			PQKEException ex = new PQKEException("Failed to receive key encapsulation from client " + clientId, e);
			try {
				connection.send(PacketRegistry.write(new PacketRegistry.PQKEAbortPacket()));
			} catch (NetworkConnectionException nested) {
				LOGGER.error(marker, "Failed to send PQKE abort to client {}, closing connection: {}", clientId, nested.getMessage(), nested);
				connection.close();
				
				ex.addSuppressed(nested);
			}
			
			throw ex;
		}
		
		byte[] encapsulation = encapsulationPacket.encapsulation();
		byte[] transcript = PQKEHelper.transcript(publicKey, encapsulation);
		try (Secret shared = Kems.decapsulate(PQKEHelper.KEM_ALG, kp.getPrivate(), encapsulation)) {
			LOGGER.info(marker, "Untested PQKE connection established to client {}", clientId);
			LOGGER.info(marker, "Verify PQKE connection to client {}", clientId);
			
			try {
				connection.send(PacketRegistry.write(new PacketRegistry.BinaryMessagePacket(PQKEHelper.finished(shared, transcript, "s2c"))));
			} catch (NetworkConnectionException e) {
				LOGGER.error(marker, "Failed to send verification mac to client {}: {}", clientId, e.getMessage(), e);
				
				PQKEException ex = new PQKEException("Failed to send verification mac to client " + clientId, e);
				try {
					connection.send(PacketRegistry.write(new PacketRegistry.PQKEAbortPacket()));
				} catch (NetworkConnectionException nested) {
					LOGGER.error(marker, "Failed to send PQKE abort to client {}, closing connection: {}", clientId, nested.getMessage(), nested);
					connection.close();
					
					ex.addSuppressed(nested);
				}
				
				throw ex;
			}
			
			PacketRegistry.BinaryMessagePacket verificationPacket;
			try {
				Either<PacketRegistry.BinaryMessagePacket, PacketRegistry.PQKEAbortPacket> verificationRequest = PacketRegistry.readRequest(connection.receive(), VERIFICATION_REQUEST);
				if (verificationRequest.isRight()) {
					LOGGER.error(marker, "PQKE connection verification failed to client {}", clientId);
					throw new PQKEException("PQKE connection verification failed to client " + clientId);
				}
				
				verificationPacket = verificationRequest.leftOrThrow();
			} catch (NetworkConnectionException e) {
				LOGGER.error(marker, "Failed to receive connection verification from client {}: {}", clientId, e.getMessage(), e);
				
				PQKEException ex = new PQKEException("Failed to receive connection verification from client " + clientId, e);
				try {
					connection.send(PacketRegistry.write(new PacketRegistry.PQKEAbortPacket()));
				} catch (NetworkConnectionException nested) {
					LOGGER.error(marker, "Failed to send PQKE abort to client {}, closing connection: {}", clientId, nested.getMessage(), nested);
					connection.close();
					
					ex.addSuppressed(nested);
				}
				
				throw ex;
			}
			
			if (!MessageDigest.isEqual(verificationPacket.message(), PQKEHelper.finished(shared, transcript, "c2s"))) {
				LOGGER.error(marker, "PQKE connection verification failed, there might be a key mismatch");
				
				PQKEException ex = new PQKEException("PQKE connection verification failed, there might be a key mismatch");
				try {
					connection.send(PacketRegistry.write(new PacketRegistry.PQKEAbortPacket()));
				} catch (NetworkConnectionException e) {
					LOGGER.error(marker, "Failed to send PQKE abort to client {}, closing connection: {}", clientId, e.getMessage(), e);
					connection.close();
					
					ex.addSuppressed(e);
				}
				
				throw ex;
			}
			
			return new PQKESession(
				marker,
				packet -> connection.send(PacketRegistry.write(packet)),
				() -> PacketRegistry.readPacket(connection.receive(), PacketRegistry.BinaryMessagePacket.class),
				PQKEHelper.derive(shared, transcript, "s2c"),
				PQKEHelper.derive(shared, transcript, "c2s")
			);
		}
	}
	
	public static @NonNull PQKESession startClientSession(@NonNull Marker marker, @NonNull TcpClient client, @NonNull PublicKey identity) throws PQKEException {
		LOGGER.info(marker, "Initializing PQKE handshake with server");
		
		try {
			client.send(PacketRegistry.write(new PacketRegistry.PQKEInitializationPacket()));
		} catch (NetworkConnectionException e) {
			LOGGER.error("Fail to send PQKE initialization packet to server: {}", e.getMessage(), e);
			throw new PQKEException("Fail to send PQKE initialization packet to server", e);
		}
		
		PacketRegistry.PQKEPublicKeyPacket publicKeyPacket;
		try {
			Either<PacketRegistry.PQKEPublicKeyPacket, PacketRegistry.PQKEAbortPacket> keyRequest = PacketRegistry.readRequest(client.receive(), KEY_REQUEST);
			if (keyRequest.isRight()) {
				LOGGER.error("PQKE handshake failed, server did not respond with a public key");
				throw new PQKEException("PQKE handshake failed, server did not respond with a public key");
			}
			
			publicKeyPacket = keyRequest.leftOrThrow();
		} catch (NetworkConnectionException e) {
			LOGGER.error("Failed to receive public key from server: {}", e.getMessage(), e);
			
			PQKEException ex = new PQKEException("Failed to receive public key from server", e);
			try {
				client.send(PacketRegistry.write(new PacketRegistry.PQKEAbortPacket()));
			} catch (NetworkConnectionException nested) {
				LOGGER.error(marker, "Failed to send PQKE abort to server, closing connection: {}", nested.getMessage(), nested);
				client.close();
				
				ex.addSuppressed(nested);
			}
			
			throw ex;
		}
		
		byte[] publicKey = publicKeyPacket.publicKey();
		if (!PQKEHelper.verify(identity, publicKey, publicKeyPacket.signature())) {
			LOGGER.error(marker, "Failed to verify public key signature received from the server");
			
			PQKEException ex = new PQKEException("PQKE public key signature verification failed");
			try {
				client.send(PacketRegistry.write(new PacketRegistry.PQKEAbortPacket()));
			} catch (NetworkConnectionException e) {
				LOGGER.error(marker, "Failed to send PQKE abort to server, closing connection: {}", e.getMessage(), e);
				client.close();
				
				ex.addSuppressed(e);
			}
			
			throw ex;
		}
		
		PublicKey peer = CryptoKeys.publicKey(PQKEHelper.KEM_ALG, publicKey);
		LOGGER.info(marker, "Received signed PQKE public key from server");
		try (Kems.Encapsulation enc = Kems.encapsulate(PQKEHelper.KEM_ALG, peer)) {
			byte[] encapsulation = enc.encapsulation();
			Secret shared = enc.sharedSecret();
			byte[] transcript = PQKEHelper.transcript(publicKey, encapsulation);
			
			try {
				client.send(PacketRegistry.write(new PacketRegistry.PQKEEncapsulationPacket(encapsulation)));
			} catch (NetworkConnectionException e) {
				LOGGER.error(marker, "Failed to send key encapsulation to server: {}", e.getMessage(), e);
				
				PQKEException ex = new PQKEException("Failed to send key encapsulation to server", e);
				try {
					client.send(PacketRegistry.write(new PacketRegistry.PQKEAbortPacket()));
				} catch (NetworkConnectionException nested) {
					LOGGER.error(marker, "Failed to send PQKE abort to server, closing connection: {}", nested.getMessage(), nested);
					client.close();
					
					ex.addSuppressed(nested);
				}
				
				throw ex;
			}
			
			LOGGER.info(marker, "Untested PQKE connection established to server {}", client.remoteEndpoint());
			LOGGER.info(marker, "Verify PQKE connection to server {}", client.remoteEndpoint());
			
			PacketRegistry.BinaryMessagePacket verificationPacket;
			try {
				Either<PacketRegistry.BinaryMessagePacket, PacketRegistry.PQKEAbortPacket> verificationRequest = PacketRegistry.readRequest(client.receive(), VERIFICATION_REQUEST);
				if (verificationRequest.isRight()) {
					LOGGER.error("PQKE connection verification failed to server {}", client.remoteEndpoint());
					throw new PQKEException("PQKE connection verification failed to server");
				}
				
				verificationPacket = verificationRequest.leftOrThrow();
			} catch (NetworkConnectionException e) {
				LOGGER.error(marker, "Failed to receive connection verification from server: {}", e.getMessage(), e);
				
				PQKEException ex = new PQKEException("Failed to receive connection verification from server", e);
				try {
					client.send(PacketRegistry.write(new PacketRegistry.PQKEAbortPacket()));
				} catch (NetworkConnectionException nested) {
					LOGGER.error(marker, "Failed to send PQKE abort to server, closing connection: {}", nested.getMessage(), nested);
					client.close();
					
					ex.addSuppressed(nested);
				}
				
				throw ex;
			}
			
			if (!MessageDigest.isEqual(verificationPacket.message(), PQKEHelper.finished(shared, transcript, "s2c"))) {
				LOGGER.error(marker, "PQKE connection verification failed, there might be a key mismatch");
				
				PQKEException ex = new PQKEException("PQKE connection verification failed, there might be a key mismatch");
				try {
					client.send(PacketRegistry.write(new PacketRegistry.PQKEAbortPacket()));
				} catch (NetworkConnectionException e) {
					LOGGER.error(marker, "Failed to send PQKE abort to server, closing connection: {}", e.getMessage(), e);
					client.close();
					
					ex.addSuppressed(e);
				}
				
				throw ex;
			}
			
			try {
				client.send(PacketRegistry.write(new PacketRegistry.BinaryMessagePacket(PQKEHelper.finished(shared, transcript, "c2s"))));
			} catch (NetworkConnectionException e) {
				LOGGER.error(marker, "Failed to send verification mac to server: {}", e.getMessage(), e);
				
				PQKEException ex = new PQKEException("Failed to send verification mac to server", e);
				try {
					client.send(PacketRegistry.write(new PacketRegistry.PQKEAbortPacket()));
				} catch (NetworkConnectionException nested) {
					LOGGER.error(marker, "Failed to send PQKE abort to server, closing connection: {}", nested.getMessage(), nested);
					client.close();
					
					ex.addSuppressed(nested);
				}
				
				throw ex;
			}
			
			return new PQKESession(
				marker,
				packet -> client.send(PacketRegistry.write(packet)),
				() -> PacketRegistry.readPacket(client.receive(), PacketRegistry.BinaryMessagePacket.class),
				PQKEHelper.derive(shared, transcript, "c2s"),
				PQKEHelper.derive(shared, transcript, "s2c")
			);
		}
	}
	
	private void checkUsable() throws PQKEException {
		if (this.broken) {
			throw new PQKEException("PQKE session broke with the last receive or send operation");
		}
	}
	
	public void send(byte @NonNull [] plaintext) throws PQKEException {
		this.checkUsable();
		
		try {
			byte[] sealed = Aeads.encrypt(AeadAlgorithm.AES_256_GCM, this.sendKey, PQKEHelper.nonce(this.sendSeq), plaintext, null);
			this.send.accept(new PacketRegistry.BinaryMessagePacket(sealed));
		} catch (Exception e) {
			LOGGER.error("Failed to send encrypted message: {}", e.getMessage(), e);
			this.broken = true;
			
			throw new PQKEException("Failed to send encrypted message", e);
		}
		this.sendSeq++;
	}
	
	public byte @NonNull [] receive() throws PQKEException {
		this.checkUsable();
		
		try {
			byte[] plaintext = Aeads.decrypt(AeadAlgorithm.AES_256_GCM, this.recvKey, PQKEHelper.nonce(this.recvSeq), this.receive.get().message(), null);
			this.recvSeq++;
			return plaintext;
		} catch (Exception e) {
			LOGGER.error(this.marker, "Failed to receive encrypted message: {}", e.getMessage(), e);
			this.broken = true;
			
			throw new PQKEException("Failed to receive encrypted message", e);
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
}
