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

package org.apache.cayenne.project.dbimport;

import org.apache.cayenne.project.EmptyProjectNodeVisitor;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReverseEngineeringTest {

    @Test
    public void encodeAsXML() {
        IncludeTable includeTable = new IncludeTable("ARTIST & <PAINTING>");
        includeTable.addIncludeColumn(new IncludeColumn("ID|NAME"));
        includeTable.addExcludeColumn(new ExcludeColumn("(?<tmp>TMP_.*)"));

        Catalog catalog = new Catalog("CATALOG_01");
        catalog.addExcludeTable(new ExcludeTable("CATALOG_TMP"));
        catalog.addSchema(new Schema("SCHEMA_01"));
        Schema schema = new Schema("SCHEMA_02");
        schema.addIncludeTable(new IncludeTable("GALLERY"));

        ReverseEngineering reverseEngineering = new ReverseEngineering();
        reverseEngineering.addIncludeTable(includeTable);
        reverseEngineering.addExcludeTable(new ExcludeTable("A&B"));
        reverseEngineering.addIncludeColumn(new IncludeColumn(".*_ID"));
        reverseEngineering.addExcludeColumn(new ExcludeColumn("SECRET"));
        reverseEngineering.addIncludeProcedure(new IncludeProcedure("PROC_.*"));
        reverseEngineering.addExcludeProcedure(new ExcludeProcedure("PROC_OLD"));
        reverseEngineering.addCatalog(catalog);
        reverseEngineering.addSchema(schema);
        reverseEngineering.setTableTypes(List.of("TABLE", "VIEW"));
        reverseEngineering.setDefaultPackage("org.example");
        reverseEngineering.setForceDataMapCatalog(true);
        reverseEngineering.setMeaningfulPkTables("ARTIST");
        reverseEngineering.setNamingStrategy("org.example.NameGenerator");
        reverseEngineering.setSkipRelationshipsLoading(true);
        reverseEngineering.setStripFromTableNames("^T_");

        // the patterns are escaped
        assertEquals("""
                <dbImport>
                \t<includeTable>
                \t\t<name>ARTIST &amp; &lt;PAINTING&gt;</name>
                \t\t<includeColumn>ID|NAME</includeColumn>
                \t\t<excludeColumn>(?&lt;tmp&gt;TMP_.*)</excludeColumn>
                \t</includeTable>
                \t<excludeTable>A&amp;B</excludeTable>
                \t<includeColumn>.*_ID</includeColumn>
                \t<excludeColumn>SECRET</excludeColumn>
                \t<includeProcedure>PROC_.*</includeProcedure>
                \t<excludeProcedure>PROC_OLD</excludeProcedure>
                \t<catalog>
                \t\t<excludeTable>CATALOG_TMP</excludeTable>
                \t\t<name>CATALOG_01</name>
                \t\t<schema>
                \t\t\t<name>SCHEMA_01</name>
                \t\t</schema>
                \t</catalog>
                \t<schema>
                \t\t<includeTable>
                \t\t\t<name>GALLERY</name>
                \t\t</includeTable>
                \t\t<name>SCHEMA_02</name>
                \t</schema>
                \t<tableTypes>
                \t\t<tableType>TABLE</tableType>
                \t\t<tableType>VIEW</tableType>
                \t</tableTypes>
                \t<defaultPackage>org.example</defaultPackage>
                \t<forceDataMapCatalog>true</forceDataMapCatalog>
                \t<forceDataMapSchema>false</forceDataMapSchema>
                \t<meaningfulPkTables>ARTIST</meaningfulPkTables>
                \t<namingStrategy>org.example.NameGenerator</namingStrategy>
                \t<skipPrimaryKeyLoading>false</skipPrimaryKeyLoading>
                \t<skipRelationshipsLoading>true</skipRelationshipsLoading>
                \t<stripFromTableNames>^T_</stripFromTableNames>
                \t<useJava7Types>false</useJava7Types>
                </dbImport>
                """, encode(reverseEngineering));
    }

    @Test
    public void encodeAsXML_Defaults() {
        // the flags and the naming strategy are written even with their default values
        assertEquals("""
                <dbImport>
                \t<forceDataMapCatalog>false</forceDataMapCatalog>
                \t<forceDataMapSchema>false</forceDataMapSchema>
                \t<namingStrategy>org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator</namingStrategy>
                \t<skipPrimaryKeyLoading>false</skipPrimaryKeyLoading>
                \t<skipRelationshipsLoading>false</skipRelationshipsLoading>
                \t<useJava7Types>false</useJava7Types>
                </dbImport>
                """, encode(new ReverseEngineering()));
    }

    private static String encode(ReverseEngineering reverseEngineering) {
        StringWriter out = new StringWriter();
        try (PrintWriter writer = new PrintWriter(out)) {
            reverseEngineering.encodeAsXML(new XMLEncoder(writer, "\t"), new EmptyProjectNodeVisitor());
        }
        return out.toString().replace(System.lineSeparator(), "\n");
    }
}
