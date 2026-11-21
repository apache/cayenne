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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.apache.cayenne.mcp.tools.openproject.ModelerDownloader.CENTRAL;
import static org.apache.cayenne.mcp.tools.openproject.ModelerDownloader.SNAPSHOTS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModelerDownloaderTest {

    private static final String RELEASE_URL =
            CENTRAL + "/org/apache/cayenne/cayenne-modeler/6.0/cayenne-modeler-6.0.jar";
    private static final String SNAPSHOT_BASE = SNAPSHOTS + "/org/apache/cayenne/cayenne-modeler/6.0-SNAPSHOT";

    @TempDir
    Path tmp;

    @Test
    public void localRepoRunnableJar() throws IOException {
        Path local = tmp.resolve("m2/org/apache/cayenne/cayenne-modeler/6.0/cayenne-modeler-6.0.jar");
        Files.createDirectories(local.getParent());
        Files.write(local, jar(true));

        FakeRepository repo = new FakeRepository();
        assertEquals(local, downloader(repo).jar("6.0"));
        assertTrue(repo.fetched.isEmpty());
    }

    @Test
    public void localRepoThinJarIgnored() throws IOException {
        Path local = tmp.resolve("m2/org/apache/cayenne/cayenne-modeler/6.0/cayenne-modeler-6.0.jar");
        Files.createDirectories(local.getParent());
        Files.write(local, jar(false));

        FakeRepository repo = new FakeRepository().publish(RELEASE_URL, jar(true));
        Path jar = downloader(repo).jar("6.0");

        assertEquals(tmp.resolve("cache/cayenne-modeler-6.0.jar"), jar);
    }

    @Test
    public void release() throws IOException {
        byte[] content = jar(true);
        FakeRepository repo = new FakeRepository().publish(RELEASE_URL, content);
        ModelerDownloader downloader = downloader(repo);

        Path jar = downloader.jar("6.0");
        assertEquals(tmp.resolve("cache/cayenne-modeler-6.0.jar"), jar);
        assertArrayEquals(content, Files.readAllBytes(jar));
        assertEquals(List.of(RELEASE_URL, RELEASE_URL + ".sha1"), repo.fetched);

        // the second call is served from the cache
        repo.fetched.clear();
        assertEquals(jar, downloader.jar("6.0"));
        assertTrue(repo.fetched.isEmpty());
    }

    @Test
    public void notRunnable() throws IOException {
        FakeRepository repo = new FakeRepository().publish(RELEASE_URL, jar(false));

        IOException e = assertThrows(IOException.class, () -> downloader(repo).jar("6.0"));
        assertEquals("cayenne-modeler-6.0.jar is not a runnable Modeler jar", e.getMessage());
        assertFalse(Files.exists(tmp.resolve("cache/cayenne-modeler-6.0.jar")));
    }

    @Test
    public void checksumMismatch() throws IOException {
        FakeRepository repo = new FakeRepository().publish(RELEASE_URL, jar(true));
        repo.files.put(RELEASE_URL + ".sha1", "0000".getBytes(StandardCharsets.US_ASCII));

        IOException e = assertThrows(IOException.class, () -> downloader(repo).jar("6.0"));

        assertTrue(e.getMessage().startsWith("Checksum mismatch"), e.getMessage());
        assertFalse(Files.exists(tmp.resolve("cache/cayenne-modeler-6.0.jar")));
    }

    @Test
    public void snapshot() throws IOException {
        FakeRepository repo = new FakeRepository()
                .publish(SNAPSHOT_BASE + "/maven-metadata.xml", metadata("6.0-20261121.165512-21"))
                .publish(SNAPSHOT_BASE + "/cayenne-modeler-6.0-20261121.165512-21.jar", jar(true));
        ModelerDownloader downloader = downloader(repo);

        Path jar = downloader.jar("6.0-SNAPSHOT");
        assertEquals(tmp.resolve("cache/cayenne-modeler-6.0-SNAPSHOT.jar"), jar);

        // a snapshot is not refreshed: once there, it is used as is
        repo.fetched.clear();
        repo.publish(SNAPSHOT_BASE + "/maven-metadata.xml", metadata("6.0-20261122.101010-22"));
        assertEquals(jar, downloader.jar("6.0-SNAPSHOT"));
        assertTrue(repo.fetched.isEmpty());
    }

    @Test
    public void cachedSnapshotUsedAsIs() throws IOException {
        Path cached = tmp.resolve("cache/cayenne-modeler-6.0-SNAPSHOT.jar");
        Files.createDirectories(cached.getParent());
        Files.write(cached, jar(true));

        FakeRepository repo = new FakeRepository();
        assertEquals(cached, downloader(repo).jar("6.0-SNAPSHOT"));
        assertTrue(repo.fetched.isEmpty());
    }

    @Test
    public void localRepoSnapshot() throws IOException {
        Path local = tmp.resolve("m2/org/apache/cayenne/cayenne-modeler/6.0-SNAPSHOT/cayenne-modeler-6.0-SNAPSHOT.jar");
        Files.createDirectories(local.getParent());
        Files.write(local, jar(true));

        FakeRepository repo = new FakeRepository();
        assertEquals(local, downloader(repo).jar("6.0-SNAPSHOT"));
        assertTrue(repo.fetched.isEmpty());
    }

    @Test
    public void snapshotOfflineNothingCached() {
        IOException e = assertThrows(IOException.class, () -> downloader(new FakeRepository()).jar("6.0-SNAPSHOT"));
        assertTrue(e.getMessage().contains("maven-metadata.xml"), e.getMessage());
    }

    @Test
    public void snapshotJarVersionSkipsOtherArtifacts() throws IOException {
        String metadata = """
                <metadata>
                  <versioning>
                    <snapshotVersions>
                      <snapshotVersion>
                        <extension>pom</extension>
                        <value>6.0-20261121.165512-21</value>
                      </snapshotVersion>
                      <snapshotVersion>
                        <classifier>sources</classifier>
                        <extension>jar</extension>
                        <value>6.0-20261121.165512-20</value>
                      </snapshotVersion>
                      <snapshotVersion>
                        <extension>jar</extension>
                        <value>6.0-20261121.165512-21</value>
                      </snapshotVersion>
                    </snapshotVersions>
                  </versioning>
                </metadata>""";

        assertEquals("6.0-20261121.165512-21", ModelerDownloader.snapshotJarVersion(metadata, "6.0-SNAPSHOT"));
    }

    private ModelerDownloader downloader(FakeRepository repo) {
        return new ModelerDownloader(tmp.resolve("cache"), tmp.resolve("m2"), repo);
    }

    private static byte[] metadata(String jarVersion) {
        return """
                <metadata>
                  <versioning>
                    <snapshotVersions>
                      <snapshotVersion>
                        <extension>jar</extension>
                        <value>%s</value>
                      </snapshotVersion>
                    </snapshotVersions>
                  </versioning>
                </metadata>""".formatted(jarVersion).getBytes(StandardCharsets.UTF_8);
    }

    static byte[] jar(boolean runnable) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (runnable) {
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.apache.cayenne.modeler.Main");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JarOutputStream(out, manifest).close();
        return out.toByteArray();
    }

    static class FakeRepository implements ModelerDownloader.Repository {

        final Map<String, byte[]> files = new HashMap<>();
        final List<String> fetched = new ArrayList<>();

        FakeRepository publish(String url, byte[] content) {
            files.put(url, content);
            files.put(url + ".sha1", ModelerDownloader.sha1(content).getBytes(StandardCharsets.US_ASCII));
            return this;
        }

        @Override
        public byte[] fetch(String url) throws IOException {
            fetched.add(url);
            byte[] content = files.get(url);
            if (content == null) {
                throw new IOException("Got HTTP 404 for " + url);
            }
            return content;
        }
    }
}
