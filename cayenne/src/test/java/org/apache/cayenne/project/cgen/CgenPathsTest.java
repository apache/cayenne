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

package org.apache.cayenne.project.cgen;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CgenPathsTest {

    private static Path absolute(String path) {
        return Path.of(path).toAbsolutePath();
    }

    private static Path resolve(Path root, String destDir) {
        return root.resolve(destDir).normalize();
    }

    private static CgenDescriptor addCgenDescriptor(DataMap dataMap, String destDir) {
        CgenDescriptor descriptor = new CgenDescriptor();
        descriptor.setName("cgen" + dataMap.getCgenDescriptors().size());
        descriptor.setDestDir(destDir);
        dataMap.addCgenDescriptor(descriptor);
        return descriptor;
    }

    /**
     * An unset output directory keeps pointing at the old root.
     */
    @Test
    public void rebase_ExistingRootOverride() {
        Path newRoot = absolute("/tmp/src/main/resources");

        String destDir = CgenPaths.rebase(null, absolute("/tmp/src/main/java"), newRoot);

        assertEquals("../java", destDir);
        assertEquals(absolute("/tmp/src/main/java"), resolve(newRoot, destDir));
    }

    @Test
    public void rebase_ExistingRootAndRelPath() {
        Path newRoot = absolute("/tmp/src/main/resources");

        String destDir = CgenPaths.rebase("", absolute("/tmp/src/main/java"), newRoot);

        assertEquals("../java", destDir);
        assertEquals(absolute("/tmp/src/main/java"), resolve(newRoot, destDir));
    }

    /**
     * With no old root, the output directory is the Maven source directory.
     */
    @Test
    public void rebase_EmptyRootInMavenTree() {
        Path newRoot = absolute("/tmp/src/main/resources");

        String destDir = CgenPaths.rebase(null, null, newRoot);

        assertEquals("../java", destDir);
        assertEquals(absolute("/tmp/src/main/java"), resolve(newRoot, destDir));
    }

    @Test
    public void rebase_EmptyRootInMavenTestTree() {
        Path newRoot = absolute("/tmp/src/test/resources");

        assertEquals("../java", CgenPaths.rebase(null, null, newRoot));
    }

    /**
     * With no old root and outside a Maven tree, the output directory follows the new root.
     */
    @Test
    public void rebase_EmptyRoot() {
        Path newRoot = absolute("/tmp/somefolder");

        String destDir = CgenPaths.rebase(null, null, newRoot);

        assertEquals(".", destDir);
        assertEquals(absolute("/tmp/somefolder"), resolve(newRoot, destDir));
    }

    /**
     * With no old root, a configured relative output directory stays relative to the new root.
     */
    @Test
    public void rebase_RelativeWithoutRoot() {
        assertEquals("gen", CgenPaths.rebase("gen", null, absolute("/tmp/somefolder")));
        assertEquals("gen", CgenPaths.rebase("gen", null, absolute("/tmp/src/main/resources")));
        assertEquals("../generated", CgenPaths.rebase("../generated", null, absolute("/tmp/src/main/resources")));
        assertEquals(".", CgenPaths.rebase(".", null, absolute("/tmp/src/main/resources")));
        assertEquals("../java", CgenPaths.rebase("", null, absolute("/tmp/src/main/resources")));
    }

    @Test
    public void rebase_AbsoluteOutputDirSurvives() {
        Path newRoot = absolute("/tmp/other/project");

        String destDir = CgenPaths.rebase(absolute("/tmp/generated").toString(), absolute("/tmp/project"), newRoot);

        assertEquals("../../generated", destDir);
        assertEquals(absolute("/tmp/generated"), resolve(newRoot, destDir));
    }

    @Test
    public void rebase_RelativeOutputDirSurvives() {
        Path newRoot = absolute("/tmp/other/project");

        String destDir = CgenPaths.rebase("../java", absolute("/tmp/project/resources"), newRoot);

        assertEquals("../../project/java", destDir);
        assertEquals(absolute("/tmp/project/java"), resolve(newRoot, destDir));
    }

    @Test
    public void rebase_RelativeOutputDirToSibling() {
        Path newRoot = absolute("/tmp/new");

        String destDir = CgenPaths.rebase("../java", absolute("/tmp/old"), newRoot);

        assertEquals("../java", destDir);
        assertEquals(absolute("/tmp/java"), resolve(newRoot, destDir));
    }

    /**
     * Rebasing to the same root normalizes the output directory.
     */
    @Test
    public void rebase_SameRoot() {
        Path root = absolute("/tmp/project");

        assertEquals("../java", CgenPaths.rebase("../java/", root, root));
        assertEquals("out", CgenPaths.rebase(absolute("/tmp/project/out").toString(), root, root));
        assertEquals(".", CgenPaths.rebase(".", root, root));
        assertEquals(".", CgenPaths.rebase(null, root, root));
    }

    @Test
    public void rebase_SameRootRoundTrip() {
        Path root = absolute("/tmp/project");

        for (String destDir : new String[]{".", "../java", "../../java", "cgenConfigTest"}) {
            String saved = CgenPaths.rebase(destDir, root, root);
            assertEquals(destDir, saved);
            assertEquals(saved, CgenPaths.rebase(saved, root, root));
            assertEquals(resolve(root, destDir), resolve(root, saved));
        }
    }

    @Test
    public void encode() {
        Path root = absolute("/tmp/project");

        assertEquals(".", CgenPaths.encode(root, root));
        assertEquals(".", CgenPaths.encode(Path.of(""), root));
        assertEquals("out", CgenPaths.encode(root.resolve("out"), root));
        assertEquals("../java", CgenPaths.encode(absolute("/tmp/java"), root));
        assertEquals("../java", CgenPaths.encode(Path.of("..", "java"), root));
    }

    @Test
    public void encode_NoRoot() {
        assertEquals(".", CgenPaths.encode(Path.of(""), null));
        assertEquals("../java", CgenPaths.encode(Path.of("..", "java"), null));
        assertEquals(absolute("/tmp/java").toString().replace('\\', '/'),
                CgenPaths.encode(absolute("/tmp/java"), null));
    }

    @Test
    public void mavenSourceDir() {
        Path project = absolute("/tmp/project");

        assertEquals(Optional.of(project.resolve(Path.of("src", "main", "java"))),
                CgenPaths.mavenSourceDir(project.resolve(Path.of("src", "main", "resources", "maps"))));
        assertEquals(Optional.of(project.resolve(Path.of("src", "test", "java"))),
                CgenPaths.mavenSourceDir(project.resolve(Path.of("src", "test", "resources"))));
        assertEquals(Optional.empty(), CgenPaths.mavenSourceDir(project));
    }

    @Test
    public void dataMapDir(@TempDir Path tempDir) throws Exception {
        Path mapFile = Files.createFile(tempDir.resolve("test.map.xml"));
        DataMap dataMap = new DataMap("test");
        dataMap.setConfigurationSource(new URLResource(mapFile.toUri().toURL()));

        assertEquals(Optional.of(tempDir), CgenPaths.dataMapDir(dataMap));
    }

    @Test
    public void dataMapDir_MissingFile(@TempDir Path tempDir) throws Exception {
        DataMap dataMap = new DataMap("test");
        dataMap.setConfigurationSource(new URLResource(tempDir.resolve("missing.map.xml").toUri().toURL()));

        assertEquals(Optional.of(tempDir), CgenPaths.dataMapDir(dataMap));
    }

    @Test
    public void dataMapDir_NotSaved() {
        assertTrue(CgenPaths.dataMapDir(new DataMap("test")).isEmpty());
    }

    @Test
    public void dataMapDir_NotAFile() throws Exception {
        DataMap dataMap = new DataMap("test");
        dataMap.setConfigurationSource(new URLResource(URI.create("jar:file:/tmp/maps.jar!/test.map.xml").toURL()));
        assertTrue(CgenPaths.dataMapDir(dataMap).isEmpty());

        dataMap.setConfigurationSource(new URLResource(new URL("jar:file:/tmp/my maps.jar!/my map.map.xml")));
        assertTrue(CgenPaths.dataMapDir(dataMap).isEmpty());
    }

    /**
     * Path.of(..) can resolve a "jar:" URL once a file system is open for the jar.
     */
    @Test
    public void dataMapDir_JarWithOpenFileSystem(@TempDir Path tempDir) throws Exception {
        URI jar = URI.create("jar:" + tempDir.resolve("maps.jar").toUri());
        try (FileSystem fileSystem = FileSystems.newFileSystem(jar, Map.of("create", "true"))) {
            URI location = URI.create(jar + "!/maps/test.map.xml");
            assertEquals(fileSystem.getPath("/maps/test.map.xml"), Path.of(location));

            DataMap dataMap = new DataMap("test");
            dataMap.setConfigurationSource(new URLResource(location.toURL()));
            assertTrue(CgenPaths.dataMapDir(dataMap).isEmpty());
        }
    }

    /**
     * The spaces in the location of a DataMap may be unencoded.
     */
    @Test
    public void dataMapDir_Spaces(@TempDir Path tempDir) throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("my maps"));
        URL encoded = dir.resolve("my map.map.xml").toUri().toURL();
        URL unencoded = new URL(encoded.toExternalForm().replace("%20", " "));
        URL resolved = new URLResource(dir.toUri().toURL()).getRelativeResource("my map.map.xml").getURL();
        assertTrue(encoded.toExternalForm().endsWith("/my%20maps/my%20map.map.xml"), encoded.toExternalForm());
        assertTrue(unencoded.toExternalForm().endsWith("/my maps/my map.map.xml"), unencoded.toExternalForm());
        assertTrue(resolved.toExternalForm().endsWith("/my%20maps/my map.map.xml"), resolved.toExternalForm());

        for (URL url : List.of(encoded, unencoded, resolved)) {
            DataMap dataMap = new DataMap("my map");
            dataMap.setConfigurationSource(new URLResource(url));
            assertEquals(Optional.of(dir), CgenPaths.dataMapDir(dataMap), url.toExternalForm());
        }
    }

    /**
     * As done by CayenneModeler when a DataMap is imported into a project.
     */
    @Test
    public void moveDataMap() throws Exception {
        DataMap dataMap = new DataMap("ext");
        dataMap.setConfigurationSource(new URLResource(absolute("/tmp/other/res/ext.map.xml").toUri().toURL()));
        CgenDescriptor relative = addCgenDescriptor(dataMap, "../gen");
        CgenDescriptor absoluteDir = addCgenDescriptor(dataMap, absolute("/tmp/abs").toString());
        CgenDescriptor notSet = addCgenDescriptor(dataMap, null);

        URLResource newLocation = new URLResource(absolute("/tmp/proj/ext.map.xml").toUri().toURL());
        CgenPaths.moveDataMap(dataMap, newLocation);

        assertSame(newLocation, dataMap.getConfigurationSource());
        assertEquals("../other/gen", relative.getDestDir());
        assertEquals("../abs", absoluteDir.getDestDir());
        assertEquals("../other/res", notSet.getDestDir());

        Path newRoot = CgenPaths.dataMapDir(dataMap).orElseThrow();
        assertEquals(absolute("/tmp/other/gen"), resolve(newRoot, relative.getDestDir()));
        assertEquals(absolute("/tmp/abs"), resolve(newRoot, absoluteDir.getDestDir()));
        assertEquals(absolute("/tmp/other/res"), resolve(newRoot, notSet.getDestDir()));
    }

    @Test
    public void moveDataMap_NotSaved() throws Exception {
        DataMap dataMap = new DataMap("ext");
        CgenDescriptor relative = addCgenDescriptor(dataMap, "gen");
        CgenDescriptor notSet = addCgenDescriptor(dataMap, null);

        URLResource newLocation = new URLResource(absolute("/tmp/proj/ext.map.xml").toUri().toURL());
        CgenPaths.moveDataMap(dataMap, newLocation);

        assertSame(newLocation, dataMap.getConfigurationSource());
        assertEquals("gen", relative.getDestDir());
        assertEquals(".", notSet.getDestDir());
    }

    @Test
    public void moveDataMap_NewLocationNotAFile() throws Exception {
        DataMap dataMap = new DataMap("ext");
        dataMap.setConfigurationSource(new URLResource(absolute("/tmp/other/res/ext.map.xml").toUri().toURL()));
        CgenDescriptor relative = addCgenDescriptor(dataMap, "../gen");

        URLResource newLocation = new URLResource(URI.create("jar:file:/tmp/maps.jar!/ext.map.xml").toURL());
        CgenPaths.moveDataMap(dataMap, newLocation);

        assertSame(newLocation, dataMap.getConfigurationSource());
        assertEquals("../gen", relative.getDestDir());
    }

    /**
     * Path.toUri() gives a "file:" URL with an authority for a Windows UNC path, which java.io.File rejects.
     */
    @Nested
    public class WindowsDataMapDirTest {

        @BeforeEach
        public void checkPlatform() {
            Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win"));
        }

        @Test
        public void uncPath() throws Exception {
            Path dir = Path.of("\\\\server\\share\\my maps");
            URL projectUrl = dir.resolve("cayenne-project.xml").toUri().toURL();
            URL dataMapUrl = new URLResource(projectUrl).getRelativeResource("my map.map.xml").getURL();

            DataMap dataMap = new DataMap("my map");
            dataMap.setConfigurationSource(new URLResource(dataMapUrl));
            assertEquals(Optional.of(dir), CgenPaths.dataMapDir(dataMap), dataMapUrl.toExternalForm());
        }
    }

    @Nested
    public class UnixDataMapDirTest {

        @BeforeEach
        public void checkPlatform() {
            Assumptions.assumeFalse(System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win"));
        }

        /**
         * A "file:" URL with an authority, the form of a Windows UNC path, names no local file here.
         */
        @Test
        public void authority() throws Exception {
            URL projectUrl = new URL("file://server/share/my%20maps/cayenne-project.xml");
            URL dataMapUrl = new URLResource(projectUrl).getRelativeResource("my map.map.xml").getURL();
            assertEquals("file://server/share/my%20maps/my map.map.xml", dataMapUrl.toExternalForm());

            DataMap dataMap = new DataMap("my map");
            dataMap.setConfigurationSource(new URLResource(dataMapUrl));
            assertTrue(CgenPaths.dataMapDir(dataMap).isEmpty());
        }
    }

    /**
     * Windows has more than one filesystem root - drive letters and UNC shares - and {@code relativize()} throws
     * across them. Output directories that cannot be relativized against the root are encoded absolute, unchanged.
     * See CAY-2615 and CAY-2710.
     */
    @Nested
    public class WindowsEncodeTest {

        @BeforeEach
        public void checkPlatform() {
            Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win"));
        }

        @Test
        public void equalRootsEqualDirectories() {
            assertEquals(".", CgenPaths.encode(Path.of("C:\\test1\\test2\\test3"),
                    Path.of("C:\\test1\\test2\\test3")));
        }

        @Test
        public void equalRootsNotEqualDirectories() {
            assertEquals("../testAnother", CgenPaths.encode(Path.of("C:\\test1\\test2\\testAnother"),
                    Path.of("C:\\test1\\test2\\test3")));
        }

        @Test
        public void equalRootsEmptyDirectories() {
            assertEquals(".", CgenPaths.encode(Path.of("C:\\"), Path.of("C:\\")));
        }

        @Test
        public void notEqualRootsEqualDirectories() {
            assertEquals("E:/test1/test2/test3", CgenPaths.encode(Path.of("E:\\test1\\test2\\test3"),
                    Path.of("C:\\test1\\test2\\test3")));
        }

        @Test
        public void notEqualRootsNotEqualDirectories() {
            assertEquals("E:/test1/test2/testAnother", CgenPaths.encode(Path.of("E:\\test1\\test2\\testAnother"),
                    Path.of("C:\\test1\\test2\\test3")));
        }

        @Test
        public void notEqualRootsEmptyDirectories() {
            assertEquals("E:/", CgenPaths.encode(Path.of("E:\\"), Path.of("C:\\")));
        }

        /**
         * UNC shares are separate filesystem roots too: same share relativizes, different shares don't.
         */
        @Test
        public void uncShares() {
            Path root = Path.of("\\\\server\\share\\project");
            assertEquals("../java", CgenPaths.encode(Path.of("\\\\server\\share\\java"), root));
            assertEquals("//server/other/java", CgenPaths.encode(Path.of("\\\\server\\other\\java"), root));
        }

        /**
         * A drive-relative path such as {@code C:foo} carries a root but is not absolute, so it cannot be
         * relativized against an absolute root - {@code relativize()} would throw "different type of Path".
         */
        @Test
        public void driveRelativeOutputDirIsNotRelativized() {
            assertEquals("C:foo", CgenPaths.encode(Path.of("C:foo"), Path.of("C:\\test1")));
        }

        @Test
        public void notEmptyRootEmptyRelPath() {
            assertEquals(".", CgenPaths.encode(Path.of(""), Path.of("E:\\")));
        }

        @Test
        public void nullRootPath() {
            assertEquals("C:/test1/test2/test3", CgenPaths.encode(Path.of("C:\\test1\\test2\\test3"), null));
        }
    }

    @Nested
    public class UnixEncodeTest {

        @BeforeEach
        public void checkPlatform() {
            Assumptions.assumeFalse(System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win"));
        }

        @Test
        public void equalRootsEqualDirectories() {
            assertEquals(".", CgenPaths.encode(Path.of("/test1/test2/test3"), Path.of("/test1/test2/test3")));
        }

        @Test
        public void equalRootsNotEqualDirectories() {
            assertEquals("../testAnother",
                    CgenPaths.encode(Path.of("/test1/test2/testAnother"), Path.of("/test1/test2/test3")));
        }

        @Test
        public void equalRootsEmptyDirectories() {
            assertEquals(".", CgenPaths.encode(Path.of("/"), Path.of("/")));
        }

        @Test
        public void relativeDirectory() {
            assertEquals("test1/test2/test3",
                    CgenPaths.encode(Path.of("test1/test2/test3"), Path.of("/test1/test2/test3")));
        }

        @Test
        public void notEmptyRootEmptyRelPath() {
            assertEquals(".", CgenPaths.encode(Path.of(""), Path.of("/")));
        }

        @Test
        public void nullRootPath() {
            assertEquals("/test1/test2/test3", CgenPaths.encode(Path.of("/test1/test2/test3"), null));
        }
    }
}
