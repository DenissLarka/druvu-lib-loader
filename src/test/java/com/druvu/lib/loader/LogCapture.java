package com.druvu.lib.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Collects what a class logs through {@code System.Logger}, which the JDK routes to {@code java.util.logging} in this
 * suite: attached to the logger named after the class until closed.
 */
final class LogCapture implements AutoCloseable {

    private final Logger logger;
    private final Level levelBefore;
    private final Handler handler;
    private final List<LogRecord> records = new ArrayList<>();

    LogCapture(Class<?> source) {
        logger = Logger.getLogger(source.getName());
        levelBefore = logger.getLevel();
        logger.setLevel(Level.ALL);
        handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        logger.addHandler(handler);
    }

    /** The messages logged at WARNING or above since the capture started. */
    List<String> warnings() {
        return records.stream()
                .filter(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
                .map(LogRecord::getMessage)
                .toList();
    }

    @Override
    public void close() {
        logger.removeHandler(handler);
        logger.setLevel(levelBefore);
    }
}
