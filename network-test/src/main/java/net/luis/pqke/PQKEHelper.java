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

package net.luis.pqke;

import net.luis.utils.crypto.*;
import net.luis.utils.crypto.algorithm.*;
import net.luis.utils.crypto.key.Secret;
import net.luis.utils.crypto.util.CryptoBytes;
import org.jspecify.annotations.NonNull;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

import static java.nio.charset.StandardCharsets.*;

/**
 *
 * @author Luis-St
 *
 */

class PQKEHelper {
	
	static final KemAlgorithm KEM_ALG = KemAlgorithm.ML_KEM_768;
	private static final NativeSignatureAlgorithm SIGNATURE_ALG = SignatureAlgorithm.ML_DSA_65;
	private static final byte[] SIGNATURE_CONTEXT = "PQKE-v1 signature".getBytes(UTF_8);
	
	static byte @NonNull [] transcript(byte @NonNull [] publicKey, byte @NonNull [] encapsulation) {
		return CryptoBytes.concat(publicKey, encapsulation);
	}
	
	static @NonNull SecretKey derive(@NonNull Secret shared, byte @NonNull [] transcript, @NonNull String label) {
		return Kdfs.deriveKey(KdfAlgorithm.HKDF_SHA_256, shared.material(), transcript, label.getBytes(UTF_8), AeadAlgorithm.AES_256_GCM);
	}
	
	static byte @NonNull [] finished(@NonNull Secret shared, byte @NonNull [] transcript, @NonNull String label) {
		byte[] info = (label + "-finished").getBytes(UTF_8);
		try (Secret key = Kdfs.derive(KdfAlgorithm.HKDF_SHA_256, shared.material(), transcript, info, MacAlgorithm.HMAC_SHA_256.recommendedKeyLength())) {
			return Macs.mac(MacAlgorithm.HMAC_SHA_256, key.material(), transcript);
		}
	}
	
	static byte @NonNull [] sign(@NonNull PrivateKey identity, byte @NonNull [] publicKey) {
		return Signatures.signer(SIGNATURE_ALG, identity).update(SIGNATURE_CONTEXT).update(publicKey).sign();
	}
	
	static boolean verify(@NonNull PublicKey identity, byte @NonNull [] publicKey, byte @NonNull [] signature) {
		return Signatures.verifier(SIGNATURE_ALG, identity).update(SIGNATURE_CONTEXT).update(publicKey).verify(signature);
	}
	
	static byte @NonNull [] nonce(long seq) {
		return CryptoBytes.concat(CryptoBytes.of(0), CryptoBytes.of(seq));
	}
}
