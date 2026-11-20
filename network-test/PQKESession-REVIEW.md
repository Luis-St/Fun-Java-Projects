# PQKESession Review

Review of `src/main/java/net/luis/pqke/PQKESession.java` together with `PQKEHelper` and the packet layer it relies on.
LUtils `11.0.0-beta.3` was checked from its sources jar so the findings rest on the actual library behaviour.

## Summary

The cryptographic design holds up.
The server signs its ephemeral ML-KEM-768 key with ML-DSA-65, both sides confirm the key with HMAC finished messages,
and each direction gets its own AES-256-GCM key with a 12-byte counter nonce.
The problems are in error handling, thread safety and key cleanup, not in the protocol itself.

## Findings

Ordered by severity.

1. Concurrent `send` can reuse a GCM nonce, lines 349 to 362.
   `sendSeq` is read in `send` and incremented after the network write, without any synchronisation.
   Two threads sending at the same time encrypt under the same key and nonce,
   which with GCM leaks the XOR of both plaintexts and allows forging the authentication tag.
   `send` and `receive` should be `synchronized` on separate locks,
   or the class should be documented as single-threaded, because a silent nonce reuse is the one failure GCM does not survive.
2. Unchecked exceptions escape the handshake without an abort and without closing, lines 96, 121, 145, 206, 246, 248 and 276.
   Only `NetworkConnectionException` is caught.
   `PacketRegistry.readRequest` throws `IllegalStateException` for an unexpected packet type and `RuntimeException` for a decode failure,
   for example a public key that is not exactly 1206 bytes.
   `Kems.decapsulate`, `CryptoKeys.publicKey` and `Kems.encapsulate` throw `CryptoException`, which extends `RuntimeException`.
   In all these cases the peer is left waiting and the caller receives no `PQKEException`,
   so the handshake should catch `RuntimeException` as well and route it through the same abort path.
3. Destroying the session keys always fails silently, lines 384 to 390.
   `PQKEHelper.derive` returns a `SecretKeySpec`, and `SecretKeySpec.destroy()` throws `DestroyFailedException`.
   This was verified with `jshell` on JDK 25.
   The empty catch blocks hide that both AES keys stay in memory after `close`.
   Keeping the raw key material as a `Secret` and building the `SecretKeySpec` per call would make the wipe real,
   otherwise the catch should at least log at debug level so the gap is visible.
4. The ephemeral ML-KEM private key is never cleared, line 72.
   `kp.getPrivate()` lives until garbage collection, which weakens forward secrecy if the process memory is later exposed.
   It should be destroyed right after `decapsulate`, as far as the provider allows.
5. An abort that is sent successfully leaves the connection open, for example lines 82 to 89.
   The connection is only closed when sending the abort fails.
   After a failed handshake the stream is in an undefined state, and on the server the next `onMessage` call may interpret leftover handshake bytes as a new packet.
   Closing the connection after every abort is simpler and safer.
6. Sending an abort right after a failed send is mostly pointless, lines 78 to 92, 127 to 141, 199 to 202, 255 to 269 and 317 to 331.
   When `send` just failed, a second `send` on the same connection almost always fails too.
   Closing directly is enough there.
7. The handshake has no timeout.
   Every `receive()` call blocks until the peer sends something.
   A client that sends `PQKEInitializationPacket` and then stays silent blocks the server thread that runs `onMessage` indefinitely,
   unless `TcpConnection` enforces a read timeout, which is not visible from this code.
8. Session end is not authenticated.
   `close` only flips `broken` locally and sends nothing, so the peer cannot tell a clean end from a cut connection.
   An attacker on the path can drop the trailing messages undetected.
   An encrypted close message with its own sequence number closes that gap, and it matters as soon as the session carries more than one request and response.
9. The client identity is not bound into the session.
   `clientId` comes from the unauthenticated `ClientConnectPacket` and is only used for logging.
   This is fine as long as the id is never used for authorisation, but it should not start being trusted because the channel is encrypted.

## Smaller Points

- The transcript is `publicKey || encapsulation` without length prefixes.
  This is only unambiguous because the codecs enforce fixed lengths of 1206 and 1088 bytes,
  so a comment in `PQKEHelper.transcript` should record that dependency.
- Several log calls lack the marker, at lines 200, 208, 214, 278 and 356.
  The `ToDo` at line 29 points at the same inconsistency.
- Line 98 reads "this might because of an invalid public key signature or an packet loss" and should read "this might be because of an invalid public key signature or a packet loss".
- Line 200 and 201 say "Fail to send", which should be "Failed to send".
- The abort and close block is repeated twelve times.
  A private helper taking the send action, the close action and the `PQKEException` would cut the class by roughly a third,
  and it would make finding 5 a one-line fix.
