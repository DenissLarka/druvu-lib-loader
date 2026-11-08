package com.druvu.lib.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The cardinality rules of this library over the candidates a {@link java.util.ServiceLoader} yields: {@link #one}
 * wants at most one match and throws on a second, {@link #all} takes every match. Iterating the candidates instantiates
 * them, so a registration that cannot be instantiated throws the JDK's {@link java.util.ServiceConfigurationError} out
 * of either method, as a deployment error should.
 *
 * @author Deniss Larka on 18 Jan 2025
 */
final class ServiceLoaderExtended {

    private ServiceLoaderExtended() {}

    /**
     * The single candidate, empty when there is none.
     *
     * @throws IllegalStateException when there are two
     */
    static <T> Optional<T> one(Class<T> type, Iterable<T> candidates) {
        return one(type, candidates, candidate -> true);
    }

    /**
     * The single candidate matching the predicate, empty when none does.
     *
     * @param type the service type, for the message
     * @param candidates what the service loader yields
     * @param chooser which candidates count
     * @param <T> the service type
     * @return the one match, or empty
     * @throws IllegalStateException when two candidates match
     */
    static <T> Optional<T> one(Class<T> type, Iterable<T> candidates, Predicate<? super T> chooser) {
        T match = null;
        for (T candidate : candidates) {
            if (!chooser.test(candidate)) {
                continue;
            }
            if (match != null) {
                throw new IllegalStateException(String.format(
                        "More than one %s found: %n%s%n%s",
                        type.getSimpleName(),
                        match.getClass().getName(),
                        candidate.getClass().getName()));
            }
            match = candidate;
        }
        return Optional.ofNullable(match);
    }

    /** Every candidate, in the service loader's order; an unmodifiable list, empty when there is none. */
    static <T> List<T> all(Iterable<T> candidates) {
        return all(candidates, candidate -> true);
    }

    /**
     * Every candidate matching the predicate, in the service loader's order.
     *
     * @param candidates what the service loader yields
     * @param chooser which candidates count
     * @param <T> the service type
     * @return an unmodifiable list, empty when none matches
     */
    static <T> List<T> all(Iterable<T> candidates, Predicate<? super T> chooser) {
        List<T> matches = new ArrayList<>();
        for (T candidate : candidates) {
            if (chooser.test(candidate)) {
                matches.add(candidate);
            }
        }
        return Collections.unmodifiableList(matches);
    }
}
