package net.luis;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 *
 * @author Luis-St
 *
 */

public record PacketRequest<P extends PacketRegistry.Packet, E extends PacketRegistry.Packet>(
	@NonNull Class<P> packetClass,
	@NonNull Optional<Class<E>> errorPacket
) {
	
	public static <P extends PacketRegistry.Packet> @NonNull PacketRequest<P, ? extends PacketRegistry.Packet> of(@NonNull Class<P> packetClass) {
		return new PacketRequest<>(packetClass, Optional.empty());
	}
	
	public static <P extends PacketRegistry.Packet, E extends PacketRegistry.Packet> @NonNull PacketRequest<P, E> of(@NonNull Class<P> packetClass, @NonNull Class<E> errorPacket) {
		return new PacketRequest<>(packetClass, Optional.of(errorPacket));
	}
	
	public boolean hasError() {
		return this.errorPacket.isPresent();
	}
	
	@Override
	public @NotNull String toString() {
		if (this.hasError()) {
			return this.packetClass.getName() + " or " + this.errorPacket.orElseThrow().getName();
		}
		return this.packetClass.getName();
	}
}
