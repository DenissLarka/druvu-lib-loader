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
 * Proves every registration style on the module path: the consumer module under {@code src/test/modules} is compiled
 * against the library's classes and launched in its own JVM with {@code --module-path}. The suite itself runs on the
 * class path, where the JDK's {@code uses} check does not exist, so no other test here can catch what this one catches.
 */
public class ModulePathTest {

    private static final String MODULE = "com.myapp.modtest";
    private static final String MAIN = "com.myapp.Main";

    @Test(timeOut = 120_000)
    public void everyRegistrationStyleLoadsOnTheModulePath() throws Exception {
        Path libraryClasses = libraryClasses();
        Path fixtureSources =
                libraryClasses.resolve("../../src/test/modules").normalize().resolve(MODULE);
        Path workDir = Files.createTempDirectory("druvu-lib-loader-modtest");
        try {
            Path modules = workDir.resolve("modules");
            compile(fixtureSources, libraryClasses, modules.resolve(MODULE));
            Run run = launch(libraryClasses, modules, workDir.resolve("stderr.txt"));

            assertThat(run.exitCode())
                    .as("exit code; stderr:%n%s", run.stderr())
                    .isZero();
            List<String> lines = run.stdout().lines().toList();
            assertThat(lines)
                    .startsWith(
                            "tier1 com.myapp.csv.CsvGreeter",
                            "tier2 com.myapp.csv.CsvPrinter",
                            "tier3 com.myapp.csv.CsvAccBook",
                            "hidden refused: No ComponentFactory or ServiceLoader provider for com.myapp.internal.Hidden");
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
            List<String> options = List.of("--module-path", modulePath.toString(), "-d", output.toString());
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
        Process process = new ProcessBuilder(java, "--module-path", modulePath, "--module", MODULE + "/" + MAIN)
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
