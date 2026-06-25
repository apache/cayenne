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

package org.apache.cayenne.gen;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.validation.ValidationException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CgenConfigurationTest {

    @Nested
    public class CreateDefaultTest {

        private DataMap savedMap(Path mapFile) throws MalformedURLException {
            DataMap map = unsavedMap();
            map.setConfigurationSource(new URLResource(mapFile.toUri().toURL()));
            return map;
        }

        private DataMap unsavedMap() {
            DataMap map = new DataMap("test");

            ObjEntity person = new ObjEntity("Person");
            person.setClassName("com.example.Person");
            map.addObjEntity(person);

            // generic entity (no class name) — must be excluded from generation
            map.addObjEntity(new ObjEntity("Generic"));

            map.addEmbeddable(new Embeddable("com.example.Address"));
            return map;
        }

        @Test
        public void populatesNonGenericArtifacts(@TempDir Path tmp) throws IOException {
            Path mapFile = Files.createFile(tmp.resolve("test.map.xml"));
            CgenConfiguration config = CgenConfiguration.createDefault(savedMap(mapFile), tmp);

            assertTrue(config.getEntities().contains("Person"));
            assertFalse(config.getEntities().contains("Generic"), "Generic entity must be skipped");
            assertTrue(config.getEmbeddables().contains("com.example.Address"));
        }

        @Test
        public void derivesMavenOutputDir(@TempDir Path tmp) throws IOException {
            Path resources = Files.createDirectories(tmp.resolve("src/main/resources"));
            Path mapFile = Files.createFile(resources.resolve("test.map.xml"));
            DataMap map = savedMap(mapFile);

            // src/main/resources -> src/main/java
            Path outputDir = CgenConfiguration.defaultOutputDir(map);
            assertEquals(tmp.resolve("src/main/java"), outputDir);

            CgenConfiguration config = CgenConfiguration.createDefault(map, outputDir);
            assertEquals(resources, config.getRootPath());
            assertEquals(tmp.resolve("src/main/java"), config.buildOutputPath());
        }

        @Test
        public void explicitOutputDir(@TempDir Path tmp) throws IOException {
            Path mapFile = Files.createFile(tmp.resolve("test.map.xml"));
            Path outputDir = tmp.resolve("custom/output");

            CgenConfiguration config = CgenConfiguration.createDefault(savedMap(mapFile), outputDir);

            assertEquals(outputDir, config.buildOutputPath());
        }

        @Test
        public void unsavedMapSkipsRootPath() {
            CgenConfiguration config = CgenConfiguration.createDefault(unsavedMap(), null);

            assertNull(config.getRootPath());
            assertNull(config.buildOutputPath());
            // artifacts still populated even without a saved location
            assertTrue(config.getEntities().contains("Person"));
            assertTrue(config.getEmbeddables().contains("com.example.Address"));
        }
    }

    @Nested
    public class CgenWindowsConfigurationTest {

        CgenConfiguration configuration;

        @BeforeEach
        public void setUp() {
            configuration = new CgenConfiguration();
        }

        @BeforeEach
        public void checkPlatform() {
            Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win"));
        }

        @Test
        public void equalRootsEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path relPath = Paths.get("C:\\test1\\test2\\test3");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get(""), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void equalRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path relPath = Paths.get("C:\\test1\\test2\\testAnother");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("..\\testAnother"), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void equalRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("C:\\"));
            Path relPath = Paths.get("C:\\");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get(""), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void notEqualRootsEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path relPath = Paths.get("E:\\test1\\test2\\test3");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("E:\\test1\\test2\\test3"), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void notEqualRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path relPath = Paths.get("E:\\test1\\test2\\testAnother");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("E:\\test1\\test2\\testAnother"), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void notEqualRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("C:\\"));
            Path relPath = Paths.get("E:\\");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("E:\\"), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void emptyRootNotEmptyRelPath() {
            Path relPath = Paths.get("E:\\");
            assertThrows(ValidationException.class, () -> {
                configuration.setRootPath(Paths.get(""));
                configuration.updateOutputPath(relPath);
            });
        }

        @Test
        public void notEmptyRootEmptyRelPath() {
            configuration.setRootPath(Paths.get("E:\\"));
            Path relPath = Paths.get("");

            configuration.updateOutputPath(relPath);

            assertEquals(relPath, configuration.getRawOutputPath());
            assertEquals(Paths.get("E:\\"), configuration.buildOutputPath());
        }

        @Test
        public void invalidRootPath() {
            assertThrows(InvalidPathException.class, () -> configuration.setRootPath(Paths.get("invalidRoot:\\test")));
        }

        @Test
        public void nullRootPath() {
            configuration.updateOutputPath(Path.of("C:\\test1\\test2\\test3"));
            assertEquals(Paths.get("C:\\test1\\test2\\test3"), configuration.getRawOutputPath());
            assertEquals(Paths.get("C:\\test1\\test2\\test3"), configuration.buildOutputPath());
        }
    }

    @Nested
    public class CgenUnixConfigurationTest {

        CgenConfiguration configuration;

        @BeforeEach
        public void setUp() {
            configuration = new CgenConfiguration();
        }

        @BeforeEach
        public void checkPlatform() {
            Assumptions.assumeFalse(System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win"));
        }

        @Test
        public void equalRootsEqualDirectories() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            Path relPath = Paths.get("/test1/test2/test3");
            configuration.updateOutputPath(relPath);


            assertEquals(Paths.get(""), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void equalRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            Path relPath = Paths.get("/test1/test2/testAnother");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("../testAnother"), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void equalRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("/"));
            Path relPath = Paths.get("/");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get(""), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void concatCorrectRootPathAndRelPath() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            Path relPath = Paths.get("test1/test2/test3");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("test1/test2/test3"), configuration.getRawOutputPath());
            assertEquals(Paths.get("/test1/test2/test3/test1/test2/test3"), configuration.buildOutputPath());
        }

        @Test
        public void emptyRootNotEmptyRelPath() {
            Path relPath = Paths.get("/");
            assertThrows(ValidationException.class, () -> {
                configuration.setRootPath(Paths.get(""));
                configuration.updateOutputPath(relPath);
            });
        }

        @Test
        public void notEmptyRootEmptyRelPath() {
            configuration.setRootPath(Paths.get("/"));
            configuration.updateOutputPath(Paths.get(""));

            assertEquals(Paths.get(""), configuration.getRawOutputPath());
            assertEquals(Paths.get("/"), configuration.buildOutputPath());
        }

        @Test
        public void invalidRootPath() {
            assertThrows(ValidationException.class, () -> {
                configuration.setRootPath(Paths.get("invalidRoot:/test"));
                configuration.updateOutputPath(Paths.get("/test1/test2/test3"));
            });
        }

        @Test
        public void concatInvalidRootPathAndRelPath() {
            assertThrows(ValidationException.class, () -> {
                configuration.setRootPath(Paths.get("invalidRoot:/test"));
                configuration.updateOutputPath(Paths.get("test1/test2/test3"));
            });
        }

        @Test
        public void nullRootPath() {
            configuration.updateOutputPath(Paths.get("/test1/test2/test3"));
            assertEquals(Paths.get("/test1/test2/test3"), configuration.getRawOutputPath());
            assertEquals(Paths.get("/test1/test2/test3"), configuration.buildOutputPath());
        }
    }

}
