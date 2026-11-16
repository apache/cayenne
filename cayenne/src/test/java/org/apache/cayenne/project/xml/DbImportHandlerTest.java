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
import org.apache.cayenne.project.ProjectLoadingMode;
import org.apache.cayenne.project.dbimport.Catalog;
import org.apache.cayenne.project.dbimport.IncludeTable;
import org.apache.cayenne.project.dbimport.ReverseEngineering;
import org.apache.cayenne.project.dbimport.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.cayenne.project.xml.XMLTestUtils.patterns;
import static org.junit.jupiter.api.Assertions.*;

public class DbImportHandlerTest extends BaseHandlerTest {

    private static final String DB_IMPORT = """
            <dataMap>
                <dbImport>
                    <includeTable>
                        <name>ARTIST</name>
                        <includeColumn>ARTIST_ID</includeColumn>
                        <includeColumn>ARTIST_NAME</includeColumn>
                        <excludeColumn>ARTIST_TMP</excludeColumn>
                    </includeTable>
                    <includeTable>
                        <name>PAINTING</name>
                    </includeTable>
                    <excludeTable>OLD_.*</excludeTable>
                    <excludeTable>TMP_.*</excludeTable>
                    <includeColumn>.*_ID</includeColumn>
                    <excludeColumn>SECRET</excludeColumn>
                    <includeProcedure>PROC_.*</includeProcedure>
                    <excludeProcedure>PROC_OLD</excludeProcedure>
                    <catalog>
                        <includeTable>
                            <name>GALLERY</name>
                        </includeTable>
                        <excludeTable>CATALOG_TMP</excludeTable>
                        <includeColumn>CATALOG_ID</includeColumn>
                        <excludeColumn>CATALOG_SECRET</excludeColumn>
                        <includeProcedure>CATALOG_PROC</includeProcedure>
                        <excludeProcedure>CATALOG_PROC_OLD</excludeProcedure>
                        <name>CATALOG_01</name>
                        <schema>
                            <includeTable>
                                <name>EXHIBIT</name>
                                <excludeColumn>EXHIBIT_TMP</excludeColumn>
                            </includeTable>
                            <excludeTable>SCHEMA_TMP</excludeTable>
                            <name>SCHEMA_01</name>
                        </schema>
                        <schema>
                            <name>SCHEMA_02</name>
                        </schema>
                    </catalog>
                    <catalog>
                        <name>CATALOG_02</name>
                    </catalog>
                    <schema>
                        <includeColumn>SCHEMA_ID</includeColumn>
                        <excludeColumn>SCHEMA_SECRET</excludeColumn>
                        <includeProcedure>SCHEMA_PROC</includeProcedure>
                        <excludeProcedure>SCHEMA_PROC_OLD</excludeProcedure>
                        <name>SCHEMA_03</name>
                    </schema>
                    <tableTypes>
                        <tableType>TABLE</tableType>
                        <tableType>VIEW</tableType>
                    </tableTypes>
                    <defaultPackage>org.example</defaultPackage>
                    <forceDataMapCatalog>true</forceDataMapCatalog>
                    <forceDataMapSchema>true</forceDataMapSchema>
                    <meaningfulPkTables>ARTIST</meaningfulPkTables>
                    <namingStrategy>org.example.NameGenerator</namingStrategy>
                    <skipPrimaryKeyLoading>true</skipPrimaryKeyLoading>
                    <skipRelationshipsLoading>true</skipRelationshipsLoading>
                    <stripFromTableNames>^T_</stripFromTableNames>
                    <useJava7Types>true</useJava7Types>
                    <usePrimitives>true</usePrimitives>
                </dbImport>
                <dbEntity name="ARTIST">
                    <dbAttribute name="ARTIST_ID" type="INTEGER"/>
                </dbEntity>
            </dataMap>
            """;

