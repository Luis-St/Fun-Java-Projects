/*
 * LUtils
 * Copyright (C) 2025 Luis Staudt
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

import net.luis.utils.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 *
 * @author Luis-St
 *
 */

@SuppressWarnings({ "ProhibitedExceptionThrown" })
public class StructuredConcurrency implements AutoCloseable {
	
	private final ExecutorService executor;
	private final Phaser phaser = new Phaser(1);
	private final AtomicInteger failureCount = new AtomicInteger(0);
	private boolean join;
	
	public StructuredConcurrency() {
		this(ForkJoinPool.commonPool());
	}
	
	public StructuredConcurrency(@NotNull ExecutorService executor) {
		this.executor = Objects.requireNonNull(executor, "ExecutorService must not be null");
		if (this.isShutdown()) {
			throw new IllegalStateException("Executor service is already shutdown");
		}
	}
	
	public boolean isShutdown() {
		return this.executor.isShutdown() || this.executor.isTerminated();
	}
	
	public @NotNull Future<?> fork(@NotNull Runnable action) {
		Objects.requireNonNull(action, "Runnable action must not be null");
		if (this.isShutdown()) {
			throw new IllegalStateException("Executor service is already shutdown");
		}
		if (this.join) {
			throw new IllegalStateException("Cannot submit tasks to a structured concurrency instance that joins the tasks");
		}
		
		this.phaser.register();
		
		return this.executor.submit(() -> {
			try {
				action.run();
			} catch (Throwable t) {
				this.failureCount.incrementAndGet();
				throw t;
			} finally {
				this.phaser.arriveAndDeregister();
			}
		});
	}
	
	public <T> @NotNull Future<T> fork(@NotNull Callable<T> action) {
		Objects.requireNonNull(action, "Callable action must not be null");
		if (this.isShutdown()) {
			throw new IllegalStateException("Executor service is already shutdown");
		}
		if (this.join) {
			throw new IllegalStateException("Cannot submit tasks to a structured concurrency instance that joins the tasks");
		}
		
		this.phaser.register();
		
		return this.executor.submit(() -> {
			try {
				return action.call();
			} catch (Throwable t) {
				this.failureCount.incrementAndGet();
				throw t;
			} finally {
				this.phaser.arriveAndDeregister();
			}
		});
	}
	
	public void join() {
		this.join(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}
	
	public void join(long timeout, @NotNull TimeUnit unit) {
		this.join = true;
		try {
			this.phaser.awaitAdvanceInterruptibly(this.phaser.arriveAndDeregister(), timeout, unit);
		} catch (InterruptedException | TimeoutException e) {
			throw new RuntimeException(e);
		}
		this.join = false;
	}
	
	@Override
	public void close() {
		if (this.isShutdown()) {
			return;
		}
		
		if (this.executor != ForkJoinPool.commonPool()) {
			try {
				if (!this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
					this.executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Executor was interrupted while waiting for termination", e);
			}
		}
	}
}
