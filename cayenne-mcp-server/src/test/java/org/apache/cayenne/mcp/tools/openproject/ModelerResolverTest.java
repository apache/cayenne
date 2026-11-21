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

import org.apache.cayenne.mcp.tools.openproject.ModelerDownloaderTest.FakeRepository;
import org.apache.cayenne.mcp.tools.openproject.ModelerResolver.Resolved;
import org.apache.cayenne.mcp.tools.openproject.ModelerResolver.Unresolved;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectErrorCode;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectModelerSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModelerResolverTest {

    @TempDir
    Path tmp;

    @Test
    public void bundled() throws IOException {
        Path mcpDir = Files.createDirectories(tmp.resolve("bin"));
        Path launcher = Files.createFile(mcpDir.resolve(ModelerDiscovery.GENERIC_JAR_NAME));

        // the bundled Modeler is used without any download
        FakeRepository repo = new FakeRepository();
        Resolved r = assertInstanceOf(Resolved.class, resolver(mcpDir, "6.0", repo).resolve());

        assertEquals(launcher, r.found().launcher());
        assertEquals(LauncherKind.GENERIC_JAR, r.found().launcherKind());
        assertEquals(OpenProjectModelerSource.bundled, r.source());
        assertTrue(repo.fetched.isEmpty());
    }

    @Test
    public void downloaded() throws IOException {
        FakeRepository repo = new FakeRepository().publish(
                ModelerDownloader.CENTRAL + "/org/apache/cayenne/cayenne-modeler/6.0/cayenne-modeler-6.0.jar",
                ModelerDownloaderTest.jar(true));

        Resolved r = assertInstanceOf(Resolved.class, resolver(tmp, "6.0", repo).resolve());

        assertEquals(tmp.resolve("cache/cayenne-modeler-6.0.jar"), r.found().launcher());
        assertEquals(LauncherKind.GENERIC_JAR, r.found().launcherKind());
        assertEquals(OpenProjectModelerSource.downloaded, r.source());
    }

    @Test
    public void downloadFailed() {
        Unresolved u = assertInstanceOf(Unresolved.class, resolver(null, "6.0", new FakeRepository()).resolve());

        assertEquals(OpenProjectErrorCode.modeler_download_failed, u.code());
        assertTrue(u.message().startsWith("Can't download CayenneModeler 6.0: Got HTTP 404"), u.message());
    }

    @Test
    public void unpublishedServerVersion() {
        FakeRepository repo = new FakeRepository();
        Unresolved u = assertInstanceOf(Unresolved.class, resolver(tmp, "dev", repo).resolve());

        assertEquals(OpenProjectErrorCode.modeler_not_found, u.code());
        assertTrue(u.message().contains("\"dev\""), u.message());
        assertTrue(repo.fetched.isEmpty());
    }

    private ModelerResolver resolver(Path mcpDir, String version, FakeRepository repo) {
        ModelerDownloader downloader = new ModelerDownloader(tmp.resolve("cache"), tmp.resolve("m2"), repo);
        return new ModelerResolver(OsKind.OTHER, mcpDir, version, downloader);
    }
}
