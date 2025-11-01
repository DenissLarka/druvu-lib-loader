package com.druvu.lib.loader;

import java.util.ServiceLoader;
import java.util.function.Predicate;

/**
 * Use standard ServiceLoader with predicate choosing the right candidate.
 * Prevents the existence of multiple implementations in a classpath by throwing an exception
 *
 * @author Deniss Larka
 * on 18 Jan 2025
 */
public final class ServiceLoaderExtended {

	private ServiceLoaderExtended() {
	}

	public static <T> T create(Class<T> targetClass) {
		return create(targetClass, (candidate) -> true);
	}

	public static <T> T create(Class<T> targetClass, Predicate<T> candidateChooser) {
		Iterable<T> serviceLoader = ServiceLoader.load(targetClass);
		T goodCandidate = null;
		for (T candidate : serviceLoader) {
			if (!candidateChooser.test(candidate)) {
				continue;
			}
			if (goodCandidate != null) {
				throw new IllegalStateException(String.format("More than one %s found: %n%s%n%s",
						shortName(targetClass),
						shortName(goodCandidate),
						shortName(candidate)));
			}
			goodCandidate = candidate;
		}

		if (goodCandidate == null) {
			throw new TargetClassNotFoundException("Candidate not found: " + targetClass);
		}

		return goodCandidate;
	}

	private static String shortName(Object object) {
		if (object == null) {
			return "NULL";
		}
		if (object instanceof Class type) {
			return type.getSimpleName();
		}
		return shortName(object.getClass());
	}
}
