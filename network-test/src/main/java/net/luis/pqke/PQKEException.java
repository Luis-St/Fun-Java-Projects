package net.luis.pqke;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 *
 * @author Luis-St
 *
 */

public class PQKEException extends IOException {
	
	public PQKEException(@Nullable String message) {
		super(message);
	}
	
	public PQKEException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
