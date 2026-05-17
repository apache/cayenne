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

package org.apache.cayenne.modeler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CliArgsTest {

    @TempDir
    Path tmp;

    @Test
    public void empty() {
        CliArgs cli = CliArgs.parse(new String[0]);
        assertNull(cli.initialProject());
        assertNull(cli.mcpHandshakeNonce());
        assertNotNull(cli.rawArgs());
    }

    @Test
    public void nullArgs() {
        CliArgs cli = CliArgs.parse(null);
        assertNull(cli.initialProject());
        assertNull(cli.mcpHandshakeNonce());
        assertNotNull(cli.rawArgs());
        assertEquals(0, cli.rawArgs().length);
    }

    @Test
    public void positionalProjectPath() throws IOException {
        File project = makeCayenneFile("cayenne-foo.xml");
        CliArgs cli = CliArgs.parse(new String[]{project.getAbsolutePath()});
        assertEquals(project, cli.initialProject());
        assertNull(cli.mcpHandshakeNonce());
    }

    @Test
    public void positionalPathRejectedWhenNotCayenneXml() throws IOException {
        File f = Files.createFile(tmp.resolve("not-a-cayenne-project.xml")).toFile();
        CliArgs cli = CliArgs.parse(new String[]{f.getAbsolutePath()});
        assertNull(cli.initialProject());
    }

    @Test
    public void positionalPathRejectedWhenNotXml() throws IOException {
        File f = Files.createFile(tmp.resolve("cayenne-foo.txt")).toFile();
        CliArgs cli = CliArgs.parse(new String[]{f.getAbsolutePath()});
        assertNull(cli.initialProject());
    }

    @Test
    public void positionalPathRejectedWhenFileMissing() {
        CliArgs cli = CliArgs.parse(new String[]{tmp.resolve("cayenne-missing.xml").toString()});
        assertNull(cli.initialProject());
    }

    @Test
    public void handshakeOnly() {
        CliArgs cli = CliArgs.parse(new String[]{"--mcp-handshake", "abc123"});
        assertEquals("abc123", cli.mcpHandshakeNonce());
        assertNull(cli.initialProject());
    }

    @Test
    public void handshakeThenProject() throws IOException {
        File project = makeCayenneFile("cayenne-foo.xml");
        CliArgs cli = CliArgs.parse(new String[]{
                "--mcp-handshake", "abc123", project.getAbsolutePath()});
        assertEquals("abc123", cli.mcpHandshakeNonce());
        assertEquals(project, cli.initialProject());
    }

    @Test
    public void projectThenHandshake() throws IOException {
        File project = makeCayenneFile("cayenne-foo.xml");
        CliArgs cli = CliArgs.parse(new String[]{
                project.getAbsolutePath(), "--mcp-handshake", "abc123"});
        assertEquals("abc123", cli.mcpHandshakeNonce());
        assertEquals(project, cli.initialProject());
    }

    @Test
    public void handshakeWithoutValueIgnored() {
        CliArgs cli = CliArgs.parse(new String[]{"--mcp-handshake"});
        assertNull(cli.mcpHandshakeNonce());
        assertNull(cli.initialProject());
    }

    @Test
    public void unknownFlagIgnored() throws IOException {
        File project = makeCayenneFile("cayenne-foo.xml");
        CliArgs cli = CliArgs.parse(new String[]{
                "--bogus-flag", project.getAbsolutePath()});
        assertEquals(project, cli.initialProject());
        assertNull(cli.mcpHandshakeNonce());
    }

    @Test
    public void rawArgsPreserved() {
        String[] in = {"--mcp-handshake", "n", "/some/path"};
        CliArgs cli = CliArgs.parse(in);
        assertEquals(3, cli.rawArgs().length);
        assertEquals("--mcp-handshake", cli.rawArgs()[0]);
        assertEquals("n", cli.rawArgs()[1]);
        assertEquals("/some/path", cli.rawArgs()[2]);
    }

    private File makeCayenneFile(String name) throws IOException {
        return Files.createFile(tmp.resolve(name)).toFile();
    }
}
