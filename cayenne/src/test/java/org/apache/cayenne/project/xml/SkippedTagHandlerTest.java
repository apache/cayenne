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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.NamedSQLQuery;
import org.apache.cayenne.map.NamedSelectQuery;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.project.DefaultConfigurationNameMapper;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectLoadingMode;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.apache.cayenne.project.xml.XMLTestUtils.patterns;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SkippedTagHandlerTest extends BaseHandlerTest {

    // unknown tags at various levels, with nested tags named like the known ones that they could overwrite
    private static final String MAP_WITH_UNKNOWN_TAGS = """
            <dataMap xmlns:ext="http://example.org/ext">
                <comment>map comment</comment>
                <unknown name="unknown">
                    unknown text
                    <comment>skipped</comment>
                    <dbEntity name="SKIPPED">
                        <dbAttribute name="ID" type="INTEGER"/>
                    </dbEntity>
                </unknown>
                <ext:unknown>
                    <dbEntity name="SKIPPED_EXT"/>
                </ext:unknown>
                <dbEntity name="ARTIST">
                    <dbAttribute name="ID" type="INTEGER" primaryKey="true">
                        <unknown>
                            <comment>skipped</comment>
                        </unknown>
                        <ext:unknown>
                            <comment>skipped</comment>
                        </ext:unknown>
                    </dbAttribute>
                    <ext:unknown>
                        <dbAttribute name="SKIPPED" type="INTEGER"/>
                    </ext:unknown>
                    <dbAttribute name="NAME" type="VARCHAR" length="100"/>
                    <qualifier>NAME = "test"</qualifier>
                    <unknown>
                        <qualifier>NAME = "skipped"</qualifier>
                    </unknown>
                </dbEntity>
                <objEntity name="Artist" className="org.example.Artist" dbEntityName="ARTIST">
                    <comment>objEntity comment</comment>
                    <unknown>
                        <comment>skipped</comment>
                        <objAttribute name="skipped" type="java.lang.String"/>
                    </unknown>
                    <objAttribute name="name" type="java.lang.String" dbAttributePath="NAME">
                        <comment>objAttribute comment</comment>
                        <ext:unknown>
                            <comment>skipped</comment>
                        </ext:unknown>
                    </objAttribute>
                </objEntity>
                <objectQuery name="objectQuery">
                    <unknown/>
                    <ql>from Artist</ql>
                    <cacheGroup>group</cacheGroup>
                </objectQuery>
                <sqlQuery name="sqlQuery" root="dataMap">
                    <ext:unknown/>
                    <sql>select 1</sql>
                    <unknown>
                        <sql>select 2</sql>
                    </unknown>
                </sqlQuery>
                <ext:unknown/>
            </dataMap>
            """;

    private static final String MAP_WITH_TEXT_TAGS_OF_OTHER_NAMESPACE = """
            <dataMap xmlns:ext="http://example.org/ext">
                <ext:comment>skipped</ext:comment>
                <comment>map comment</comment>
                <ext:comment>skipped</ext:comment>
                <dbEntity name="ARTIST">
                    <ext:comment>skipped</ext:comment>
                    <comment>dbEntity comment</comment>
                    <ext:comment>skipped</ext:comment>
                    <dbAttribute name="ID" type="INTEGER">
                        <ext:comment>skipped</ext:comment>
                        <comment>dbAttribute comment</comment>
                        <ext:comment>skipped</ext:comment>
                    </dbAttribute>
                    <ext:qualifier>ID = 1</ext:qualifier>
                    <qualifier>ID = 2</qualifier>
                    <ext:qualifier>ID = 3</ext:qualifier>
                </dbEntity>
                <objEntity name="Artist" className="org.example.Artist" dbEntityName="ARTIST">
                    <ext:qualifier>id = 1</ext:qualifier>
                    <qualifier>id = 2</qualifier>
                    <ext:qualifier>id = 3</ext:qualifier>
                </objEntity>
                <objectQuery name="objectQuery">
                    <ext:ql>from Skipped</ext:ql>
                    <ql>from Artist</ql>
                    <ext:ql>from Skipped</ext:ql>
                    <ext:cacheGroup>skipped</ext:cacheGroup>
                    <cacheGroup>group</cacheGroup>
                    <ext:cacheGroup>skipped</ext:cacheGroup>
                </objectQuery>
                <sqlQuery name="sqlQuery" root="dataMap">
                    <ext:sql>select 0</ext:sql>
                    <sql>select 1</sql>
                    <ext:sql>select 2</ext:sql>
                </sqlQuery>
                <dbImport>
                    <ext:excludeTable>SKIPPED_BEFORE</ext:excludeTable>
                    <excludeTable>OLD_.*</excludeTable>
                    <ext:excludeTable>SKIPPED_AFTER</ext:excludeTable>
                </dbImport>
                <cgen>
                    <ext:name>skipped</ext:name>
                    <name>Custom</name>
                    <ext:name>skipped</ext:name>
                </cgen>
            </dataMap>
            """;

    @Test
    public void skipUnknownTags() throws Exception {
        DataMap map = parseDataMap(MAP_WITH_UNKNOWN_TAGS, ProjectLoadingMode.RUNTIME);

        assertEquals(Set.of("ARTIST"), map.getDbEntityMap().keySet());
        assertEquals(Set.of("Artist"), map.getObjEntityMap().keySet());
        assertEquals(Set.of("objectQuery", "sqlQuery"), map.getNamedQueryMap().keySet());
        assertEquals(Set.of("ID", "NAME"), map.getDbEntity("ARTIST").getAttributeMap().keySet());
        assertEquals(Set.of("name"), map.getObjEntity("Artist").getAttributeMap().keySet());

        assertEquals("NAME = \"test\"", map.getDbEntity("ARTIST").getQualifier().toString());
        NamedSelectQuery objectQuery = (NamedSelectQuery) map.getNamedQuery("objectQuery");
        assertEquals("from Artist", objectQuery.getQl());
        assertEquals("group", objectQuery.getCacheGroup());
        assertEquals("select 1", ((NamedSQLQuery) map.getNamedQuery("sqlQuery")).getSql());
    }

    @Test
    public void loadCommentsNextToUnknownTags() throws Exception {
        DataMap map = parseDataMap(MAP_WITH_UNKNOWN_TAGS, ProjectLoadingMode.FULL);

        ObjEntity artist = map.getObjEntity("Artist");
        assertEquals("map comment", map.getComment());
        assertEquals("objEntity comment", artist.getComment());
        assertEquals("objAttribute comment", artist.getAttribute("name").getComment());
        assertNull(map.getDbEntity("ARTIST").getAttribute("ID").getComment());
    }

    @Test
    public void skipTextTagsOfOtherNamespace() throws Exception {
        DataMap map = parseDataMap(MAP_WITH_TEXT_TAGS_OF_OTHER_NAMESPACE, ProjectLoadingMode.RUNTIME);

        assertEquals("ID = 2", map.getDbEntity("ARTIST").getQualifier().toString());
        assertEquals("id = 2", map.getObjEntity("Artist").getDeclaredQualifier().toString());
        NamedSelectQuery objectQuery = (NamedSelectQuery) map.getNamedQuery("objectQuery");
        assertEquals("from Artist", objectQuery.getQl());
        assertEquals("group", objectQuery.getCacheGroup());
        assertEquals("select 1", ((NamedSQLQuery) map.getNamedQuery("sqlQuery")).getSql());
    }

    @Test
    public void skipToolOnlyTextTagsOfOtherNamespace() throws Exception {
        DataMap map = parseDataMap(MAP_WITH_TEXT_TAGS_OF_OTHER_NAMESPACE, ProjectLoadingMode.FULL);

        DbEntity artist = map.getDbEntity("ARTIST");
        assertEquals("map comment", map.getComment());
        assertEquals("dbEntity comment", artist.getComment());
        assertEquals("dbAttribute comment", artist.getAttribute("ID").getComment());
        assertEquals(List.of("OLD_.*"), patterns(map.getReverseEngineering().getExcludeTables()));
        assertEquals(1, map.getCgenDescriptors().size());
        assertEquals("Custom", map.getCgenDescriptors().get(0).getName());
    }

    @Test
    public void skipUnknownProjectTags() throws Exception {
        Project project = parseProject("""
                <project xmlns="http://cayenne.apache.org/schema/15/project" xmlns:ext="http://example.org/ext">
                    <ext:map name="skipped"/>
                    <map name="map1"/>
                    <unknown>
                        <map name="skipped"/>
                    </unknown>
                    <ext:unknown>
                        <map name="skipped"/>
                    </ext:unknown>
                    <validation>
                        <ext:exclude>SKIPPED_BEFORE</ext:exclude>
                        <exclude>PROJECT_NO_NAME</exclude>
                        <ext:exclude>SKIPPED_AFTER</ext:exclude>
                    </validation>
                </project>
                """);

        assertEquals(List.of("map1"), project.getDataMaps().stream().map(DataMap::getName).toList());
        assertEquals(Set.of("PROJECT_NO_NAME"), project.getDisabledInspections());
    }

    private static Project parseProject(String xml) throws Exception {
        Project project = new Project();
        project.setConfigurationSource(new URLResource(new URL("file:/project/cayenne-project.xml")));

        XMLReader parser = new XMLReaderProvider(false).get();
        ProjectHandler handler = new ProjectHandler(project, new LoaderContext(parser, ProjectLoadingMode.FULL),
                new DefaultConfigurationNameMapper(), source -> new DataMap());
        parser.setContentHandler(handler);
        parser.parse(new InputSource(new StringReader(xml)));
        return project;
    }
}
