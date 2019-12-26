/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.editor.dbimport.tree;

import org.junit.Before;
import org.junit.Test;

public class ColumnNodeTest extends BaseNodeTest {

    private ColumnNode node;

    @Before
    public void createNode() {
        CatalogNode catalogNode = new CatalogNode("catalog");
        SchemaNode schemaNode = new SchemaNode("schema", catalogNode);
        SchemaTableNode tableNode = new SchemaTableNode("table1", schemaNode);
        node = new ColumnNode("id", tableNode);
    }

    @Test
    public void rootEmpty() {
        config = config().build();

        assertIncluded(node);
    }

    @Test
    public void rootIncludeTable() {
        config = config().includeTable("table1").build();

        assertIncluded(node);
    }

    @Test
    public void rootNoIncludeTable() {
        config = config().includeTable("table2").build();

        assertExcludedImplicitly(node);
    }

    @Test
    public void rootExcludeTable() {
        config = config().excludeTable("table1").build();

        assertExcludedExplicitly(node);
    }

    @Test
    public void rootIncludeColumn() {
        config = config().includeColumn("id").build();

        assertIncluded(node);
    }

    @Test
    public void rootNoIncludeColumn() {
        config = config().includeColumn("name").build();

        assertExcludedImplicitly(node);
    }

    @Test
    public void rootExcludeColumn() {
        config = config().excludeColumn("id").build();

        assertExcludedExplicitly(node);
    }

    @Test
    public void schemaIncludeAll() {
        config = config().schema(schema("schema")).build();

        assertIncluded(node);
    }

    @Test
    public void schemaIncludeTable() {
        config = config().schema(schema("schema").includeTable("table1")).build();

        assertIncluded(node);
    }

    @Test
    public void schemaNoIncludeTable() {
        config = config().schema(schema("schema").includeTable("table2")).build();

        assertExcludedImplicitly(node);
    }

    @Test
    public void schemaExcludeTable() {
        config = config().schema(schema("schema").excludeTable("table1").includeTable("table2")).build();

        assertExcludedExplicitly(node);
    }


    @Test
    public void schemaIncludeColumn() {
        config = config().schema(schema("schema").includeColumn("id")).build();

        assertIncluded(node);
    }

    @Test
    public void schemaNoIncludeColumn() {
        config = config().schema(schema("schema").includeColumn("name")).build();

        assertExcludedImplicitly(node);
    }

    @Test
    public void schemaExcludeColumn() {
        config = config().schema(schema("schema").excludeColumn("id").includeColumn("name")).build();

        assertExcludedExplicitly(node);
    }


    @Test
    public void schemaIncludeTableRootNoIncludeTable() {
        config = config()
                .includeTable("table2")
                .schema(schema("schema").includeTable("table1")).build();

        assertIncluded(node);
    }

    @Test
    public void schemaNoIncludeTableRootNoIncludeTable() {
        config = config()
                .includeTable("table2")
                .schema(schema("schema")).build();

        assertExcludedImplicitly(node);
    }

    @Test
    public void schemaIncludeColumnRootNoIncludeColumn() {
        config = config()
                .includeColumn("name")
                .schema(schema("schema").includeColumn("id")).build();

        assertIncluded(node);
    }

    @Test
    public void schemaNoIncludeColumnRootNoIncludeColumn() {
        config = config()
                .includeColumn("name")
                .schema(schema("schema")).build();

        assertExcludedImplicitly(node);
    }


    @Test
    public void tableIncludeAll() {
        config = config().includeTable(table("table1")).build();

        assertIncluded(node);
    }

    @Test
    public void tableInclude() {
        config = config().includeTable(table("table1").includeColumn("id")).build();

        assertIncluded(node);
    }

    @Test
    public void tableNoInclude() {
        config = config().includeTable(table("table1").includeColumn("name")).build();

        assertExcludedImplicitly(node);
    }

    @Test
    public void tableExclude() {
        config = config().includeTable(table("table1").excludeColumn("id")).build();

        assertExcludedExplicitly(node);
    }

    @Test
    public void tableNoExclude() {
        config = config().includeTable(table("table1").excludeColumn("name")).build();

        assertIncluded(node);
    }

    @Test
    public void tableIncludeExclude() {
        config = config().includeTable(table("table1").excludeColumn("name").includeColumn("id")).build();

        assertIncluded(node);
    }
}
