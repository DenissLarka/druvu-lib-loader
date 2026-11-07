package com.myapp.plugins;

import com.myapp.Exporter;
import java.nio.file.Path;

/** The other {@code Exporter.Factory} registration. */
public final class PdfExporterFactory implements Exporter.Factory {
    @Override
    public Exporter create(Path target) {
        return new PdfExporter(target);
    }
}
