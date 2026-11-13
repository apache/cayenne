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
import org.apache.cayenne.project.cgen.CgenDescriptor;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
            assertNull(config.getDescriptor().getDestDir());
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
     * A configuration of a DataMap uses the DataMap directory as the root path, unless one is set. A configuration
     * given a DataMap later, as by the build tool plugins, doesn't.
     */
    @Nested
    public class DataMapRootPathTest {

        private DataMap mapIn(Path dir) throws IOException {
            DataMap map = new DataMap("test");
            moveTo(map, dir);
            return map;
        }

        private void moveTo(DataMap map, Path dir) throws IOException {
            Path mapFile = Files.createFile(Files.createDirectories(dir).resolve("test.map.xml"));
            map.setConfigurationSource(new URLResource(mapFile.toUri().toURL()));
        }

        @Test
        public void followsTheDataMap(@TempDir Path tmp) throws IOException {
            DataMap map = mapIn(tmp.resolve("old"));
            CgenDescriptor descriptor = new CgenDescriptor();
            descriptor.setDestDir("java");
            CgenConfiguration configuration = new CgenConfiguration(descriptor, map);
            assertEquals(tmp.resolve("old"), configuration.getRootPath());

            moveTo(map, tmp.resolve("new"));
            assertEquals(tmp.resolve("new"), configuration.getRootPath());
            assertEquals(tmp.resolve("new/java"), configuration.requireOutputDirectory());
        }

        @Test
        public void defaultConfigurationFollowsTheDataMap(@TempDir Path tmp) throws IOException {
            DataMap map = mapIn(tmp.resolve("old"));
            CgenConfiguration configuration = CgenConfiguration.createDefault(map, null);

            moveTo(map, tmp.resolve("new"));
            assertEquals(tmp.resolve("new"), configuration.getRootPath());
            assertEquals(tmp.resolve("new"), configuration.requireOutputDirectory());
        }

        @Test
        public void rootPathSet(@TempDir Path tmp) throws IOException {
            CgenConfiguration configuration = new CgenConfiguration(new CgenDescriptor(), mapIn(tmp.resolve("map")));
            configuration.setRootPath(tmp.resolve("root"));

            assertEquals(tmp.resolve("root"), configuration.getRootPath());
        }

        /**
         * The saved output directory of a DataMap is relative to the DataMap directory, so the one relative to another
         * root path is stored absolute, pointing at the same directory before and after the DataMap is saved.
         */
        @Test
        public void rootPathThenRelativeOutputDir(@TempDir Path tmp) throws IOException {
            CgenConfiguration configuration = new CgenConfiguration(new CgenDescriptor(), mapIn(tmp.resolve("map")));
            configuration.setRootPath(tmp.resolve("root"));
            configuration.setOutputDir(Path.of("java"));

            assertEquals(tmp.resolve("root/java"), configuration.requireOutputDirectory());
            assertEquals(tmp.resolve("root/java"), Path.of(configuration.getDescriptor().getDestDir()));
        }

        @Test
        public void relativeOutputDirThenRootPath(@TempDir Path tmp) throws IOException {
            CgenDescriptor descriptor = new CgenDescriptor();
            descriptor.setDestDir("java");
            CgenConfiguration configuration = new CgenConfiguration(descriptor, mapIn(tmp.resolve("map")));
            configuration.setRootPath(tmp.resolve("root"));

            assertEquals(tmp.resolve("map/java"), configuration.requireOutputDirectory());
            assertEquals(tmp.resolve("map/java"), Path.of(configuration.getDescriptor().getDestDir()));
        }

        @Test
        public void addedWithRootPath(@TempDir Path tmp) throws IOException {
            DataMap map = mapIn(tmp.resolve("map"));
            CgenConfiguration configuration = new CgenConfiguration();
            configuration.setRootPath(tmp.resolve("root"));
            configuration.setOutputDir(Path.of("java"));

            new CgenConfigList(map).add(configuration);

            assertEquals(tmp.resolve("root/java"), configuration.requireOutputDirectory());
            assertEquals(tmp.resolve("root/java"), Path.of(configuration.getDescriptor().getDestDir()));
        }

        @Test
        public void dataMapSetLater(@TempDir Path tmp) throws IOException {
            CgenConfiguration configuration = new CgenConfiguration();
            configuration.setDataMap(mapIn(tmp));

            assertNull(configuration.getRootPath());
        }
    }

    /**
     * Setting the root path and the output directory are independent writes; neither reads the other, so
     * the order in which they happen must not change the outcome.
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
            assertEquals(a.getDescriptor().getDestDir(), b.getDescriptor().getDestDir());
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

    @Nested
    public class DescriptorTest {

        private DataMap savedMapIn(Path dir) throws IOException {
            Path mapFile = Files.createFile(dir.resolve("test.map.xml"));
            DataMap map = new DataMap("test");
            map.setConfigurationSource(new URLResource(mapFile.toUri().toURL()));

            ObjEntity artist = new ObjEntity("Artist");
            artist.setClassName("org.example.Artist");
            map.addObjEntity(artist);
            ObjEntity painting = new ObjEntity("Painting");
            painting.setClassName("org.example.Painting");
            map.addObjEntity(painting);

            map.addEmbeddable(new Embeddable("org.example.Address"));
            map.addEmbeddable(new Embeddable("org.example.Phone"));
            return map;
        }

        @Test
        public void defaults() {
            CgenConfiguration configuration = new CgenConfiguration();

            assertEquals(CgenConfigList.DEFAULT_CONFIG_NAME, configuration.getName());
            assertEquals("entity", configuration.getArtifactsGenerationMode());
            assertEquals("*.java", configuration.getOutputPattern());
            assertEquals(TemplateType.ENTITY_SUBCLASS.defaultTemplate(), configuration.getTemplate());
            assertEquals(TemplateType.ENTITY_SUPERCLASS.defaultTemplate(), configuration.getSuperTemplate());
            assertEquals(TemplateType.EMBEDDABLE_SUBCLASS.defaultTemplate(), configuration.getEmbeddableTemplate());
            assertEquals(TemplateType.EMBEDDABLE_SUPERCLASS.defaultTemplate(),
                    configuration.getEmbeddableSuperTemplate());
            assertEquals(TemplateType.DATAMAP_SUBCLASS.defaultTemplate(), configuration.getDataMapTemplate());
            assertEquals(TemplateType.DATAMAP_SUPERCLASS.defaultTemplate(), configuration.getDataMapSuperTemplate());
            assertTrue(configuration.isDefault());
            assertTrue(configuration.outputDirectory().isEmpty());
        }

        @Test
        public void settersChangeTheDescriptor(@TempDir Path tmp) {
            CgenConfiguration configuration = new CgenConfiguration();
            configuration.setName("Custom");
            configuration.setRootPath(tmp);
            configuration.setOutputDir(Paths.get("..", "java"));
            configuration.setArtifactsGenerationMode("ALL");
            configuration.setMakePairs(false);
            configuration.setUsePkgPath(false);
            configuration.setOverwrite(true);
            configuration.setCreatePropertyNames(true);
            configuration.setCreatePKProperties(false);
            configuration.setOutputPattern("*.groovy");
            configuration.setSuperPkg("org.example.auto");
            configuration.setExternalToolConfig("tools.properties");
            configuration.setTemplate(new CgenTemplate("subclass", false, TemplateType.ENTITY_SUBCLASS));
            configuration.setSuperTemplate(new CgenTemplate("superclass", false, TemplateType.ENTITY_SUPERCLASS));
            configuration.setEmbeddableTemplate(
                    new CgenTemplate("embeddable subclass", false, TemplateType.EMBEDDABLE_SUBCLASS));
            configuration.setEmbeddableSuperTemplate(
                    new CgenTemplate("embeddable superclass", false, TemplateType.EMBEDDABLE_SUPERCLASS));
            configuration.setDataMapTemplate(
                    new CgenTemplate("datamap subclass", false, TemplateType.DATAMAP_SUBCLASS));
            configuration.setDataMapSuperTemplate(
                    new CgenTemplate("datamap superclass", false, TemplateType.DATAMAP_SUPERCLASS));
            configuration.getExcludedEntityArtifacts().add("Painting");
            configuration.getExcludedEmbeddableArtifacts().add("org.example.Phone");

            CgenDescriptor descriptor = configuration.getDescriptor();
            assertEquals("Custom", descriptor.getName());
            assertEquals("../java", descriptor.getDestDir());
            assertEquals("all", descriptor.getMode());
            assertFalse(descriptor.isMakePairs());
            assertFalse(descriptor.isUsePkgPath());
            assertTrue(descriptor.isOverwrite());
            assertTrue(descriptor.isCreatePropertyNames());
            assertFalse(descriptor.isCreatePKProperties());
            assertEquals("*.groovy", descriptor.getOutputPattern());
            assertEquals("org.example.auto", descriptor.getSuperPkg());
            assertEquals("tools.properties", descriptor.getExternalToolConfig());
            assertEquals("subclass", descriptor.getTemplate());
            assertEquals("superclass", descriptor.getSuperTemplate());
            assertEquals("embeddable subclass", descriptor.getEmbeddableTemplate());
            assertEquals("embeddable superclass", descriptor.getEmbeddableSuperTemplate());
            assertEquals("datamap subclass", descriptor.getDataMapTemplate());
            assertEquals("datamap superclass", descriptor.getDataMapSuperTemplate());
            assertEquals(Set.of("Painting"), descriptor.getExcludedEntities());
            assertEquals(Set.of("org.example.Phone"), descriptor.getExcludedEmbeddables());
        }

        @Test
        public void outputDirInTheDescriptor(@TempDir Path tmp) {
            CgenConfiguration configuration = new CgenConfiguration();
            configuration.setRootPath(tmp);

            configuration.setOutputDir(Paths.get("src", "main", "java"));
            assertEquals("src/main/java", configuration.getDescriptor().getDestDir());

            configuration.setOutputDir(tmp.resolve("out"));
            assertEquals(tmp.resolve("out"), Paths.get(configuration.getDescriptor().getDestDir()));

            configuration.setOutputDir(Paths.get(""));
            assertNull(configuration.getDescriptor().getDestDir());
            assertEquals(tmp, configuration.requireOutputDirectory());

            configuration.getDescriptor().setDestDir("../java");
            assertEquals(tmp.resolve("../java").normalize(), configuration.requireOutputDirectory());
        }

        @Test
        public void wrapsTheStoredSettings(@TempDir Path tmp) throws IOException {
            CgenDescriptor descriptor = new CgenDescriptor();
            descriptor.setName("Custom");
            descriptor.setDestDir("../java");
            descriptor.setMode("all");
            descriptor.setMakePairs(false);
            descriptor.setUsePkgPath(false);
            descriptor.setOverwrite(true);
            descriptor.setCreatePropertyNames(true);
            descriptor.setCreatePKProperties(false);
            descriptor.setOutputPattern("*.groovy");
            descriptor.setSuperPkg("org.example.auto");
            descriptor.setExternalToolConfig("tools.properties");
            descriptor.setTemplate("subclass");
            descriptor.setSuperTemplate(TemplateType.ENTITY_SUPERCLASS.pathFromSourceRoot());
            descriptor.setExcludedEntities(List.of("Painting", "Removed"));
            descriptor.setExcludedEmbeddables(List.of("org.example.Phone"));

            DataMap map = savedMapIn(tmp);
            CgenConfiguration configuration = new CgenConfiguration(descriptor, map);

            assertSame(descriptor, configuration.getDescriptor());
            assertSame(map, configuration.getDataMap());
            assertEquals(tmp, configuration.getRootPath());
            assertEquals("Custom", configuration.getName());
            assertEquals(tmp.resolve("../java").normalize(), configuration.requireOutputDirectory());
            assertEquals("all", configuration.getArtifactsGenerationMode());
            assertFalse(configuration.isMakePairs());
            assertFalse(configuration.isUsePkgPath());
            assertTrue(configuration.isOverwrite());
            assertTrue(configuration.isCreatePropertyNames());
            assertFalse(configuration.isCreatePKProperties());
            assertEquals("*.groovy", configuration.getOutputPattern());
            assertEquals("org.example.auto", configuration.getSuperPkg());
            assertEquals("tools.properties", configuration.getExternalToolConfig());
            assertEquals(new CgenTemplate("subclass", false, TemplateType.ENTITY_SUBCLASS),
                    configuration.getTemplate());
            assertEquals(TemplateType.ENTITY_SUPERCLASS.defaultTemplate(), configuration.getSuperTemplate());
            assertEquals(TemplateType.EMBEDDABLE_SUBCLASS.defaultTemplate(), configuration.getEmbeddableTemplate());

            assertEquals(Set.of("Artist"), configuration.getEntities());
            assertEquals(Set.of("org.example.Address"), configuration.getEmbeddables());
            assertSame(descriptor.getExcludedEntities(), configuration.getExcludedEntityArtifacts());
            assertSame(descriptor.getExcludedEmbeddables(), configuration.getExcludedEmbeddableArtifacts());
        }

        @Test
        public void inlineTemplatesHaveTheirOwnType() {
            CgenDescriptor descriptor = new CgenDescriptor();
            descriptor.setTemplate("subclass");
            descriptor.setSuperTemplate("superclass");
            descriptor.setEmbeddableTemplate("embeddable subclass");
            descriptor.setEmbeddableSuperTemplate("embeddable superclass");
            descriptor.setDataMapTemplate("datamap subclass");
            descriptor.setDataMapSuperTemplate("datamap superclass");

            CgenConfiguration configuration = new CgenConfiguration(descriptor, new DataMap("test"));

            assertEquals(new CgenTemplate("subclass", false, TemplateType.ENTITY_SUBCLASS),
                    configuration.getTemplate());
            assertEquals(new CgenTemplate("superclass", false, TemplateType.ENTITY_SUPERCLASS),
                    configuration.getSuperTemplate());
            assertEquals(new CgenTemplate("embeddable subclass", false, TemplateType.EMBEDDABLE_SUBCLASS),
                    configuration.getEmbeddableTemplate());
            assertEquals(new CgenTemplate("embeddable superclass", false, TemplateType.EMBEDDABLE_SUPERCLASS),
                    configuration.getEmbeddableSuperTemplate());
            assertEquals(new CgenTemplate("datamap subclass", false, TemplateType.DATAMAP_SUBCLASS),
                    configuration.getDataMapTemplate());
            assertEquals(new CgenTemplate("datamap superclass", false, TemplateType.DATAMAP_SUPERCLASS),
                    configuration.getDataMapSuperTemplate());
        }

        /**
         * The single class template is a default of the subclass template as well.
         */
        @Test
        public void defaultTemplates() {
            CgenConfiguration configuration = new CgenConfiguration();
            assertNull(configuration.getDescriptor().getTemplate());

            configuration.setTemplate(TemplateType.ENTITY_SUBCLASS.defaultTemplate());
            assertEquals("templates/v4_1/subclass.vm", configuration.getDescriptor().getTemplate());
            assertEquals(TemplateType.ENTITY_SUBCLASS.defaultTemplate(), configuration.getTemplate());

            configuration.setTemplate(TemplateType.ENTITY_SINGLE_CLASS.defaultTemplate());
            assertEquals("templates/v4_1/singleclass.vm", configuration.getDescriptor().getTemplate());
            assertEquals(TemplateType.ENTITY_SINGLE_CLASS.defaultTemplate(), configuration.getTemplate());

            configuration.getDescriptor().setTemplate("templates/v4_1/superclass.vm");
            assertEquals(TemplateType.ENTITY_SUBCLASS.defaultTemplate(), configuration.getTemplate());
        }

        @Test
        public void wrappersOfOneDescriptorAgree() {
            CgenDescriptor descriptor = new CgenDescriptor();
            DataMap map = new DataMap("test");
            CgenConfiguration first = new CgenConfiguration(descriptor, map);
            CgenConfiguration second = new CgenConfiguration(descriptor, map);

            first.setTemplate(TemplateType.ENTITY_SINGLE_CLASS.defaultTemplate());
            assertEquals(TemplateType.ENTITY_SINGLE_CLASS.defaultTemplate(), second.getTemplate());

            second.setTemplate(TemplateType.ENTITY_SUBCLASS.defaultTemplate());
            assertEquals(TemplateType.ENTITY_SUBCLASS.defaultTemplate(), first.getTemplate());
        }

        @Test
        public void singleClassTemplatesWrappedAgain() {
            DataMap map = new DataMap("test");
            CgenConfiguration configuration = new CgenConfiguration(new CgenDescriptor(), map);
            configuration.setMakePairs(false);
            configuration.setTemplate(TemplateType.ENTITY_SINGLE_CLASS.defaultTemplate());
            configuration.setEmbeddableTemplate(TemplateType.EMBEDDABLE_SINGLE_CLASS.defaultTemplate());
            configuration.setDataMapTemplate(TemplateType.DATAMAP_SINGLE_CLASS.defaultTemplate());

            CgenConfiguration wrappedAgain = new CgenConfiguration(configuration.getDescriptor(), map);

            assertFalse(wrappedAgain.isMakePairs());
            CgenTemplate template = wrappedAgain.getTemplate();
            assertTrue(template.isFile());
            assertEquals(TemplateType.ENTITY_SINGLE_CLASS, template.getType());
            assertEquals("templates/v4_1/singleclass.vm", template.getData());
            assertEquals(TemplateType.EMBEDDABLE_SINGLE_CLASS.defaultTemplate(), wrappedAgain.getEmbeddableTemplate());
            assertEquals(TemplateType.DATAMAP_SINGLE_CLASS.defaultTemplate(), wrappedAgain.getDataMapTemplate());
        }

        /**
         * The descriptor can't tell a template file from a template text, so the configuration keeps the file.
         */
        @Test
        public void customTemplateFile() {
            CgenTemplate file = new CgenTemplate("custom/superclass.vm", true, TemplateType.ENTITY_SUPERCLASS);

            CgenConfiguration configuration = new CgenConfiguration();
            configuration.setSuperTemplate(file);

            assertEquals("custom/superclass.vm", configuration.getDescriptor().getSuperTemplate());
            assertSame(file, configuration.getSuperTemplate());

            configuration.getDescriptor().setSuperTemplate("superclass");
            assertEquals(new CgenTemplate("superclass", false, TemplateType.ENTITY_SUPERCLASS),
                    configuration.getSuperTemplate());

            configuration.getDescriptor().setSuperTemplate(null);
            assertEquals(TemplateType.ENTITY_SUPERCLASS.defaultTemplate(), configuration.getSuperTemplate());
        }

        /**
         * The names missing from the DataMap are only left out when saving.
         */
        @Test
        public void excludedEntities(@TempDir Path tmp) throws IOException {
            DataMap map = savedMapIn(tmp);
            CgenConfiguration configuration = new CgenConfiguration(new CgenDescriptor(), map);
            configuration.parseExcludedEntities("Painting,Removed");

            assertEquals(List.of("Painting", "Removed"),
                    List.copyOf(configuration.getDescriptor().getExcludedEntities()));

            ObjEntity gallery = new ObjEntity("Gallery");
            gallery.setClassName("org.example.Gallery");
            map.addObjEntity(gallery);
            configuration.resolveExcludedEntities();

            assertEquals(Set.of("Artist", "Gallery"), configuration.getEntities());
        }
    }

    /**
     * Windows has more than one filesystem root - drive letters and UNC shares - and {@code relativize()}
     * throws across them. See CAY-2615 and CAY-2710.
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

            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void equalRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path outputDir = Paths.get("C:\\test1\\test2\\testAnother");
            configuration.setOutputDir(outputDir);

            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void equalRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("C:\\"));
            Path outputDir = Paths.get("C:\\");
            configuration.setOutputDir(outputDir);

            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void notEqualRootsEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path outputDir = Paths.get("E:\\test1\\test2\\test3");
            configuration.setOutputDir(outputDir);

            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void notEqualRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path outputDir = Paths.get("E:\\test1\\test2\\testAnother");
            configuration.setOutputDir(outputDir);

            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void notEqualRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("C:\\"));
            Path outputDir = Paths.get("E:\\");
            configuration.setOutputDir(outputDir);

            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        /**
         * UNC shares are separate filesystem roots too: same share relativizes, different shares don't.
         */
        @Test
        public void uncShares() {
            configuration.setRootPath(Paths.get("\\\\server\\share\\project"));
            configuration.setOutputDir(Paths.get("\\\\server\\share\\java"));
            assertEquals(Paths.get("\\\\server\\share\\java"), configuration.requireOutputDirectory());

            configuration.setOutputDir(Paths.get("\\\\server\\other\\java"));
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

            assertEquals(Paths.get("E:\\"), configuration.requireOutputDirectory());
        }

        @Test
        public void invalidRootPath() {
            assertThrows(InvalidPathException.class, () -> configuration.setRootPath(Paths.get("invalidRoot:\\test")));
        }

        @Test
        public void nullRootPath() {
            configuration.setOutputDir(Path.of("C:\\test1\\test2\\test3"));

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

            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void equalRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            Path outputDir = Paths.get("/test1/test2/testAnother");
            configuration.setOutputDir(outputDir);

            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void equalRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("/"));
            Path outputDir = Paths.get("/");
            configuration.setOutputDir(outputDir);

            assertEquals(outputDir, configuration.requireOutputDirectory());
        }

        @Test
        public void concatCorrectRootPathAndRelPath() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            configuration.setOutputDir(Paths.get("test1/test2/test3"));

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

            assertEquals(Paths.get("/"), configuration.requireOutputDirectory());
        }

        @Test
        public void invalidRootPath() {
            assertThrows(ValidationException.class, () -> configuration.setRootPath(Paths.get("invalidRoot:/test")));
        }

        @Test
        public void nullRootPath() {
            configuration.setOutputDir(Paths.get("/test1/test2/test3"));

            assertEquals(Paths.get("/test1/test2/test3"), configuration.requireOutputDirectory());
        }
    }
}