    @Test
    public void loadReverseEngineering() throws Exception {
        DataMap map = parseDataMap(DB_IMPORT, ProjectLoadingMode.FULL);
        ReverseEngineering reverseEngineering = map.getReverseEngineering();
        assertNotNull(reverseEngineering);

        assertEquals(List.of("ARTIST", "PAINTING"), patterns(reverseEngineering.getIncludeTables()));
        IncludeTable artist = reverseEngineering.getIncludeTables().get(0);
        assertEquals(List.of("ARTIST_ID", "ARTIST_NAME"), patterns(artist.getIncludeColumns()));
        assertEquals(List.of("ARTIST_TMP"), patterns(artist.getExcludeColumns()));
        IncludeTable painting = reverseEngineering.getIncludeTables().get(1);
        assertTrue(painting.getIncludeColumns().isEmpty());
        assertTrue(painting.getExcludeColumns().isEmpty());
        assertEquals(List.of("OLD_.*", "TMP_.*"), patterns(reverseEngineering.getExcludeTables()));
        assertEquals(List.of(".*_ID"), patterns(reverseEngineering.getIncludeColumns()));
        assertEquals(List.of("SECRET"), patterns(reverseEngineering.getExcludeColumns()));
        assertEquals(List.of("PROC_.*"), patterns(reverseEngineering.getIncludeProcedures()));
        assertEquals(List.of("PROC_OLD"), patterns(reverseEngineering.getExcludeProcedures()));

        assertEquals(2, reverseEngineering.getCatalogs().size());
        Catalog catalog = reverseEngineering.getCatalogs().get(0);
        assertEquals("CATALOG_01", catalog.getName());
        assertEquals(List.of("GALLERY"), patterns(catalog.getIncludeTables()));
        assertEquals(List.of("CATALOG_TMP"), patterns(catalog.getExcludeTables()));
        assertEquals(List.of("CATALOG_ID"), patterns(catalog.getIncludeColumns()));
        assertEquals(List.of("CATALOG_SECRET"), patterns(catalog.getExcludeColumns()));
        assertEquals(List.of("CATALOG_PROC"), patterns(catalog.getIncludeProcedures()));
        assertEquals(List.of("CATALOG_PROC_OLD"), patterns(catalog.getExcludeProcedures()));

        assertEquals(2, catalog.getSchemas().size());
        Schema catalogSchema = catalog.getSchemas().get(0);
        assertEquals("SCHEMA_01", catalogSchema.getName());
        assertEquals(List.of("EXHIBIT"), patterns(catalogSchema.getIncludeTables()));
        assertEquals(List.of("EXHIBIT_TMP"), patterns(catalogSchema.getIncludeTables().get(0).getExcludeColumns()));
        assertEquals(List.of("SCHEMA_TMP"), patterns(catalogSchema.getExcludeTables()));
        assertEquals("SCHEMA_02", catalog.getSchemas().get(1).getName());
        assertTrue(catalog.getSchemas().get(1).isEmptyContainer());

        Catalog emptyCatalog = reverseEngineering.getCatalogs().get(1);
        assertEquals("CATALOG_02", emptyCatalog.getName());
        assertTrue(emptyCatalog.isEmptyContainer());

        assertEquals(1, reverseEngineering.getSchemas().size());
        Schema schema = reverseEngineering.getSchemas().get(0);
        assertEquals("SCHEMA_03", schema.getName());
        assertEquals(List.of("SCHEMA_ID"), patterns(schema.getIncludeColumns()));
        assertEquals(List.of("SCHEMA_SECRET"), patterns(schema.getExcludeColumns()));
        assertEquals(List.of("SCHEMA_PROC"), patterns(schema.getIncludeProcedures()));
        assertEquals(List.of("SCHEMA_PROC_OLD"), patterns(schema.getExcludeProcedures()));

        assertArrayEquals(new String[]{"TABLE", "VIEW"}, reverseEngineering.getTableTypes());
        assertEquals("org.example", reverseEngineering.getDefaultPackage());
        assertTrue(reverseEngineering.isForceDataMapCatalog());
        assertTrue(reverseEngineering.isForceDataMapSchema());
        assertEquals("ARTIST", reverseEngineering.getMeaningfulPkTables());
        assertEquals("org.example.NameGenerator", reverseEngineering.getNamingStrategy());
        assertTrue(reverseEngineering.getSkipPrimaryKeyLoading());
        assertTrue(reverseEngineering.getSkipRelationshipsLoading());
        assertEquals("^T_", reverseEngineering.getStripFromTableNames());
        assertTrue(reverseEngineering.isUseJava7Types());

        assertNotNull(map.getDbEntity("ARTIST"));
    }

    @Test
    public void skipReverseEngineering() throws Exception {
        DataMap map = parseDataMap(DB_IMPORT, ProjectLoadingMode.RUNTIME);

        assertNull(map.getReverseEngineering());
        assertNotNull(map.getDbEntity("ARTIST"));
    }

    @Test
    public void skipReverseEngineeringOfOtherNamespace() throws Exception {
        // the pre-v15 "dbimport" extension namespace, not converted in a current version DataMap
        DataMap map = parseDataMap("""
                <dataMap>
                    <dbImport xmlns="http://cayenne.apache.org/schema/15/dbimport">
                        <defaultPackage>org.example</defaultPackage>
                    </dbImport>
                </dataMap>
                """, ProjectLoadingMode.FULL);

        assertNull(map.getReverseEngineering());
    }

    @Test
    public void loadReverseEngineeringBlankValues() throws Exception {
        DataMap map = parseDataMap("""
                <dataMap>
                    <dbImport>
                        <includeTable>
                            <name> </name>
                            <includeColumn>ID</includeColumn>
                        </includeTable>
                        <excludeTable> </excludeTable>
                        <catalog>
                            <name> </name>
                            <includeColumn></includeColumn>
                        </catalog>
                        <tableTypes>
                            <tableType> </tableType>
                        </tableTypes>
                        <namingStrategy> </namingStrategy>
                        <skipRelationshipsLoading></skipRelationshipsLoading>
                    </dbImport>
                </dataMap>
                """, ProjectLoadingMode.FULL);

        ReverseEngineering reverseEngineering = map.getReverseEngineering();
        ReverseEngineering defaults = new ReverseEngineering();
        assertTrue(reverseEngineering.getIncludeTables().isEmpty());
        assertTrue(reverseEngineering.getExcludeTables().isEmpty());
        assertEquals(1, reverseEngineering.getCatalogs().size());
        assertNull(reverseEngineering.getCatalogs().get(0).getName());
        assertTrue(reverseEngineering.getCatalogs().get(0).isEmptyContainer());
        assertEquals(0, reverseEngineering.getTableTypes().length);
        assertEquals(defaults.getNamingStrategy(), reverseEngineering.getNamingStrategy());
        assertFalse(reverseEngineering.getSkipRelationshipsLoading());
    }
}
