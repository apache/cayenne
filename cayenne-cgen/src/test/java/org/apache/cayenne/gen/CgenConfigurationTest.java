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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CgenConfigurationTest {

    /**
     * No root and a relative output path: nothing to resolve the output against, so no output directory
     * has been configured at all.
     */
    @Test
    public void relativeOutputDirWithoutRootIsNotConfigured() {
        CgenConfiguration configuration = new CgenConfiguration();
        configuration.setOutputDir(Paths.get("out"));

        assertTrue(configuration.outputDirectory().isEmpty());
        assertThrows(ValidationException.class, configuration::requireOutputDirectory);
    }

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

            // src/main/resources -> src/main/java, derived from the map's own directory
            CgenConfiguration config = CgenConfiguration.createDefault(savedMap(mapFile), null);

            assertEquals(resources, config.getRootPath());
            assertEquals(tmp.resolve("src/main/java"), config.requireOutputDirectory());
        }

        @Test
        public void explicitOutputDir(@TempDir Path tmp) throws IOException {
            Path mapFile = Files.createFile(tmp.resolve("test.map.xml"));
            Path outputDir = tmp.resolve("custom/output");

            CgenConfiguration config = CgenConfiguration.createDefault(savedMap(mapFile), outputDir);

            assertEquals(outputDir, config.requireOutputDirectory());
        }

        /**
         * The Import DataMap state: the map has a configuration source but no output dir was supplied,
         * and the layout is not a Maven one. Used to leave the config with a root and no output path at
         * all, which made the effective output directory null.
         */
        @Test
        public void savedMapWithoutOutputDirFallsBackToMapDir(@TempDir Path tmp) throws IOException {
            Path mapFile = Files.createFile(tmp.resolve("test.map.xml"));

            CgenConfiguration config = CgenConfiguration.createDefault(savedMap(mapFile), null);

            assertEquals(tmp, config.getRootPath());
            assertEquals(tmp, config.requireOutputDirectory());
            assertEquals(".", config.encodedDestDir());
        }

        @Test
        public void unsavedMapSkipsRootPath() {
            CgenConfiguration config = CgenConfiguration.createDefault(unsavedMap(), null);

            assertTrue(config.outputDirectory().isEmpty());
            assertThrows(ValidationException.class, config::requireOutputDirectory);
            // artifacts still populated even without a saved location
            assertTrue(config.getEntities().contains("Person"));
            assertTrue(config.getEmbeddables().contains("com.example.Address"));
        }
    }

    /**
     * Setting the root path and the output directory are independent writes; neither reads the other, so
     * the order in which they happen must not change the outcome. The XML loader sets them in one order
     * (destDir during SAX parsing, root from a later DataMap callback) and everything else in the other.
     */
    @Nested
    public class OrderIndependenceTest {

        private CgenConfiguration rootFirst(Path root, Path out) {
            CgenConfiguration config = new CgenConfiguration();
            config.setRootPath(root);
            config.setOutputDir(out);
            return config;
        }

        private CgenConfiguration outputFirst(Path root, Path out) {
            CgenConfiguration config = new CgenConfiguration();
            config.setOutputDir(out);
            config.setRootPath(root);
            return config;
        }

        private void assertCommutes(Path root, Path out) {
            CgenConfiguration a = rootFirst(root, out);
            CgenConfiguration b = outputFirst(root, out);

            assertEquals(a.outputDirectory(), b.outputDirectory());
            assertEquals(a.encodedDestDir(), b.encodedDestDir());
        }

        @Test
        public void absoluteUnderRoot(@TempDir Path tmp) {
            assertCommutes(tmp, tmp.resolve("out"));
        }

        @Test
        public void absoluteOutsideRoot(@TempDir Path tmp) {
            assertCommutes(tmp.resolve("project"), tmp.resolve("elsewhere/out"));
        }

        @Test
        public void relative(@TempDir Path tmp) {
            assertCommutes(tmp, Paths.get("../java"));
        }

        @Test
        public void empty(@TempDir Path tmp) {
            assertCommutes(tmp, Paths.get(""));
        }
    }

    /**
     * {@code destDir} is what actually lands in the DataMap XML, so it has to survive an arbitrary number
     * of save/load cycles unchanged.
     */
    @Nested
    public class DestDirRoundTripTest {

        private void assertRoundTrips(Path root, String destDir) {
            CgenConfiguration loaded = new CgenConfiguration();
            loaded.setOutputDir(Paths.get(destDir));
            loaded.setRootPath(root);

            String encoded = loaded.encodedDestDir();

            CgenConfiguration reloaded = new CgenConfiguration();
            reloaded.setOutputDir(Paths.get(encoded));
            reloaded.setRootPath(root);

            assertEquals(encoded, reloaded.encodedDestDir());
            assertEquals(loaded.outputDirectory(), reloaded.outputDirectory());
        }

        @Test
        public void currentDir(@TempDir Path tmp) {
            assertRoundTrips(tmp, ".");
        }

        @Test
        public void parentDir(@TempDir Path tmp) {
            assertRoundTrips(tmp, "../java");
        }

        @Test
        public void grandParentDir(@TempDir Path tmp) {
            assertRoundTrips(tmp, "../../java");
        }

        @Test
        public void plainChildDir(@TempDir Path tmp) {
            assertRoundTrips(tmp, "cgenConfigTest");
        }

        @Test
        public void emptyOutputPathEncodesAsCurrentDir(@TempDir Path tmp) {
            CgenConfiguration config = new CgenConfiguration();
            config.setRootPath(tmp);

            assertEquals(".", config.encodedDestDir());
            assertEquals(tmp, config.requireOutputDirectory());
        }
    }

    /**
     * Rebasing follows the project to a new location while the generated classes keep going to the same
     * physical directory. A configuration that never had an output directory keeps following the root.
     */
    @Nested
    public class RebaseTest {

        @Test
        public void relativeOutputKeepsPointingAtTheSameDir(@TempDir Path tmp) {
            Path oldRoot = tmp.resolve("old");
            Path newRoot = tmp.resolve("new");

            CgenConfiguration config = new CgenConfiguration();
            config.setRootPath(oldRoot);
            config.setOutputDir(Paths.get("../java"));

            config.rebase(newRoot);

            assertEquals(newRoot, config.getRootPath());
            assertEquals(tmp.resolve("java"), config.requireOutputDirectory());
            assertEquals("../java", config.encodedDestDir());
        }

        @Test
        public void absoluteOutputSurvives(@TempDir Path tmp) {
            Path target = tmp.resolve("generated");

            CgenConfiguration config = new CgenConfiguration();
            config.setRootPath(tmp.resolve("old"));
            config.setOutputDir(target);

            config.rebase(tmp.resolve("new"));

            assertEquals(target, config.requireOutputDirectory());
        }

        @Test
        public void neverConfiguredFollowsTheRoot(@TempDir Path tmp) {
            Path newRoot = tmp.resolve("new");

            CgenConfiguration config = new CgenConfiguration();
            config.rebase(newRoot);

            assertEquals(newRoot, config.requireOutputDirectory());
            assertEquals(".", config.encodedDestDir());
        }

        @Test
        public void rejectsRelativeRoot() {
            CgenConfiguration config = new CgenConfiguration();
            assertThrows(ValidationException.class, () -> config.rebase(Paths.get("relative")));
        }
    }

    /**
     * Windows has more than one filesystem root - drive letters and UNC shares - and {@code relativize()}
     * throws across them. Output directories that cannot be relativized against the project root must be
     * stored and persisted absolute, unchanged. See CAY-2615 and CAY-2710.
     */
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
            Path outputDir = Paths.get("C:\\test1\\test2\\test3");
            configuration.setOutputDir(outputDir);

            assertEquals(".", configuration.encodedDestDir());
            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void equalRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path outputDir = Paths.get("C:\\test1\\test2\\testAnother");
            configuration.setOutputDir(outputDir);

            assertEquals("../testAnother", configuration.encodedDestDir());
            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void equalRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("C:\\"));
            Path outputDir = Paths.get("C:\\");
            configuration.setOutputDir(outputDir);

            assertEquals(".", configuration.encodedDestDir());
            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void notEqualRootsEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path outputDir = Paths.get("E:\\test1\\test2\\test3");
            configuration.setOutputDir(outputDir);

            assertEquals("E:/test1/test2/test3", configuration.encodedDestDir());
            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void notEqualRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path outputDir = Paths.get("E:\\test1\\test2\\testAnother");
            configuration.setOutputDir(outputDir);

            assertEquals("E:/test1/test2/testAnother", configuration.encodedDestDir());
            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void notEqualRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("C:\\"));
            Path outputDir = Paths.get("E:\\");
            configuration.setOutputDir(outputDir);

            assertEquals("E:/", configuration.encodedDestDir());
            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        /**
         * UNC shares are separate filesystem roots too: same share relativizes, different shares don't.
         */
        @Test
        public void uncShares() {
            configuration.setRootPath(Paths.get("\\\\server\\share\\project"));
            configuration.setOutputDir(Paths.get("\\\\server\\share\\java"));
            assertEquals("../java", configuration.encodedDestDir());

            configuration.setOutputDir(Paths.get("\\\\server\\other\\java"));
            assertEquals("//server/other/java", configuration.encodedDestDir());
            assertEquals(Paths.get("\\\\server\\other\\java"), configuration.requireOutputDirectory());
        }

        /**
         * A drive-relative path such as {@code C:foo} carries a root but is not absolute, so it cannot be
         * relativized against an absolute root - {@code relativize()} would throw "different type of Path".
         */
        @Test
        public void driveRelativeOutputDirIsNotRelativized() {
            configuration.setRootPath(Paths.get("C:\\test1"));
            configuration.setOutputDir(Paths.get("C:foo"));

            assertEquals("C:foo", configuration.encodedDestDir());
            assertEquals(Paths.get("C:\\test1\\foo"), configuration.requireOutputDirectory());
        }

        @Test
        public void emptyRootNotEmptyRelPath() {
            assertThrows(ValidationException.class, () -> configuration.setRootPath(Paths.get("")));
        }

        @Test
        public void notEmptyRootEmptyRelPath() {
            configuration.setRootPath(Paths.get("E:\\"));
            configuration.setOutputDir(Paths.get(""));

            assertEquals(".", configuration.encodedDestDir());
            assertEquals(Paths.get("E:\\"), configuration.requireOutputDirectory());
        }

        @Test
        public void invalidRootPath() {
            assertThrows(InvalidPathException.class, () -> configuration.setRootPath(Paths.get("invalidRoot:\\test")));
        }

        @Test
        public void nullRootPath() {
            configuration.setOutputDir(Path.of("C:\\test1\\test2\\test3"));

            assertEquals("C:/test1/test2/test3", configuration.encodedDestDir());
            assertEquals(Paths.get("C:\\test1\\test2\\test3"), configuration.requireOutputDirectory());
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
            Path outputDir = Paths.get("/test1/test2/test3");
            configuration.setOutputDir(outputDir);

            assertEquals(".", configuration.encodedDestDir());
            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void equalRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            Path outputDir = Paths.get("/test1/test2/testAnother");
            configuration.setOutputDir(outputDir);

            assertEquals("../testAnother", configuration.encodedDestDir());
            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void equalRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("/"));
            Path outputDir = Paths.get("/");
            configuration.setOutputDir(outputDir);

            assertEquals(".", configuration.encodedDestDir());
            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void concatCorrectRootPathAndRelPath() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            configuration.setOutputDir(Paths.get("test1/test2/test3"));

            assertEquals("test1/test2/test3", configuration.encodedDestDir());
            assertEquals(Paths.get("/test1/test2/test3/test1/test2/test3"), configuration.requireOutputDirectory());
        }

        @Test
        public void emptyRootNotEmptyRelPath() {
            assertThrows(ValidationException.class, () -> configuration.setRootPath(Paths.get("")));
        }

        @Test
        public void notEmptyRootEmptyRelPath() {
            configuration.setRootPath(Paths.get("/"));
            configuration.setOutputDir(Paths.get(""));

            assertEquals(".", configuration.encodedDestDir());
            assertEquals(Paths.get("/"), configuration.requireOutputDirectory());
        }

        @Test
        public void invalidRootPath() {
            assertThrows(ValidationException.class, () -> configuration.setRootPath(Paths.get("invalidRoot:/test")));
        }

        @Test
        public void nullRootPath() {
            configuration.setOutputDir(Paths.get("/test1/test2/test3"));

            assertEquals("/test1/test2/test3", configuration.encodedDestDir());
            assertEquals(Paths.get("/test1/test2/test3"), configuration.requireOutputDirectory());
        }
    }
}
