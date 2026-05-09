package org.apache.cayenne.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class McpStarter {

    private static final String JAVA_EXE = javaExecutable();
    private static final String MCP_JAR = mcpJar();

    public static Process start(String... flags) {
        List<String> cmd = new ArrayList<>();
        cmd.add(JAVA_EXE);
        cmd.add("-jar");
        cmd.add(MCP_JAR);
        cmd.addAll(List.of(flags));
        try {
            return new ProcessBuilder(cmd).start();
        } catch (IOException e) {
            throw new IllegalStateException("Error starting MCP server", e);
        }
    }

    private static String javaExecutable() {
        String name = System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", name).toString();
    }

    private static String mcpJar() {
        Path targetDir = Path.of(System.getProperty("user.dir"), "target");

        if (!Files.isDirectory(targetDir)) {
            throw new IllegalStateException("Shaded jar not found — run mvn package first");
        }

        try (Stream<Path> stream = Files.list(targetDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().matches("cayenne-mcp-server-.*\\.jar"))
                    .filter(p -> !p.getFileName().toString().contains("original"))
                    .filter(p -> !p.getFileName().toString().contains("sources"))
                    .map(Path::toString)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Shaded jar not found — run mvn package first"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
