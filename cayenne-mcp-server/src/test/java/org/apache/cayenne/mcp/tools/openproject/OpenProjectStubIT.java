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

import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.apache.cayenne.mcp.tools.openproject.HandshakeWatcher.Outcome;
import org.apache.cayenne.mcp.tools.openproject.HandshakeWatcher.WatchResult;
import org.apache.cayenne.mcp.tools.openproject.ModelerLauncher.LaunchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spawns a stub jar that plays the Modeler's role on the handshake side, and asserts
 * the full {@code launch + await} round-trip succeeds. Validates that
 * {@link ModelerLauncher} produces a correctly-detached child JVM and
 * {@link HandshakeWatcher} reads the prefs entries it writes.
 */
public class OpenProjectStubIT {

    @Test
    public void handshakeRoundtripViaGenericJar(@TempDir Path tmp) throws Exception {
        Path stubJar = buildStubJar(tmp.resolve("stub.jar"));
        Path fakeProject = tmp.resolve("fake-cayenne-project.xml");
        Files.writeString(fakeProject, "<?xml version=\"1.0\"?>\n<project/>");

        String nonce = "it-" + UUID.randomUUID().toString().replace("-", "");
        LaunchResult launch = ModelerLauncher.launch(
                LauncherKind.GENERIC_JAR, stubJar, fakeProject, nonce);

        try {
            WatchResult watch = HandshakeWatcher.await(
                    nonce, () -> launch.process().isAlive(), Duration.ofSeconds(15), new PrefsLocator());

            assertEquals(Outcome.HANDSHAKE_RECEIVED, watch.outcome(),
                    "expected stub to write handshake within 15s");
            assertNotNull(watch.data());
            assertTrue(watch.data().pid() > 0,
                    "stub reported its pid: " + watch.data().pid());
            assertEquals(fakeProject.toString(), watch.data().resolvedProjectPath());
            assertNotNull(watch.data().startedAt());
        } finally {
            launch.process().destroyForcibly();
        }
    }

    private static Path buildStubJar(Path jar) throws IOException {
        Manifest manifest = new Manifest();
        Attributes mainAttrs = manifest.getMainAttributes();
        mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttrs.put(Attributes.Name.MAIN_CLASS, HandshakeStubMain.class.getName());

        String classResource = HandshakeStubMain.class.getName().replace('.', '/') + ".class";
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar), manifest);
             InputStream classBytes = OpenProjectStubIT.class.getClassLoader()
                     .getResourceAsStream(classResource)) {
            if (classBytes == null) {
                throw new IOException("Could not locate class resource: " + classResource);
            }
            jos.putNextEntry(new JarEntry(classResource));
            classBytes.transferTo(jos);
            jos.closeEntry();
        }
        return jar;
    }
}
