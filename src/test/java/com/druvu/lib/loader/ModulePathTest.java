package com.druvu.lib.loader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.testng.annotations.Test;

/**
 * Proves every registration way on the module path: the consumer modules under {@code src/test/modules} are compiled
 * against the library's classes and launched in their own JVM with {@code --module-path}. The API and the app are one
 * module; the implementation module {@code com.myapp.plugins} is required by nobody and reaches the module graph
 * through the API's {@code uses} lines only. The suite itself runs on the class path, where neither the JDK's
 * {@code uses} check nor the module graph exists, so no other test here can catch what this one catches.
 */
public class ModulePathTest {

    private static final String APP_MODULE = "com.myapp.modtest";
    private static final String MAIN = "com.myapp.Main";

    @Test(timeOut = 120_000)
    public void everyRegistrationWayLoadsOnTheModulePath() throws Exception {
        Path libraryClasses = libraryClasses();
        Path fixtureSources = libraryClasses.resolve("../../src/test/modules").normalize();
        Path workDir = Files.createTempDirectory("druvu-lib-loader-modtest");
        try {
            Path modules = workDir.resolve("modules");
            compile(fixtureSources, libraryClasses, modules);
            Run run = launch(libraryClasses, modules, workDir.resolve("stderr.txt"));

            assertThat(run.exitCode())
                    .as("exit code; stderr:%n%s", run.stderr())
                    .isZero();
            List<String> lines = run.stdout().lines().toList();
            assertThat(lines)
                    .startsWith(
                            "factory com.myapp.csv.CsvGreeter",
                            "direct com.myapp.csv.CsvAccBook",
                            "direct-factory com.myapp.plugins.XmlBook books.xml",
                            "direct-factories com.myapp.plugins.CsvExporter csv to out, com.myapp.plugins.PdfExporter pdf to out");
            assertThat(lines.get(4)).startsWith("duplicate refused: More than one");
            assertThat(lines.get(5))
                    .isEqualTo(
                            "hidden refused: No ComponentFactory produces com.myapp.internal.Hidden, and no registration under it could be loaded");
            assertThat(lines)
                    .anySatisfy(line -> assertThat(line)
                            .startsWith("  because: java.util.ServiceConfigurationError")
                            .contains("not accessible to module com.druvu.lib.loader"));
        } finally {
            deleteTree(workDir);
        }
    }

    /** The library's compiled classes: an exploded module, {@code module-info.class} at its root. */
    private static Path libraryClasses() throws URISyntaxException {
        Path classes = Path.of(ComponentLoader.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
        assertThat(classes.resolve("module-info.class"))
                .as("the library as an exploded module")
                .exists();
        return classes;
    }

    /** Compiles every module under the fixture root into {@code output/<module>}, the library on the module path. */
    private static void compile(Path sources, Path modulePath, Path output) throws IOException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertThat(javac).as("the system Java compiler (a JDK runs this suite)").isNotNull();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(sources)) {
            files = walk.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertThat(files).as("fixture sources under %s", sources).isNotEmpty();
        Files.createDirectories(output);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager =
                javac.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            List<String> options = List.of(
                    "--module-path", modulePath.toString(),
                    "--module-source-path", sources.toString(),
                    "-d", output.toString());
            Boolean compiled = javac.getTask(
                            null,
                            fileManager,
                            diagnostics,
                            options,
                            null,
                            fileManager.getJavaFileObjectsFromPaths(files))
                    .call();
            assertThat(compiled)
                    .as("javac said:%n%s", diagnostics.getDiagnostics())
                    .isTrue();
        }
    }

    private static Run launch(Path libraryClasses, Path modules, Path stderrFile)
            throws IOException, InterruptedException {
        String java = ProcessHandle.current()
                .info()
                .command()
                .orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        String modulePath = libraryClasses + File.pathSeparator + modules;
        Process process = new ProcessBuilder(java, "--module-path", modulePath, "--module", APP_MODULE + "/" + MAIN)
                .redirectError(stderrFile.toFile())
                .start();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean ended = process.waitFor(1, TimeUnit.MINUTES);
        assertThat(ended).as("the consumer JVM ended").isTrue();
        return new Run(process.exitValue(), stdout, Files.readString(stderrFile));
    }

    private static void deleteTree(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private record Run(int exitCode, String stdout, String stderr) {}
}
