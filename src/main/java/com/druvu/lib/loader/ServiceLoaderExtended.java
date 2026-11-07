package com.druvu.lib.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * The standard ServiceLoader with a predicate choosing the right candidates, and the cardinality rules of this library:
 * {@link #load} wants exactly one match and throws on a second, {@link #loadAll} takes every match.
 *
 * @author Deniss Larka on 18 Jan 2025
 */
final class ServiceLoaderExtended {

    private ServiceLoaderExtended() {}

    static <T> T load(Class<T> targetClass) {
        return load(targetClass, (candidate) -> true);
    }

    /**
     * Load the single candidate matching the predicate. Throws if several match or none does.
     *
     * @param targetClass the service type to load
     * @param candidateChooser predicate to filter candidates
     * @param <T> type of the service
     * @return the single matching candidate
     * @throws IllegalStateException if more than one matching candidate is found
     * @throws TargetClassNotFoundException if no matching candidate is found
     */
    static <T> T load(Class<T> targetClass, Predicate<T> candidateChooser) {
        Iterable<T> serviceLoader = ModuleServices.load(targetClass);
        T goodCandidate = null;
        for (T candidate : serviceLoader) {
            if (!candidateChooser.test(candidate)) {
                continue;
            }
            if (goodCandidate != null) {
                throw new IllegalStateException(String.format(
                        "More than one %s found: %n%s%n%s",
                        shortName(targetClass), shortName(goodCandidate), shortName(candidate)));
            }
            goodCandidate = candidate;
        }

        if (goodCandidate == null) {
            throw new TargetClassNotFoundException("Candidate not found: " + targetClass);
        }

        return goodCandidate;
    }

    /**
     * Load all candidates matching the predicate. Returns an empty list if none does.
     *
     * @param targetClass the service type to load
     * @param candidateChooser predicate to filter candidates
     * @param <T> type of the service
     * @return unmodifiable list of all matching candidates (empty if none found)
     */
    static <T> List<T> loadAll(Class<T> targetClass, Predicate<T> candidateChooser) {
        Iterable<T> serviceLoader = ModuleServices.load(targetClass);
        List<T> candidates = new ArrayList<>();
        for (T candidate : serviceLoader) {
            if (candidateChooser.test(candidate)) {
                candidates.add(candidate);
            }
        }
        return Collections.unmodifiableList(candidates);
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
