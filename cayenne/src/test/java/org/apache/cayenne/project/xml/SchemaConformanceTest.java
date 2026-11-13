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

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbKeyGenerator;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.NamedProcedureQuery;
import org.apache.cayenne.map.NamedQuery;
import org.apache.cayenne.map.NamedSQLQuery;
import org.apache.cayenne.map.NamedSelectQuery;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.cgen.CgenDescriptor;
import org.apache.cayenne.project.dbimport.Catalog;
import org.apache.cayenne.project.dbimport.ExcludeColumn;
import org.apache.cayenne.project.dbimport.ExcludeProcedure;
import org.apache.cayenne.project.dbimport.ExcludeTable;
import org.apache.cayenne.project.dbimport.FilterContainer;
import org.apache.cayenne.project.dbimport.IncludeColumn;
import org.apache.cayenne.project.dbimport.IncludeProcedure;
import org.apache.cayenne.project.dbimport.IncludeTable;
import org.apache.cayenne.project.dbimport.ReverseEngineering;
import org.apache.cayenne.project.dbimport.Schema;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.apache.cayenne.project.xml.XMLTestUtils.assertMatchesSchema;
import static org.apache.cayenne.project.xml.XMLTestUtils.childElements;
import static org.apache.cayenne.project.xml.XMLTestUtils.encode;
import static org.apache.cayenne.project.xml.XMLTestUtils.parse;
import static org.apache.cayenne.project.xml.XMLTestUtils.schema;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the saved XML of a project and of a DataMap with every element and setting matches the XML schemas, and
 * covers every attribute and child element the schemas declare.
 */
public class SchemaConformanceTest {

    private static final String COMMENT = "<comment> & \"more\"";

    @Test
    public void projectMatchesSchema() throws Exception {
        Project project = new Project();
        project.setSharedCacheEnabled(false);
        project.setValidatingObjectsOnCommit(false);
        project.getDataMaps().add(new DataMap("map1"));
        project.getDataMaps().add(new DataMap("map2"));
        project.setDisabledInspections(List.of("PROJECT_NO_NAME", "DB_ENTITY_NO_PK"));

        assertMatchesSchema(project);
        assertContentMatchesSchema(encode(project), "project.xsd");
    }

    @Test
    public void dataMapMatchesSchema() throws Exception {
        DataMap map = createDataMap();

        assertMatchesSchema(map);
        assertContentMatchesSchema(encode(map), "dataMap.xsd");
    }

    @Test
    public void freeTextReverseEngineeringMatchesSchema() throws Exception {
        Catalog catalog = new Catalog();
        catalog.addSchema(new Schema("schéma-01 %"));
        ReverseEngineering freeText = new ReverseEngineering();
        freeText.addIncludeTable(new IncludeTable());
        freeText.addCatalog(catalog);
        freeText.addSchema(new Schema());
        freeText.setTableTypes(List.of("SYSTEM TABLE", "GLOBAL TEMPORARY"));

        DataMap map = createDataMap();
        map.setReverseEngineering(freeText);
        assertMatchesSchema(map);
    }

    @Test
    public void blankTableTypesMatchSchema() throws Exception {
        ReverseEngineering blankTableTypes = new ReverseEngineering();
        blankTableTypes.addTableType(" ");

        DataMap map = createDataMap();
        map.setReverseEngineering(blankTableTypes);
        assertTrue(encode(map).contains("<tableTypes/>"));
        assertMatchesSchema(map);
    }

    @Test
    public void freeTextCgenMatchesSchema() throws Exception {
        CgenDescriptor freeText = new CgenDescriptor();
        freeText.setName("<schéma> & \"more\"");
        freeText.setDestDir("C:\\Program Files\\out & in");
        freeText.setMode("all");
        freeText.setTemplate("#if($a < $b && $c > $d)]]>");
        freeText.setSuperTemplate("line 1\n]]>\nline 3");
        freeText.setSuperPkg("org\\example");
        freeText.setOutputPattern("*.<java>");
        freeText.setExternalToolConfig("tools & more.properties");
        freeText.setExcludedEntities(List.of("Painting", "Artist"));
        freeText.setExcludedEmbeddables(List.of("org.example.Address"));

        DataMap map = createDataMap();
        map.addCgenDescriptor(freeText);
        assertMatchesSchema(map);
    }

