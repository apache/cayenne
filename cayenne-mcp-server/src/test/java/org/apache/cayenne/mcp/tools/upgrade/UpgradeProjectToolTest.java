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

package org.apache.cayenne.mcp.tools.upgrade;

import org.apache.cayenne.mcp.tools.upgrade.protocol.UpgradeErrorCode;
import org.apache.cayenne.mcp.tools.upgrade.protocol.UpgradeProjectResult;
import org.apache.cayenne.project.upgrade.UpgradeChange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpgradeProjectToolTest {

    // DI bootstrap is expensive, so the tool is shared by the tests
    private static UpgradeProjectTool tool;

    @TempDir
    Path tempDir;

    private Path projectFile;
    private Path mapFile;
    private Path graphFile;

    @BeforeAll
    public static void createTool() {
        tool = new UpgradeProjectTool();
    }

    @BeforeEach
    public void copyProject() throws IOException {
        projectFile = copy("cayenne-project1.xml");
        mapFile = copy("map1.map.xml");
        graphFile = copy("project1.graph.xml");
    }

    @Test
    public void projectNotFound() {
        UpgradeProjectResult result = tool.run("/no/such/file/cayenne-project.xml");
        assertEquals("error", result.status());
        assertEquals(UpgradeErrorCode.project_not_found, result.error().code());
    }

    @Test
    public void intermediateUpgradeNeeded() throws IOException {
        Path old = tempDir.resolve("cayenne-old.xml");
        Files.writeString(old, "<domain project-version=\"8\"/>");

        UpgradeProjectResult result = tool.run(old.toString());
        assertEquals("error", result.status());
        assertEquals("8", result.fromVersion());
        assertEquals(UpgradeErrorCode.intermediate_upgrade_needed, result.error().code());
        assertTrue(result.error().message().contains("Cayenne 4.2"), result.error().message());
    }

    @Test
    public void downgradeNeeded() throws IOException {
        Path newer = tempDir.resolve("cayenne-new.xml");
        Files.writeString(newer, "<project xmlns=\"http://cayenne.apache.org/schema/99/project\"/>");

        UpgradeProjectResult result = tool.run(newer.toString());
        assertEquals("error", result.status());
        assertEquals(UpgradeErrorCode.downgrade_needed, result.error().code());
    }

    @Test
    public void upgrade() throws IOException {
        UpgradeProjectResult result = tool.run(projectFile.toString());

        assertEquals("upgraded", result.status());
        assertChanges(result);

        assertTrue(Files.readString(projectFile).contains("http://cayenne.apache.org/schema/15/project"));
        assertTrue(Files.readString(mapFile).contains("http://cayenne.apache.org/schema/15/dataMap"));
        assertFalse(Files.exists(graphFile), "graph file must be deleted");

        UpgradeProjectResult secondRun = tool.run(projectFile.toString());
        assertEquals("up_to_date", secondRun.status());
        assertEquals("15", secondRun.fromVersion());
        assertTrue(secondRun.changes().isEmpty());
        assertNull(secondRun.error());
    }

    private void assertChanges(UpgradeProjectResult result) {
        assertNull(result.error());
        assertEquals("11", result.fromVersion());
        assertEquals("15", result.toVersion());

        List<UpgradeChange> destructive = result.changes().stream()
                .filter(UpgradeChange::affectsRuntime)
                .toList();
        assertEquals(1, destructive.size(), result.changes().toString());

        UpgradeChange nodeRemoval = destructive.getFirst();
        assertTrue(nodeRemoval.message().contains("DataNode 'node1'"), nodeRemoval.message());
        assertFalse(nodeRemoval.details().isEmpty());
    }

    private Path copy(String name) throws IOException {
        Path target = tempDir.resolve(name);
        try (InputStream in = getClass().getResourceAsStream(name)) {
            Files.copy(in, target);
        }
        return target;
    }
}
