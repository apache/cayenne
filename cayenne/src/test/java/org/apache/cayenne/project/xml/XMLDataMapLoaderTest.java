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
package org.apache.cayenne.project.xml;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.project.ConfigurationNameMapper;
import org.apache.cayenne.project.DataMapLoader;
import org.apache.cayenne.project.DefaultConfigurationNameMapper;
import org.apache.cayenne.project.ProjectLoadingMode;
import org.apache.cayenne.project.upgrade.ProjectFileUpgrader;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.map.Commentable;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.NamedSQLQuery;
import org.apache.cayenne.map.NamedSelectQuery;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.project.cgen.CgenDescriptor;
import org.apache.cayenne.project.dbimport.Catalog;
import org.apache.cayenne.project.dbimport.IncludeTable;
import org.apache.cayenne.project.dbimport.ReverseEngineering;
import org.apache.cayenne.project.dbimport.Schema;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.XMLReader;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static org.apache.cayenne.project.xml.XMLTestUtils.childElements;
import static org.apache.cayenne.project.xml.XMLTestUtils.encode;
import static org.apache.cayenne.project.xml.XMLTestUtils.parse;
import static org.apache.cayenne.project.xml.XMLTestUtils.patterns;
import static org.junit.jupiter.api.Assertions.*;

public class XMLDataMapLoaderTest {

    private DataMapLoader loader;

    @BeforeEach
    public void setUp() throws Exception {
        loader = createLoader(ProjectLoadingMode.RUNTIME);
    }

    private static DataMapLoader createLoader(ProjectLoadingMode loadingMode) {
        Module testModule = binder -> {
            binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
            binder.bind(ProjectFileUpgrader.class).to(ProjectFileUpgrader.class);
            binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            binder.bind(ProjectLoadingMode.class).toInstance(loadingMode);
            binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();
        };

        return DIBootstrap.createInjector(testModule).getInstance(DataMapLoader.class);
    }

    @Test
    public void loadMissingConfig() throws Exception {
        assertThrows(CayenneRuntimeException.class, () ->
                loader.load(new URLResource(new URL("file:/no_such_file_for_map_xml"))));
    }

    @Test
    public void loadOldVersionConfig() {
        // version 9, upgraded in memory
        URL url = getClass().getResource("testConfigMap5.map.xml");
        DataMap map = loader.load(new URLResource(url));
        assertEquals("testConfigMap5", map.getName());
        assertTrue(map.getDbEntities().isEmpty());
    }

    @Test
    public void loadOldVersionWithContent() {
        // version 9, upgraded in memory
        URL url = getClass().getResource("testConfigMap9.map.xml");
        DataMap map = loader.load(new URLResource(url));
        assertEquals("testConfigMap9", map.getName());
        assertEquals(2, map.getDbEntity("ARTIST").getAttributes().size());
        assertEquals("org.apache.cayenne.GenericPersistentObject", map.getObjEntity("Artist").getClassName());

        NamedSelectQuery select = (NamedSelectQuery) map.getNamedQuery("ArtistQuery");
        assertEquals("from Artist where artistName = \"a\" limit 5", select.getQl());
        assertEquals(10, select.getPageSize());
        assertEquals("g1 & g2", select.getCacheGroup());

        NamedSQLQuery sql = (NamedSQLQuery) map.getNamedQuery("ArtistSql");
        assertEquals("select * from ARTIST", sql.getSql());
        assertEquals(7, sql.getPageSize());
        assertEquals(CapsStrategy.UPPER, sql.getColumnNamesCapitalization());
    }

