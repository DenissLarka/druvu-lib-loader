package com.myapp.plugins;

import com.myapp.Exporter;
import java.nio.file.Path;

/** One of the two {@code Exporter.Factory} registrations: {@code loadAll} takes both, {@code load} refuses the pair. */
public final class CsvExporterFactory implements Exporter.Factory {
    @Override
    public Exporter create(Path target) {
        return new CsvExporter(target);
    }
}
