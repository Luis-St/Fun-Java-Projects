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