    private static DataMap createDataMap() {
        DataMap map = new DataMap("map");
        map.setDefaultLockType(ObjEntity.LOCK_TYPE_OPTIMISTIC);
        map.setDefaultPackage("org.example");
        map.setDefaultCatalog("CATALOG");
        map.setDefaultSchema("SCHEMA");
        map.setDefaultSuperclass("org.example.BaseObject");
        map.setQuotingSQLIdentifiers(true);
        map.setComment(COMMENT);

        addEmbeddable(map);
        Procedure procedure = addProcedure(map);
        addDbEntities(map);
        addObjEntities(map);
        addQueries(map, procedure);
        map.setReverseEngineering(createReverseEngineering());
        addCgenDescriptors(map);
        return map;
    }

    private static void addEmbeddable(DataMap map) {
        Embeddable embeddable = new Embeddable("org.example.Address");
        embeddable.setComment(COMMENT);

        EmbeddableAttribute city = new EmbeddableAttribute("city");
        city.setType("java.lang.String");
        city.setDbAttributeName("CITY");
        city.setComment(COMMENT);
        embeddable.addAttribute(city);

        map.addEmbeddable(embeddable);
    }

    private static Procedure addProcedure(DataMap map) {
        Procedure procedure = new Procedure("proc");
        procedure.setSchema("SCHEMA");
        procedure.setCatalog("CATALOG");
        procedure.setReturningValue(true);
        procedure.setComment(COMMENT);

        ProcedureParameter in = new ProcedureParameter("in_param", Types.DECIMAL, ProcedureParameter.IN_PARAMETER);
        in.setMaxLength(10);
        in.setPrecision(2);
        in.setComment(COMMENT);
        procedure.addCallParameter(in);
        procedure.addCallParameter(
                new ProcedureParameter("out_param", Types.INTEGER, ProcedureParameter.OUT_PARAMETER));
        procedure.addCallParameter(
                new ProcedureParameter("in_out_param", Types.INTEGER, ProcedureParameter.IN_OUT_PARAMETER));

        map.addProcedure(procedure);
        return procedure;
    }

    private static void addDbEntities(DataMap map) {
        DbEntity artist = new DbEntity("ARTIST");
        artist.setSchema("SCHEMA");
        artist.setCatalog("CATALOG");
        artist.setComment(COMMENT);

        DbAttribute id = new DbAttribute("ID", Types.BIGINT, artist);
        id.setPrimaryKey(true);
        id.setGenerated(true);
        id.setMandatory(true);
        id.setComment(COMMENT);
        artist.addAttribute(id);

        DbAttribute name = new DbAttribute("NAME", Types.VARCHAR, artist);
        name.setMaxLength(254);
        artist.addAttribute(name);

        DbAttribute fee = new DbAttribute("FEE", Types.DECIMAL, artist);
        fee.setAttributePrecision(10);
        fee.setScale(2);
        artist.addAttribute(fee);

        artist.addAttribute(new DbAttribute("CITY", Types.VARCHAR, artist));

        DbKeyGenerator keyGenerator = new DbKeyGenerator();
        keyGenerator.setGeneratorType(DbKeyGenerator.ORACLE_TYPE);
        keyGenerator.setGeneratorName("artist_seq");
        keyGenerator.setKeyCacheSize(20);
        artist.setPrimaryKeyGenerator(keyGenerator);
        artist.setQualifier(ExpressionFactory.exp("NAME != null"));
        map.addDbEntity(artist);

        DbEntity painting = new DbEntity("PAINTING");
        DbAttribute paintingId = new DbAttribute("ID", Types.BIGINT, painting);
        paintingId.setPrimaryKey(true);
        painting.addAttribute(paintingId);
        painting.addAttribute(new DbAttribute("ARTIST_ID", Types.BIGINT, painting));
        painting.addAttribute(new DbAttribute("TITLE", Types.VARCHAR, painting));
        map.addDbEntity(painting);

        DbRelationship toPaintings = new DbRelationship("paintings");
        toPaintings.setSourceEntity(artist);
        toPaintings.setTargetEntityName("PAINTING");
        toPaintings.setToMany(true);
        toPaintings.addJoin(new DbJoin(toPaintings, "ID", "ARTIST_ID"));
        toPaintings.setComment(COMMENT);
        artist.addRelationship(toPaintings);

        DbRelationship toArtist = new DbRelationship("artist");
        toArtist.setSourceEntity(painting);
        toArtist.setTargetEntityName("ARTIST");
        toArtist.setForeignKey(true);
        toArtist.addJoin(new DbJoin(toArtist, "ARTIST_ID", "ID"));
        painting.addRelationship(toArtist);
    }

