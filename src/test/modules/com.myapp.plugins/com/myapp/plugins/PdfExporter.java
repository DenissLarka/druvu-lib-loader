package com.myapp.plugins;

import com.myapp.Exporter;
import java.nio.file.Path;

final class PdfExporter implements Exporter {
    private final Path target;

    PdfExporter(Path target) {
        this.target = target;
    }

    @Override
    public String format() {
        return "pdf to " + target;
    }
}
