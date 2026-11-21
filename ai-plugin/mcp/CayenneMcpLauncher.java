/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.jar.JarFile;

/**
 * Starts the Cayenne MCP server for the "apache-cayenne" Claude Code plugin. Run by the plugin as a single-file
 * source program ({@code java CayenneMcpLauncher.java ...}), so it needs nothing but a JDK. It fetches the
 * {@code org.apache.cayenne:cayenne-mcp-server} jar of the requested version from the local Maven repository or its own
 * cache, or if it is in neither, downloads it to the cache (a release from Maven Central, or a snapshot from the Apache
 * snapshots repository), and runs it in this JVM. A jar found locally is used as is, even a snapshot: a newer snapshot
 * comes from a local build, or from deleting the cached jar. Everything the launcher prints goes to stderr, as stdout
 * belongs to the MCP protocol.
 * <p>
 * Arguments:
 * <ul>
 * <li>{@code --server-version <version>} - the server version, e.g. "6.0" or "6.0-SNAPSHOT". Required.</li>
 * <li>{@code --cache-dir <dir>} - where to keep the downloaded jars, the server's and the CayenneModeler's (the
 * server downloads the latter, and gets this directory passed along). Defaults to "~/.cayenne/mcp".</li>
 * </ul>
 * <p>
 * The code sticks to Java 11 syntax and API, so that an older JDK compiles this file and gets to report that the
 * server needs Java 21, instead of failing with a compilation error.
 */
public class CayenneMcpLauncher {

    static final int REQUIRED_JAVA = 21;

    static final String CENTRAL = "https://repo1.maven.org/maven2";
    static final String SNAPSHOTS = "https://repository.apache.org/content/repositories/snapshots";
    static final String ARTIFACT_PATH = "org/apache/cayenne/cayenne-mcp-server";
    static final String ARTIFACT = "cayenne-mcp-server";

    public static void main(String[] args) throws Exception {

        // honor the OS proxy settings, unless told otherwise. Must be set before the first network call, as Java reads
        // it only once. The server sets it the same way, but it starts after the launcher's own download
        if (System.getProperty("java.net.useSystemProxies") == null) {
            System.setProperty("java.net.useSystemProxies", "true");
        }

        if (Runtime.version().feature() < REQUIRED_JAVA) {
            fail("The Cayenne MCP server needs Java " + REQUIRED_JAVA + " or newer, but 'java' on the PATH is "
                    + Runtime.version() + " (" + System.getProperty("java.home") + ")");
        }

        String serverVersion = null;
        String cacheDir = null;

        for (int i = 0; i < args.length; i++) {
            String value = i + 1 < args.length ? args[i + 1] : null;
            switch (args[i]) {
                case "--server-version":
                    serverVersion = value;
                    i++;
                    break;
                case "--cache-dir":
                    cacheDir = value;
                    i++;
                    break;
                default:
                    fail("Unknown launcher argument: " + args[i]);
            }
        }

        if (!isSet(serverVersion)) {
            fail("Launcher argument --server-version is required");
        }

        Path cache = isSet(cacheDir)
                ? Path.of(cacheDir)
                : Path.of(System.getProperty("user.home"), ".cayenne", "mcp");

        Path jar = resolveJar(serverVersion, cache);

        // the server caches the Modeler jar it downloads in the same directory
        runServer(jar, new String[]{"--cache-dir", cache.toString()});
    }

    // a variable Claude Code didn't expand may come as an empty string or as an unexpanded "${...}" placeholder
    static boolean isSet(String value) {
        return value != null && !value.isBlank() && !value.startsWith("${");
    }

    static Path resolveJar(String version, Path cache) throws IOException, InterruptedException {

        // the local Maven repository may have it from a local build ("mvn install"), or from a build that depends on it
        String name = ARTIFACT + "-" + version + ".jar";
        Path m2Jar = Path.of(System.getProperty("user.home"), ".m2", "repository", ARTIFACT_PATH, version, name);
        if (Files.isRegularFile(m2Jar)) {
            return m2Jar;
        }

        Path jar = cache.resolve(name);
        if (!Files.isRegularFile(jar)) {
            Files.createDirectories(cache);
            download(downloadUrl(version, name), jar);
        }
        return jar;
    }