    @Test
    public void loadInvalidObjEntityQualifier() {
        URL url = getClass().getResource("testConfigMap11.map.xml");
        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class, () -> loader.load(new URLResource(url)));
        assertTrue(e.getCause().getMessage().contains("Invalid qualifier of ObjEntity 'Artist': name = ((("),
                e.getCause().getMessage());
    }

    @Test
    public void loadInvalidDbEntityQualifier() {
        URL url = getClass().getResource("testConfigMap12.map.xml");
        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class, () -> loader.load(new URLResource(url)));
        assertTrue(e.getCause().getMessage().contains("Invalid qualifier of DbEntity 'ARTIST': NAME = ((("),
                e.getCause().getMessage());
    }

    @Test
    public void loadDbRelationshipWithUnknownSource() {
        URL url = getClass().getResource("testConfigMap13.map.xml");
        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class, () -> loader.load(new URLResource(url)));
        assertTrue(e.getCause().getMessage().contains("unable to find source NO_SUCH_ENTITY"),
                e.getCause().getMessage());
    }

    @Test
    public void loadNewerVersionConfig() {
        URL url = getClass().getResource("testConfigMap6.map.xml");
        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class, () -> loader.load(new URLResource(url)));
        assertTrue(e.getMessage().contains("version 16 is newer"), e.getMessage());
    }

    @Test
    public void loadEmptyConfig() throws Exception {
        URL url = getClass().getResource("testConfigMap2.map.xml");
        DataMap map = loader.load(new URLResource(url));

        assertNotNull(map);
        assertEquals("testConfigMap2", map.getName());
        assertTrue(map.getDbEntities().isEmpty());
        assertTrue(map.getObjEntities().isEmpty());
        assertTrue(map.getProcedures().isEmpty());
        assertTrue(map.getNamedQueries().isEmpty());
        assertTrue(map.getEmbeddables().isEmpty());
        assertNull(map.getDefaultCatalog());
        assertNull(map.getDefaultSchema());
        assertNull(map.getDefaultPackage());
    }

    @Test
    public void loadFullDataMap() {
        URL url = getClass().getResource("testConfigMap4.map.xml");
        DataMap map = loader.load(new URLResource(url));

        assertNotNull(map);
        assertEquals("testConfigMap4", map.getName());

        // check general state
        assertEquals(12, map.getDbEntities().size());
        assertEquals(17, map.getObjEntities().size());
        assertEquals(4, map.getProcedures().size());
        assertEquals(13, map.getNamedQueries().size());
        assertEquals(1, map.getEmbeddables().size());
        assertEquals("TEST_CATALOG", map.getDefaultCatalog());
        assertNull(map.getDefaultSchema());
        assertEquals("org.apache.cayenne.testdo.testmap", map.getDefaultPackage());

        // check some loaded content
        assertEquals("org.apache.cayenne.testdo.testmap.Artist",
                map.getObjEntity("Artist").getClassName());
        assertEquals(5,
                map.getObjEntity("CompoundPainting").getAttributes().size());
        assertEquals(3,
                map.getObjEntity("Artist").getRelationships().size());

        assertEquals("name = \"test\"",
                map.getDbEntity("ARTGROUP").getQualifier().toString());
        assertEquals(4,
                map.getDbEntity("EXHIBIT").getAttributes().size());
        assertEquals(3,
                map.getDbEntity("PAINTING").getRelationships().size());
        assertEquals("gallery_seq",
                map.getDbEntity("GALLERY").getPrimaryKeyGenerator().getGeneratorName());

        DbAttribute pk1 = map.getDbEntity("EXHIBIT").getAttribute("EXHIBIT_ID");
        assertFalse(pk1.isGenerated());
        assertTrue(pk1.isPrimaryKey());

        DbAttribute pk2 = map.getDbEntity("GENERATED_COLUMN").getAttribute("GENERATED_COLUMN");
        assertTrue(pk2.isGenerated());
        assertTrue(pk2.isPrimaryKey());

        assertEquals(true,
                map.getProcedure("cayenne_tst_out_proc").isReturningValue());
        assertEquals(1,
                map.getProcedure("cayenne_tst_out_proc").getCallOutParameters().size());
        assertEquals(2,
                map.getProcedure("cayenne_tst_out_proc").getCallParameters().size());

        NamedSQLQuery namedQuery = (NamedSQLQuery)map.getNamedQuery("NonSelectingQuery");
        assertEquals("INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE) " +
                        "VALUES (512, 'No Painting Like This', 12.5)",
                namedQuery.getAdapterSql().get("org.apache.cayenne.dba.db2.DB2Adapter"));

        assertEquals("TEST",
                map.getEmbeddable("org.apache.cayenne.testdo.Embeddable")
                        .getAttribute("test").getDbAttributeName());
    }

    @Test
    public void loadComments() {
        DataMap map = createLoader(ProjectLoadingMode.FULL).load(commentsMap());

        Map<String, String> expected = new TreeMap<>();
        expected.put("dataMap", "dataMap & <escaped>");
        expected.put("embeddable:org.example.Address", "embeddable");
        expected.put("embeddable:org.example.Address.city", "embeddableAttribute");
        expected.put("embeddable:org.example.Address.street", null);
        expected.put("procedure:proc", "procedure");
        expected.put("procedure:proc.in_param", "procedureParameter");
        expected.put("procedure:proc.out_param", null);
        expected.put("dbEntity:ARTIST", "dbEntity");
        expected.put("dbEntity:ARTIST.ARTIST_ID", "dbAttribute");
        expected.put("dbEntity:ARTIST.ARTIST_NAME", null);
        // not trimmed, and a blank one is skipped
        expected.put("dbEntity:ARTIST.CITY", "\n\t\t\t\tmulti-line\n\t\t\t\tdbAttribute\n\t\t\t");
        expected.put("dbEntity:ARTIST.STREET", null);
        expected.put("dbEntity:ARTIST.paintings", "dbRelationship");
        expected.put("dbEntity:PAINTING", null);
        expected.put("dbEntity:PAINTING.ARTIST_ID", null);
        expected.put("dbEntity:PAINTING.PAINTING_ID", null);
        expected.put("dbEntity:PAINTING.artist", null);
        expected.put("objEntity:Artist", "objEntity");
        expected.put("objEntity:Artist.address", "embeddedAttribute");
        expected.put("objEntity:Artist.artistName", "objAttribute");
        expected.put("objEntity:Artist.paintings", "objRelationship");
        expected.put("objEntity:Painting", null);
        expected.put("objEntity:Painting.artist", null);
        expected.put("query:objectQuery", "objectQuery");
        expected.put("query:procedureQuery", "procedureQuery");
        expected.put("query:sqlQuery", "sqlQuery");

        assertEquals(expected, comments(map));
    }

    @Test
    public void skipComments() {
        DataMap map = loader.load(commentsMap());
        Map<String, String> comments = comments(map);
        assertTrue(comments.values().stream().allMatch(Objects::isNull), comments.toString());

        DataMap fullMap = createLoader(ProjectLoadingMode.FULL).load(commentsMap());
        commentables(fullMap).values().forEach(c -> c.setComment(null));
        assertEquals(encode(fullMap), encode(map));
    }

    @Test
    public void saveAndLoadComments(@TempDir Path tempDir) throws Exception {
        DataMapLoader fullLoader = createLoader(ProjectLoadingMode.FULL);
        DataMap map = fullLoader.load(commentsMap());
        Map<String, String> comments = comments(map);
        String xml = encode(map);

        Document document = parse(xml);
        NodeList commentElements = document.getElementsByTagName("comment");
        assertEquals(comments.values().stream().filter(Objects::nonNull).count(), commentElements.getLength());
        for (int i = 0; i < commentElements.getLength(); i++) {
            Element commentElement = (Element) commentElements.item(i);
            assertSame(commentElement, childElements((Element) commentElement.getParentNode()).get(0));
        }

        Path file = tempDir.resolve("saveAndLoadComments.map.xml");
        Files.writeString(file, xml);
        DataMap loaded = fullLoader.load(new URLResource(file.toUri().toURL()));
        assertEquals(comments, comments(loaded));
    }

    /**
     * The characters that XML can't hold are dropped, keeping the saved DataMap loadable. The line ends are normalized.
     */
    @Test
    public void saveAndLoadCommentWithControlChars(@TempDir Path tempDir) throws Exception {
        DataMap map = new DataMap("map");
        map.setComment("line\u0007 1\r\nline 2");

        Path file = tempDir.resolve("saveAndLoadCommentWithControlChars.map.xml");
        Files.writeString(file, encode(map));
        DataMap loaded = createLoader(ProjectLoadingMode.FULL).load(new URLResource(file.toUri().toURL()));
        assertEquals("line 1\nline 2", loaded.getComment());
    }

    @Test
    public void loadOldVersionComments() {
        // version 14, upgraded in memory
        URL url = getClass().getResource("testConfigMap15.map.xml");
        DataMap map = createLoader(ProjectLoadingMode.FULL).load(new URLResource(url));
        assertReverseRelationshipCreated(map);

        Map<String, String> expected = new TreeMap<>();
        expected.put("dataMap", "dataMap");
        expected.put("dbEntity:ARTIST", "dbEntity");
        expected.put("dbEntity:ARTIST.ARTIST_ID", "dbAttribute");
        expected.put("dbEntity:ARTIST.ARTIST_NAME", null);
        expected.put("dbEntity:ARTIST.toInfo", "dbRelationship");
        expected.put("dbEntity:ARTIST_INFO", null);
        expected.put("dbEntity:ARTIST_INFO.ARTIST_ID", null);
        expected.put("dbEntity:ARTIST_INFO.artist", null);
        expected.put("objEntity:Artist", "objEntity");
        expected.put("objEntity:Artist.artistName", "objAttribute");

        assertEquals(expected, comments(map));
    }

    @Test
    public void loadOldVersionQueryComments() {
        // version 10, upgraded in memory
        URL url = getClass().getResource("testConfigMap21.map.xml");
        DataMap map = createLoader(ProjectLoadingMode.FULL).load(new URLResource(url));

        NamedSelectQuery select = (NamedSelectQuery) map.getNamedQuery("ArtistQuery");
        assertEquals("from Artist where artistName = \"a\" limit 5", select.getQl());
        assertEquals(10, select.getPageSize());
        assertEquals("g1", select.getCacheGroup());
        assertEquals("select query", select.getComment());
        assertEquals("select query, other prefix", map.getNamedQuery("ArtistQueryOtherPrefix").getComment());
        assertEquals("g2", map.getNamedQuery("ArtistSql").getCacheGroup());
        assertEquals("SQL query", map.getNamedQuery("ArtistSql").getComment());
    }

    @Test
    public void upgradeOldVersionQueryComments() {
        // version 10: the upgraded query tags are in the schema order, whatever the position of the comment
        URLResource resource = new URLResource(getClass().getResource("testConfigMap21.map.xml"));
        Element root = new ProjectFileUpgrader(new XMLReaderProvider(false)).upgradeDataMapDOM(resource, "10")
                .getDocument().getDocumentElement();

        Map<String, List<String>> queries = new TreeMap<>();
        for (Element child : childElements(root)) {
            if (child.getNodeName().endsWith("Query")) {
                List<String> tags = childElements(child).stream().map(Element::getNodeName).toList();
                queries.put(child.getAttribute("name"), tags);
            }
        }
        assertEquals(Map.of(
                "ArtistQuery", List.of("comment", "ql", "cacheGroup"),
                "ArtistQueryOtherPrefix", List.of("comment", "ql"),
                "ArtistSql", List.of("comment", "sql", "cacheGroup", "prefetch")), queries);
    }

    @Test
    public void skipOldVersionComments() {
        // version 14, upgraded in memory
        URL url = getClass().getResource("testConfigMap15.map.xml");
        DataMap map = loader.load(new URLResource(url));
        assertReverseRelationshipCreated(map);

        Map<String, String> comments = comments(map);
        assertTrue(comments.values().stream().allMatch(Objects::isNull), comments.toString());
    }

    @Test
    public void loadReverseEngineering() {
        DataMap map = createLoader(ProjectLoadingMode.FULL).load(reverseEngineeringMap());
        ReverseEngineering reverseEngineering = map.getReverseEngineering();
        assertNotNull(reverseEngineering);

        assertEquals(List.of("ARTIST"), patterns(reverseEngineering.getIncludeTables()));
        IncludeTable includeTable = reverseEngineering.getIncludeTables().get(0);
        assertEquals(List.of("ARTIST_ID"), patterns(includeTable.getIncludeColumns()));
        assertEquals(List.of("ARTIST_TMP"), patterns(includeTable.getExcludeColumns()));
        assertEquals(List.of("OLD_(?<name>.*) & more"), patterns(reverseEngineering.getExcludeTables()));
        assertEquals(List.of(".*_ID"), patterns(reverseEngineering.getIncludeColumns()));
        assertEquals(List.of("SECRET"), patterns(reverseEngineering.getExcludeColumns()));
        assertEquals(List.of("PROC_.*"), patterns(reverseEngineering.getIncludeProcedures()));
        assertEquals(List.of("PROC_OLD"), patterns(reverseEngineering.getExcludeProcedures()));

        assertEquals(1, reverseEngineering.getCatalogs().size());
        Catalog catalog = reverseEngineering.getCatalogs().get(0);
        assertEquals("CATALOG_01", catalog.getName());
        assertEquals(List.of("CATALOG_TMP"), patterns(catalog.getExcludeTables()));
        assertEquals(1, catalog.getSchemas().size());
        assertEquals("SCHEMA_01", catalog.getSchemas().get(0).getName());
        assertEquals(List.of("PAINTING"), patterns(catalog.getSchemas().get(0).getIncludeTables()));
        assertEquals(1, reverseEngineering.getSchemas().size());
        Schema schema = reverseEngineering.getSchemas().get(0);
        assertEquals("SCHEMA_02", schema.getName());
        assertEquals(List.of("SCHEMA_PROC"), patterns(schema.getExcludeProcedures()));

        assertArrayEquals(new String[]{"TABLE", "VIEW"}, reverseEngineering.getTableTypes());
        assertEquals("org.example.imported", reverseEngineering.getDefaultPackage());
        assertTrue(reverseEngineering.isForceDataMapCatalog());
        assertTrue(reverseEngineering.isForceDataMapSchema());
        assertEquals("ARTIST", reverseEngineering.getMeaningfulPkTables());
        assertEquals("org.example.NameGenerator", reverseEngineering.getNamingStrategy());
        assertTrue(reverseEngineering.getSkipPrimaryKeyLoading());
        assertTrue(reverseEngineering.getSkipRelationshipsLoading());
        assertEquals("^T_", reverseEngineering.getStripFromTableNames());
        assertTrue(reverseEngineering.isUseJava7Types());
    }

    @Test
    public void skipReverseEngineering() {
        DataMap map = loader.load(reverseEngineeringMap());
        assertNull(map.getReverseEngineering());

        DataMap fullMap = createLoader(ProjectLoadingMode.FULL).load(reverseEngineeringMap());
        fullMap.setReverseEngineering(null);
        assertEquals(encode(fullMap), encode(map));
    }

    @Test
    public void saveAndLoadReverseEngineering(@TempDir Path tempDir) throws Exception {
        DataMapLoader fullLoader = createLoader(ProjectLoadingMode.FULL);
        DataMap map = fullLoader.load(reverseEngineeringMap());
        String xml = encode(map);

        Element root = parse(xml).getDocumentElement();
        List<Element> children = childElements(root);
        assertEquals(List.of("dbEntity", "objEntity", "objectQuery", "dbImport"),
                children.stream().map(Element::getLocalName).toList());
        Element dbImport = children.get(3);
        assertEquals(DataMap.SCHEMA_XSD, dbImport.getNamespaceURI());
        assertFalse(dbImport.hasAttribute("xmlns"));

        Path file = tempDir.resolve("saveAndLoadReverseEngineering.map.xml");
        Files.writeString(file, xml);
        DataMap loaded = fullLoader.load(new URLResource(file.toUri().toURL()));
        assertEquals(encode(map.getReverseEngineering()), encode(loaded.getReverseEngineering()));
    }

    @Test
    public void loadOldVersionReverseEngineering() {
        // version 14, upgraded in memory
        URL url = getClass().getResource("testConfigMap17.map.xml");
        DataMap map = createLoader(ProjectLoadingMode.FULL).load(new URLResource(url));
        ReverseEngineering reverseEngineering = map.getReverseEngineering();
        assertNotNull(reverseEngineering);
        assertEquals(1, reverseEngineering.getCatalogs().size());
        assertEquals("CATALOG_01", reverseEngineering.getCatalogs().get(0).getName());
        assertEquals("SCHEMA_01", reverseEngineering.getCatalogs().get(0).getSchemas().get(0).getName());
        assertArrayEquals(new String[]{"TABLE"}, reverseEngineering.getTableTypes());
        assertTrue(reverseEngineering.getSkipRelationshipsLoading());
    }

    @Test
    public void loadOldVersionLegacyReverseEngineering() {
        // version 10, with the legacy "config" root tag, upgraded in memory
        URL url = getClass().getResource("testConfigMap18.map.xml");
        DataMap map = createLoader(ProjectLoadingMode.FULL).load(new URLResource(url));

        ReverseEngineering reverseEngineering = map.getReverseEngineering();
        assertNotNull(reverseEngineering);
        assertEquals(1, reverseEngineering.getSchemas().size());
        assertEquals("SCHEMA_01", reverseEngineering.getSchemas().get(0).getName());
        assertEquals(List.of("TMP_.*"), patterns(reverseEngineering.getSchemas().get(0).getExcludeTables()));
        assertEquals("org.example.imported", reverseEngineering.getDefaultPackage());
    }

    @Test
    public void skipOldVersionReverseEngineering() {
        // version 14, upgraded in memory
        DataMap map = loader.load(new URLResource(getClass().getResource("testConfigMap17.map.xml")));
        assertNull(map.getReverseEngineering());
        assertNotNull(map.getDbEntity("ARTIST"));
    }

    @Test
    public void loadCgen() {
        DataMap map = createLoader(ProjectLoadingMode.FULL).load(cgenMap());
        assertEquals(2, map.getCgenDescriptors().size());

        CgenDescriptor custom = map.getCgenDescriptors().get(0);
        assertEquals("Custom & more", custom.getName());
        assertEquals(List.of("Painting"), List.copyOf(custom.getExcludedEntities()));
        assertEquals(List.of("org.example.Phone"), List.copyOf(custom.getExcludedEmbeddables()));
        assertEquals("../java", custom.getDestDir());
        assertEquals("all", custom.getMode());
        assertEquals("custom/subclass.vm", custom.getTemplate());
        assertEquals("\n#if($a < $b)\nsuperclass\n#end\n", custom.getSuperTemplate());
        assertEquals("custom/embeddable-subclass.vm", custom.getEmbeddableTemplate());
        assertEquals("custom/embeddable-superclass.vm", custom.getEmbeddableSuperTemplate());
        assertEquals("custom/datamap-subclass.vm", custom.getDataMapTemplate());
        assertEquals("custom/datamap-superclass.vm", custom.getDataMapSuperTemplate());
        assertEquals("*.groovy", custom.getOutputPattern());
        assertFalse(custom.isMakePairs());
        assertFalse(custom.isUsePkgPath());
        assertTrue(custom.isOverwrite());
        assertTrue(custom.isCreatePropertyNames());
        assertEquals("org.example.auto", custom.getSuperPkg());
        assertFalse(custom.isCreatePKProperties());
        assertEquals("tools.properties", custom.getExternalToolConfig());

        CgenDescriptor defaults = map.getCgenDescriptors().get(1);
        assertEquals("Default", defaults.getName());
        assertEquals(".", defaults.getDestDir());
        assertEquals("entity", defaults.getMode());
        assertNull(defaults.getTemplate());
    }

    @Test
    public void skipCgen() {
        DataMap map = loader.load(cgenMap());
        assertTrue(map.getCgenDescriptors().isEmpty());

        DataMap fullMap = createLoader(ProjectLoadingMode.FULL).load(cgenMap());
        fullMap.setReverseEngineering(null);
        List.copyOf(fullMap.getCgenDescriptors()).forEach(fullMap::removeCgenDescriptor);
        assertEquals(encode(fullMap), encode(map));
    }

    @Test
    public void saveAndLoadCgen(@TempDir Path tempDir) throws Exception {
        DataMapLoader fullLoader = createLoader(ProjectLoadingMode.FULL);
        DataMap map = fullLoader.load(cgenMap());
        String xml = encode(map);

        Element root = parse(xml).getDocumentElement();
        List<Element> children = childElements(root);
        assertEquals(List.of("embeddable", "embeddable", "dbEntity", "objEntity", "objEntity", "dbImport", "cgen",
                "cgen"), children.stream().map(Element::getLocalName).toList());
        for (Element cgen : children.subList(6, 8)) {
            assertEquals(DataMap.SCHEMA_XSD, cgen.getNamespaceURI());
            assertFalse(cgen.hasAttribute("xmlns"));
        }
        assertEquals("Custom & more", childElements(children.get(6)).get(0).getTextContent());
        assertEquals("Default", childElements(children.get(7)).get(0).getTextContent());

        Path file = tempDir.resolve("saveAndLoadCgen.map.xml");
        Files.writeString(file, xml);
        DataMap loaded = fullLoader.load(new URLResource(file.toUri().toURL()));
        assertEquals(2, loaded.getCgenDescriptors().size());
        assertEquals(xml, encode(loaded));
    }

    @Test
    public void loadOldVersionCgen() {
        // version 14, upgraded in memory
        URL url = getClass().getResource("testConfigMap20.map.xml");
        DataMap map = createLoader(ProjectLoadingMode.FULL).load(new URLResource(url));

        assertEquals(1, map.getCgenDescriptors().size());
        CgenDescriptor descriptor = map.getCgenDescriptors().get(0);
        assertEquals("Default", descriptor.getName());
        assertEquals(List.of("Artist"), List.copyOf(descriptor.getExcludedEntities()));
        assertEquals("../java", descriptor.getDestDir());
        assertEquals("templates/v4_1/subclass.vm", descriptor.getTemplate());
        assertEquals("custom superclass", descriptor.getSuperTemplate());
    }

    @Test
    public void skipOldVersionCgen() {
        // version 14, upgraded in memory
        DataMap map = loader.load(new URLResource(getClass().getResource("testConfigMap20.map.xml")));
        assertTrue(map.getCgenDescriptors().isEmpty());
        assertNotNull(map.getObjEntity("Artist"));
    }

    private URLResource commentsMap() {
        return new URLResource(getClass().getResource("testConfigMap14.map.xml"));
    }

    private URLResource reverseEngineeringMap() {
        return new URLResource(getClass().getResource("testConfigMap16.map.xml"));
    }

    private URLResource cgenMap() {
        return new URLResource(getClass().getResource("testConfigMap19.map.xml"));
    }

    private static void assertReverseRelationshipCreated(DataMap map) {
        DbRelationship reverse = map.getDbEntity("ARTIST_INFO").getRelationship("artist");
        assertNotNull(reverse);
        assertEquals("ARTIST", reverse.getTargetEntityName());
        assertTrue(reverse.isForeignKey());
        assertFalse(reverse.isToMany());
        assertEquals(1, reverse.getJoins().size());
        assertFalse(map.getDbEntity("ARTIST").getRelationship("toInfo").isForeignKey());
    }

    private static Map<String, Commentable> commentables(DataMap map) {
        Map<String, Commentable> commentables = new TreeMap<>();
        commentables.put("dataMap", map);
        for (Embeddable embeddable : map.getEmbeddables()) {
            String path = "embeddable:" + embeddable.getClassName();
            commentables.put(path, embeddable);
            embeddable.getAttributes().forEach(a -> commentables.put(path + "." + a.getName(), a));
        }
        for (Procedure procedure : map.getProcedures()) {
            String path = "procedure:" + procedure.getName();
            commentables.put(path, procedure);
            procedure.getCallParameters().forEach(p -> commentables.put(path + "." + p.getName(), p));
        }
        for (DbEntity entity : map.getDbEntities()) {
            String path = "dbEntity:" + entity.getName();
            commentables.put(path, entity);
            entity.getAttributes().forEach(a -> commentables.put(path + "." + a.getName(), a));
            entity.getRelationships().forEach(r -> commentables.put(path + "." + r.getName(), r));
        }
        for (ObjEntity entity : map.getObjEntities()) {
            String path = "objEntity:" + entity.getName();
            commentables.put(path, entity);
            entity.getDeclaredAttributes().forEach(a -> commentables.put(path + "." + a.getName(), a));
            entity.getDeclaredRelationships().forEach(r -> commentables.put(path + "." + r.getName(), r));
        }
        map.getNamedQueries().forEach(q -> commentables.put("query:" + q.getName(), q));
        return commentables;
    }

    private static Map<String, String> comments(DataMap map) {
        Map<String, String> comments = new TreeMap<>();
        commentables(map).forEach((path, commentable) -> comments.put(path, commentable.getComment()));
        return comments;
    }
}
