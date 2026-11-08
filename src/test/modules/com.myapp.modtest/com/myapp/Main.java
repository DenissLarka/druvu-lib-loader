package com.myapp;

import com.druvu.lib.loader.ComponentLoader;
import com.druvu.lib.loader.TargetClassNotFoundException;
import com.myapp.internal.Hidden;
import java.nio.file.Path;
import java.util.stream.Collectors;

/** Loads one component per registration way and prints what arrived, one line each, for ModulePathTest to read. */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        System.out.println(
                "factory " + ComponentLoader.load(Greeter.class).getClass().getName());
        System.out.println(
                "direct " + ComponentLoader.load(AccBook.class).getClass().getName());

        AccBook book = AccBook.create(Path.of("books.xml"));
        System.out.println("direct-factory " + book.getClass().getName() + " " + book.storage());
        System.out.println("direct-factories "
                + Exporter.createAll(Path.of("out")).stream()
                        .map(exporter -> exporter.getClass().getName() + " " + exporter.format())
                        .sorted()
                        .collect(Collectors.joining(", ")));
        try {
            ComponentLoader.load(Exporter.Factory.class);
            System.out.println("duplicate accepted");
        } catch (IllegalStateException e) {
            System.out.println(
                    "duplicate refused: " + e.getMessage().lines().findFirst().orElse(""));
        }

        try {
            System.out.println(
                    "hidden " + ComponentLoader.load(Hidden.class).getClass().getName());
        } catch (TargetClassNotFoundException e) {
            System.out.println("hidden refused: " + e.getMessage());
            for (Throwable reason : e.getSuppressed()) {
                System.out.println("  because: " + reason);
            }
        }
    }
}
