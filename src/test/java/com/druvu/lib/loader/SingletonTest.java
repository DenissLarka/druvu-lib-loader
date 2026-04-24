package com.druvu.lib.loader;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SingletonLoader}.
 * Verifies thread-safety, single initialization semantics, and proper exception handling.
 *
 * @author Deniss Larka
 * on 18 Jan 2025
 */
public class SingletonTest {

	@BeforeMethod
	public void setUp() {
		// Clear the singleton registry using reflection to ensure test isolation
		try {
			var field = SingletonLoader.class.getDeclaredField("SINGLETONS_REGISTER");
			field.setAccessible(true);
			((java.util.Map<?, ?>) field.get(null)).clear();
		} catch (Exception e) {
			throw new RuntimeException("Failed to reset singleton registry", e);
		}
	}

	@Test
	public void testCreateSingletonInstance() {
		// Verify that double initialization throws IllegalStateException
		SingletonLoader.load(MySingleton.class);

		assertThatThrownBy(() -> SingletonLoader.load(MySingleton.class))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Already created");
	}

	@Test
	public void testInstanceBeforeInit_ThrowsException() {
		// Verify that accessing a singleton before initialization throws
		assertThatThrownBy(() -> SingletonLoader.get(MySingleton.class))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Singleton must be loaded before use");
	}

	@Test
	public void testInstanceReturnsSameObject() {
		// Verify that multiple calls to get() return the same object
		MySingleton first = SingletonLoader.load(MySingleton.class);
		MySingleton second = SingletonLoader.get(MySingleton.class);
		MySingleton third = SingletonLoader.get(MySingleton.class);

		assertThat(first).isSameAs(second);
		assertThat(second).isSameAs(third);
	}

	@Test
	public void testConcurrentInitialization_OnlyOneSucceeds() throws InterruptedException {
		// Test that concurrent load() calls result in exactly one initialization
		int threadCount = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch completionLatch = new CountDownLatch(threadCount);

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failureCount = new AtomicInteger(0);
		AtomicReference<MySingleton> createdInstance = new AtomicReference<>();

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					// Wait for signal to start all threads simultaneously
					startLatch.await();

					MySingleton instance = SingletonLoader.load(MySingleton.class);
					successCount.incrementAndGet();
					createdInstance.compareAndSet(null, instance);
				} catch (IllegalStateException e) {
					// Expected for all but one thread
					failureCount.incrementAndGet();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					completionLatch.countDown();
				}
			});
		}

		// Release all threads at once
		startLatch.countDown();

		// Wait for all threads to complete
		assertThat(completionLatch.await(5, TimeUnit.SECONDS)).isTrue();
		executor.shutdown();

		// Verify exactly one thread succeeded
		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failureCount.get()).isEqualTo(threadCount - 1);

		// Verify the instance is accessible and is the same one created
		MySingleton instance = SingletonLoader.get(MySingleton.class);
		assertThat(instance).isSameAs(createdInstance.get());
	}

	@Test
	public void testConcurrentAccess_AllGetSameInstance() throws InterruptedException {
		// Initialize the singleton first
		MySingleton expected = SingletonLoader.load(MySingleton.class);

		// Test that concurrent get() calls all return the same object
		int threadCount = 20;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch completionLatch = new CountDownLatch(threadCount);

		AtomicInteger sameInstanceCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();
					MySingleton instance = SingletonLoader.get(MySingleton.class);
					if (instance == expected) {
						sameInstanceCount.incrementAndGet();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					completionLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		assertThat(completionLatch.await(5, TimeUnit.SECONDS)).isTrue();
		executor.shutdown();

		// All threads should have received the exact same instance
		assertThat(sameInstanceCount.get()).isEqualTo(threadCount);
	}

	@Test
	public void testInitWithDependencies() {
		// Test initialization with dependencies
		Dependencies deps = new Dependencies();
		MySingleton instance = SingletonLoader.load(MySingleton.class, deps);

		assertThat(instance).isNotNull();
		assertThat(SingletonLoader.get(MySingleton.class)).isSameAs(instance);
	}
}