    static String downloadUrl(String version, String name) throws IOException, InterruptedException {
        if (!version.endsWith("-SNAPSHOT")) {
            return CENTRAL + "/" + ARTIFACT_PATH + "/" + version + "/" + name;
        }

        // the latest snapshot jar has a timestamped name, listed in the version metadata
        String baseUrl = SNAPSHOTS + "/" + ARTIFACT_PATH + "/" + version;
        String metadata = new String(fetch(baseUrl + "/maven-metadata.xml"), StandardCharsets.UTF_8);
        return baseUrl + "/" + ARTIFACT + "-" + snapshotJarVersion(metadata, version) + ".jar";
    }

    /**
     * Returns the timestamped version of the latest snapshot jar (e.g. "6.0-20261121.165512-21") from the
     * "maven-metadata.xml" of a snapshot version.
     */
    static String snapshotJarVersion(String metadata, String version) throws IOException {
        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            byte[] bytes = metadata.getBytes(StandardCharsets.UTF_8);
            doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new IOException("Invalid snapshot metadata of " + ARTIFACT + " " + version + ": " + e.getMessage());
        }

        NodeList snapshotVersions = doc.getElementsByTagName("snapshotVersion");
        for (int i = 0; i < snapshotVersions.getLength(); i++) {
            Element sv = (Element) snapshotVersions.item(i);
            if ("jar".equals(text(sv, "extension")) && text(sv, "classifier") == null) {
                String value = text(sv, "value");
                if (value != null) {
                    return value;
                }
            }
        }

        throw new IOException("No jar in the snapshot metadata of " + ARTIFACT + " " + version);
    }

    static String text(Element parent, String child) {
        NodeList nodes = parent.getElementsByTagName(child);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : null;
    }

    /**
     * Downloads a file from a Maven repository, checks it against the SHA-1 checksum published next to it, and only
     * then moves it to the target location, so that the target is either complete or absent.
     */
    static void download(String url, Path target) throws IOException, InterruptedException {
        log("Downloading " + url);

        byte[] bytes = fetch(url);
        String expected = new String(fetch(url + ".sha1"), StandardCharsets.US_ASCII).trim().split("\\s+")[0];
        String actual = sha1(bytes);
        if (!actual.equalsIgnoreCase(expected)) {
            throw new IOException("Checksum mismatch for " + url + ": expected " + expected + ", got " + actual);
        }

        Path tmp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".part");
        try {
            Files.write(tmp, bytes);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    static byte[] fetch(String url) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newBuilder()
                .proxy(ProxySelector.getDefault())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .header("User-Agent", "cayenne-mcp-launcher")
                .build();

        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new IOException("Can't reach " + url + ": " + e, e);
        }

        if (response.statusCode() != 200) {
            throw new IOException("Got HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    static String sha1(byte[] bytes) {
        try {
            StringBuilder hex = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-1").digest(bytes)) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Runs the server jar's main class in this JVM, in a class loader isolated from the launcher.
     */
    static void runServer(Path jar, String[] args) throws Exception {
        String mainClass;
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            mainClass = jarFile.getManifest() != null
                    ? jarFile.getManifest().getMainAttributes().getValue("Main-Class")
                    : null;
        }
        if (mainClass == null) {
            fail(jar + " has no Main-Class, it is not a runnable MCP server jar");
        }

        URL[] classpath = {jar.toUri().toURL()};
        URLClassLoader loader = new URLClassLoader(classpath, ClassLoader.getPlatformClassLoader());
        Thread.currentThread().setContextClassLoader(loader);

        try {
            Class.forName(mainClass, true, loader).getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
        }
    }

    static void log(String message) {
        System.err.println("[cayenne-mcp-launcher] " + message);
    }

    static void fail(String message) {
        log(message);
        System.exit(1);
    }
}
