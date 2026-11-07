/**
 * An implementation module nothing requires: it joins the module graph only because the API module declares
 * {@code uses} for the factory types provided here. No library type anywhere in this module.
 */
module com.myapp.plugins {
    requires com.myapp.modtest;

    provides com.myapp.AccBook.Factory with
            com.myapp.plugins.XmlBookFactory;
    provides com.myapp.Exporter.Factory with
            com.myapp.plugins.CsvExporterFactory,
            com.myapp.plugins.PdfExporterFactory;
}
