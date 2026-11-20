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
import net.luis.utils.crypto.algorithm.NativeSignatureAlgorithm;
import net.luis.utils.crypto.algorithm.SignatureAlgorithm;
import net.luis.utils.resources.ResourceLocation;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.security.*;

/**
 *
 * @author Luis-St
 *
 */

public final class Shared {
	
	private static final String PEM_LOCATION = "src/main/resources";
	private static final NativeSignatureAlgorithm SIGNATURE_ALG = SignatureAlgorithm.ML_DSA_65;
	private static final ResourceLocation PUBLIC_KEY_PATH = ResourceLocation.internal("network-test", "public_key.pem");
	private static final ResourceLocation PRIVATE_KEY_PATH = ResourceLocation.internal("network-test", "private_key.pem");
	
	
	public static void generateSignature() {
		KeyPair keyPair = Signatures.generateKeyPair(SIGNATURE_ALG);
		
		Pems.write(Path.of(PEM_LOCATION, PUBLIC_KEY_PATH.toString()), keyPair.getPublic());
		Pems.write(Path.of(PEM_LOCATION, PRIVATE_KEY_PATH.toString()), keyPair.getPrivate());
	}
	
	public static @NonNull PublicKey loadPublicKey() {
		return CryptoKeys.publicKey(SIGNATURE_ALG, Pems.read(PUBLIC_KEY_PATH).content());
	}
	
	public static @NonNull PrivateKey loadPrivateKey() {
		return CryptoKeys.privateKey(SIGNATURE_ALG, Pems.read(PRIVATE_KEY_PATH).content());
	}
}
