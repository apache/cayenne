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
package org.apache.cayenne.mcp.tools.openproject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
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
import java.util.HexFormat;
import java.util.jar.JarFile;

/**
 * Provides the runnable CayenneModeler jar ({@code org.apache.cayenne:cayenne-modeler}) of a given version, for the
 * MCP server that is not bundled with a Modeler. Looks for it in the local Maven repository, then in its own cache,
 * and if it is in neither, downloads it to the cache from Maven Central (a release) or the Apache snapshots repository
 * (a snapshot). A jar found locally is used as is, even a snapshot: a newer snapshot comes from a local build, or from
 * deleting the cached jar.
 *
 * @since 6.0
 */
class ModelerDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelerDownloader.class);

    static final String CENTRAL = "https://repo1.maven.org/maven2";
    static final String SNAPSHOTS = "https://repository.apache.org/content/repositories/snapshots";
    static final String ARTIFACT_PATH = "org/apache/cayenne/cayenne-modeler";
    static final String ARTIFACT = "cayenne-modeler";

    /**
     * Fetches the contents of a URL of a Maven repository.
     */
    @FunctionalInterface
    interface Repository {
        byte[] fetch(String url) throws IOException;
    }

    private final Path cacheDir;
    private final Path localRepo;
    private final Repository repository;

    /**
     * Creates a downloader caching the jars in the given directory, or in "~/.cayenne/modeler" if it is
     * {@code null}, and looking into the local Maven repository at "~/.m2/repository".
     */
    static ModelerDownloader create(Path cacheDir) {
        Path home = Path.of(System.getProperty("user.home"));
        return new ModelerDownloader(
                cacheDir != null ? cacheDir : home.resolve(".cayenne").resolve("modeler"),
                home.resolve(".m2").resolve("repository"),
                ModelerDownloader::httpFetch);
    }

    ModelerDownloader(Path cacheDir, Path localRepo, Repository repository) {
        this.cacheDir = cacheDir;
        this.localRepo = localRepo;
        this.repository = repository;
    }

    /**
     * Returns a runnable Modeler jar of the given version, downloading it if needed.
     */
    Path jar(String version) throws IOException {
        String name = ARTIFACT + "-" + version + ".jar";

        // the local Maven repository may have it from a local build, or from a build that depends on it. Before
        // 6.0, the jar there was not runnable
        Path localJar = localRepo.resolve(ARTIFACT_PATH).resolve(version).resolve(name);
        if (isRunnable(localJar)) {
            return localJar;
        }

        Path jar = cacheDir.resolve(name);
        if (!Files.isRegularFile(jar)) {
            Files.createDirectories(cacheDir);
            download(downloadUrl(version, name), jar);
        }

        // the Modeler jars published before 6.0 (and the early 6.0 snapshots) have no dependencies included. Not
        // keeping such a jar, so that the next call downloads it again
        if (!isRunnable(jar)) {
            Files.deleteIfExists(jar);
            throw new IOException("%s is not a runnable Modeler jar".formatted(name));
        }
        return jar;
    }

    private String downloadUrl(String version, String name) throws IOException {
        if (!version.endsWith("-SNAPSHOT")) {
            return CENTRAL + "/" + ARTIFACT_PATH + "/" + version + "/" + name;
        }

        // the latest snapshot jar has a timestamped name, listed in the version metadata
        String baseUrl = SNAPSHOTS + "/" + ARTIFACT_PATH + "/" + version;
        String metadata = new String(repository.fetch(baseUrl + "/maven-metadata.xml"), StandardCharsets.UTF_8);
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
            throw new IOException(
                    "Invalid snapshot metadata of %s %s: %s".formatted(ARTIFACT, version, e.getMessage()));
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

        throw new IOException("No jar in the snapshot metadata of %s %s".formatted(ARTIFACT, version));
    }

    private static String text(Element parent, String child) {
        NodeList nodes = parent.getElementsByTagName(child);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : null;
    }

    /**
     * Downloads a file, checks it against the SHA-1 checksum published next to it, and only then moves it to the
     * target location, so that the target is either complete or absent.
     */
    private void download(String url, Path target) throws IOException {
        LOGGER.info("Downloading {}", url);

        byte[] bytes = repository.fetch(url);
        String checksum = new String(repository.fetch(url + ".sha1"), StandardCharsets.US_ASCII);
        String expected = checksum.trim().split("\\s+")[0];
        String actual = sha1(bytes);
        if (!actual.equalsIgnoreCase(expected)) {
            throw new IOException("Checksum mismatch for %s: expected %s, got %s".formatted(url, expected, actual));
        }

        Path tmp = Files.createTempFile(cacheDir, target.getFileName().toString(), ".part");
        try {
            Files.write(tmp, bytes);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static boolean isRunnable(Path jar) {
        if (!Files.isRegularFile(jar)) {
            return false;
        }

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            return jarFile.getManifest() != null
                    && jarFile.getManifest().getMainAttributes().getValue("Main-Class") != null;
        } catch (IOException e) {
            return false;
        }
    }

    static String sha1(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] httpFetch(String url) throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .proxy(ProxySelector.getDefault())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .header("User-Agent", "cayenne-mcp-server")
                .build();

        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + url, e);
        } catch (IOException e) {
            throw new IOException("Can't reach %s: %s".formatted(url, e), e);
        }

        if (response.statusCode() != 200) {
            throw new IOException("Got HTTP %d for %s".formatted(response.statusCode(), url));
        }
        return response.body();
    }
}
