package com.myapp;

import com.druvu.lib.loader.ComponentLoader;
import com.druvu.lib.loader.TargetClassNotFoundException;
import com.myapp.internal.Hidden;

/** Loads one component per registration style and prints what arrived, one line each, for ModulePathTest to read. */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        System.out.println(
                "tier1 " + ComponentLoader.load(Greeter.class).getClass().getName());
        System.out.println(
                "tier2 " + ComponentLoader.load(Printer.class).getClass().getName());
        System.out.println(
                "tier3 " + ComponentLoader.load(AccBook.class).getClass().getName());
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
