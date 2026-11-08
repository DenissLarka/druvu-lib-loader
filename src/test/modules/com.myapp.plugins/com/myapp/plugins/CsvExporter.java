package com.myapp.plugins;

import com.myapp.Exporter;
import java.nio.file.Path;

final class CsvExporter implements Exporter {
    private final Path target;

    CsvExporter(Path target) {
        this.target = target;
    }

    @Override
    public String format() {
        return "csv to " + target;
    }
}
