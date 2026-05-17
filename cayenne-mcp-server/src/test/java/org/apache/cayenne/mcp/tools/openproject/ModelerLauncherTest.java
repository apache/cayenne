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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModelerLauncherTest {

    private static final String NONCE = "deadbeefcafebabe";
    private static final Path PROJECT = Paths.get("/abs/path/to/cayenne-project.xml");

    @Test
    public void macArgvWrapsWithOpenAndArgs() {
        Path bundle = Paths.get("/Applications/CayenneModeler.app");
        List<String> argv = ModelerLauncher.buildCommand(
                LauncherKind.MAC_APP, bundle, PROJECT, NONCE);

        assertEquals("open", argv.get(0));
        assertEquals("-n", argv.get(1));
        assertEquals(bundle.toString(), argv.get(2));
        assertEquals("--args", argv.get(3));
        assertEquals("--mcp-handshake", argv.get(4));
        assertEquals(NONCE, argv.get(5));
        assertEquals(PROJECT.toString(), argv.get(6));
        assertEquals(7, argv.size());
    }

    @Test
    public void windowsArgvIsLauncherFirst() {
        Path exe = Paths.get("C:/Program Files/Cayenne/CayenneModeler.exe");
        List<String> argv = ModelerLauncher.buildCommand(
                LauncherKind.WINDOWS_EXE, exe, PROJECT, NONCE);

        assertEquals(exe.toString(), argv.get(0));
        assertEquals("--mcp-handshake", argv.get(1));
        assertEquals(NONCE, argv.get(2));
        assertEquals(PROJECT.toString(), argv.get(3));
        assertEquals(4, argv.size());
    }

    @Test
    public void genericArgvUsesCurrentJavaBinary() {
        Path jar = Paths.get("/opt/cayenne/bin/CayenneModeler.jar");
        List<String> argv = ModelerLauncher.buildCommand(
                LauncherKind.GENERIC_JAR, jar, PROJECT, NONCE);

        // The first token must point inside the running JVM's home — never $JAVA_HOME or $PATH.
        Path javaHome = Paths.get(System.getProperty("java.home"));
        assertTrue(Paths.get(argv.get(0)).startsWith(javaHome),
                "Generic launcher must use the running JVM: " + argv.get(0));
        assertEquals("-jar", argv.get(1));
        assertEquals(jar.toString(), argv.get(2));
        assertEquals("--mcp-handshake", argv.get(3));
        assertEquals(NONCE, argv.get(4));
        assertEquals(PROJECT.toString(), argv.get(5));
        assertEquals(6, argv.size());
    }
}