    private static void addObjEntities(DataMap map) {
        ObjEntity artist = new ObjEntity("Artist");
        artist.setClassName("org.example.Artist");
        artist.setSuperClassName("org.example.BaseObject");
        artist.setDbEntityName("ARTIST");
        artist.setAbstract(true);
        artist.setReadOnly(true);
        artist.setDeclaredLockType(ObjEntity.LOCK_TYPE_OPTIMISTIC);
        artist.setDeclaredQualifier(ExpressionFactory.exp("name != null"));
        artist.setComment(COMMENT);

        ObjAttribute name = new ObjAttribute("name", "java.lang.String", artist);
        name.setDbAttributePath("NAME");
        name.setUsedForLocking(true);
        name.setLazy(true);
        name.setComment(COMMENT);
        artist.addAttribute(name);

        EmbeddedAttribute address = new EmbeddedAttribute("address", "org.example.Address", artist);
        address.addAttributeOverride("city", "CITY");
        address.setComment(COMMENT);
        artist.addAttribute(address);
        map.addObjEntity(artist);

        // a subclass that overrides an inherited attribute
        ObjEntity famousArtist = new ObjEntity("FamousArtist");
        famousArtist.setClassName("org.example.FamousArtist");
        famousArtist.setSuperEntityName("Artist");
        famousArtist.addAttributeOverride("name", "NAME");
        map.addObjEntity(famousArtist);

        ObjEntity painting = new ObjEntity("Painting");
        painting.setClassName("org.example.Painting");
        painting.setDbEntityName("PAINTING");
        ObjAttribute title = new ObjAttribute("title", "java.lang.String", painting);
        title.setDbAttributePath("TITLE");
        painting.addAttribute(title);
        map.addObjEntity(painting);

        ObjRelationship paintings = new ObjRelationship("paintings");
        paintings.setSourceEntity(artist);
        paintings.setTargetEntityName("Painting");
        paintings.setDbRelationshipPath("paintings");
        paintings.setCollectionType("java.util.Map");
        paintings.setMapKey("title");
        paintings.setDeleteRule(DeleteRule.CASCADE);
        paintings.setUsedForLocking(true);
        paintings.setComment(COMMENT);
        artist.addRelationship(paintings);
    }

    private static void addQueries(DataMap map, Procedure procedure) {
        NamedSelectQuery select = new NamedSelectQuery();
        select.setName("select");
        select.setQl("from Artist a where a.name like 'A%' and a.fee > 1");
        setQuerySettings(select);
        select.setComment(COMMENT);
        map.addNamedQuery(select);

        NamedSQLQuery sql = new NamedSQLQuery();
        sql.setName("sql");
        sql.setRoot(map.getObjEntity("Artist"));
        sql.setSql("SELECT * FROM ARTIST WHERE FEE > 1");
        sql.setAdapterSql(Map.of("org.apache.cayenne.dba.h2.H2Adapter", "SELECT * FROM ARTIST WHERE FEE <> 1"));
        sql.setColumnNamesCapitalization(CapsStrategy.UPPER);
        sql.addPrefetch("paintings", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
        setQuerySettings(sql);
        sql.setComment(COMMENT);
        map.addNamedQuery(sql);

        NamedSQLQuery dataMapSql = new NamedSQLQuery();
        dataMapSql.setName("dataMapSql");
        dataMapSql.setRoot(map);
        dataMapSql.setSql("DELETE FROM PAINTING");
        map.addNamedQuery(dataMapSql);

        NamedProcedureQuery procedureQuery = new NamedProcedureQuery();
        procedureQuery.setName("procedure");
        procedureQuery.setRoot(procedure);
        procedureQuery.setResultEntityName("Artist");
        procedureQuery.setFetchLimit(5);
        procedureQuery.setFetchOffset(2);
        procedureQuery.setColumnNamesCapitalization(CapsStrategy.LOWER);
        setQuerySettings(procedureQuery);
        procedureQuery.setComment(COMMENT);
        map.addNamedQuery(procedureQuery);
    }

    private static void setQuerySettings(NamedQuery query) {
        query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        query.setCacheGroup("group");
        query.setFetchingDataRows(true);
        query.setPageSize(10);
        query.setStatementFetchSize(20);
    }

    private static ReverseEngineering createReverseEngineering() {
        ReverseEngineering reverseEngineering = new ReverseEngineering();
        addFilters(reverseEngineering);

        Catalog catalog = new Catalog("CATALOG");
        addFilters(catalog);
        Schema catalogSchema = new Schema("SCHEMA_01");
        addFilters(catalogSchema);
        catalog.addSchema(catalogSchema);
        reverseEngineering.addCatalog(catalog);

        Schema schema = new Schema("SCHEMA_02");
        addFilters(schema);
        reverseEngineering.addSchema(schema);

        reverseEngineering.setTableTypes(List.of("TABLE", "VIEW"));
        reverseEngineering.setDefaultPackage("org.example.imported");
        reverseEngineering.setForceDataMapCatalog(true);
        reverseEngineering.setForceDataMapSchema(true);
        reverseEngineering.setMeaningfulPkTables("ARTIST");
        reverseEngineering.setNamingStrategy("org.example.NameGenerator");
        reverseEngineering.setSkipPrimaryKeyLoading(true);
        reverseEngineering.setSkipRelationshipsLoading(true);
        reverseEngineering.setStripFromTableNames("^T_");
        reverseEngineering.setUseJava7Types(true);
        return reverseEngineering;
    }

    private static void addFilters(FilterContainer container) {
        IncludeTable includeTable = new IncludeTable("ARTIST");
        includeTable.addIncludeColumn(new IncludeColumn("ID"));
        includeTable.addExcludeColumn(new ExcludeColumn("TMP"));
        container.addIncludeTable(includeTable);
        container.addExcludeTable(new ExcludeTable("OLD_.*"));
        container.addIncludeColumn(new IncludeColumn(".*_ID"));
        container.addExcludeColumn(new ExcludeColumn("SECRET"));
        container.addIncludeProcedure(new IncludeProcedure("PROC_.*"));
        container.addExcludeProcedure(new ExcludeProcedure("PROC_OLD"));
    }

    private static void addCgenDescriptors(DataMap map) {
        CgenDescriptor custom = new CgenDescriptor();
        custom.setName("custom");
        custom.setDestDir("../java");
        custom.setMode("all");
        custom.setMakePairs(false);
        custom.setUsePkgPath(false);
        custom.setOverwrite(true);
        custom.setCreatePropertyNames(true);
        custom.setCreatePKProperties(false);
        custom.setOutputPattern("*.groovy");
        custom.setSuperPkg("org.example.auto");
        custom.setExternalToolConfig("tools.properties");
        custom.setTemplate("custom/subclass.vm");
        custom.setSuperTemplate("custom/superclass.vm");
        custom.setEmbeddableTemplate("custom/embeddable-subclass.vm");
        custom.setEmbeddableSuperTemplate("custom/embeddable-superclass.vm");
        custom.setDataMapTemplate("custom/datamap-subclass.vm");
        custom.setDataMapSuperTemplate("custom/datamap-superclass.vm");
        custom.setExcludedEntities(List.of("Painting"));
        custom.setExcludedEmbeddables(List.of("org.example.Address"));
        map.addCgenDescriptor(custom);

        CgenDescriptor defaults = new CgenDescriptor();
        defaults.setName("default");
        map.addCgenDescriptor(defaults);
    }

    private static void assertContentMatchesSchema(String xml, String schemaFileName) throws Exception {
        Map<String, Content> declared = declaredContent(schemaFileName);
        Map<String, Content> found = documentContent(xml);
        assertEquals(declared.keySet(), found.keySet());
        declared.forEach((name, content) -> assertEquals(content, found.get(name), name));
    }

    private static Map<String, Content> declaredContent(String schemaFileName) throws Exception {
        Element schema = parse(schema(schemaFileName)).getDocumentElement();
        Map<String, Element> types = topLevelDeclarations(schema, "complexType");
        Map<String, Element> attributeGroups = topLevelDeclarations(schema, "attributeGroup");

        Map<String, Content> content = new TreeMap<>();
        NodeList elements = schema.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "element");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            if (element.hasAttribute("name")) {
                Element type = types.get(localName(element.getAttribute("type")));
                addDeclaredContent(type != null ? type : element, types, attributeGroups,
                        content.computeIfAbsent(element.getAttribute("name"), name -> new Content()));
            }
        }
        return content;
    }

    // doesn't descend into the child element declarations
    private static void addDeclaredContent(Element declaration, Map<String, Element> types,
                                           Map<String, Element> attributeGroups, Content content) {
        for (Element child : childElements(declaration)) {
            switch (child.getLocalName()) {
                case "element" -> content.children().add(child.hasAttribute("name")
                        ? child.getAttribute("name")
                        : localName(child.getAttribute("ref")));
                case "attribute" -> content.attributes().add(child.getAttribute("name"));
                case "attributeGroup" ->
                        addDeclaredContent(attributeGroups.get(localName(child.getAttribute("ref"))), types,
                                attributeGroups, content);
                case "extension" -> {
                    Element baseType = types.get(localName(child.getAttribute("base")));
                    if (baseType != null) {
                        addDeclaredContent(baseType, types, attributeGroups, content);
                    }
                    addDeclaredContent(child, types, attributeGroups, content);
                }
                default -> addDeclaredContent(child, types, attributeGroups, content);
            }
        }
    }

    private static Map<String, Element> topLevelDeclarations(Element schema, String kind) {
        Map<String, Element> declarations = new HashMap<>();
        for (Element child : childElements(schema)) {
            if (kind.equals(child.getLocalName())) {
                declarations.put(child.getAttribute("name"), child);
            }
        }
        return declarations;
    }

    private static String localName(String qualifiedName) {
        return qualifiedName.substring(qualifiedName.indexOf(':') + 1);
    }

    private static Map<String, Content> documentContent(String xml) throws Exception {
        Map<String, Content> content = new TreeMap<>();
        NodeList elements = parse(xml).getElementsByTagNameNS("*", "*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            Content elementContent = content.computeIfAbsent(element.getLocalName(), name -> new Content());

            NamedNodeMap attributes = element.getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                // skip the namespace declarations and the schema location
                Node attribute = attributes.item(j);
                if (attribute.getNamespaceURI() == null) {
                    elementContent.attributes().add(attribute.getLocalName());
                }
            }

            childElements(element).forEach(child -> elementContent.children().add(child.getLocalName()));
        }
        return content;
    }

    private record Content(Set<String> attributes, Set<String> children) {

        Content() {
            this(new TreeSet<>(), new TreeSet<>());
        }
    }
}
