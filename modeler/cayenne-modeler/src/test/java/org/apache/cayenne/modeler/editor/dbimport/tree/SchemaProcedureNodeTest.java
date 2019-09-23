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

public class SchemaProcedureNodeTest extends BaseNodeTest {

    private SchemaProcedureNode node;

    @Before
    public void createNode() {
        CatalogNode catalogNode = new CatalogNode("catalog");
        SchemaNode schemaNode = new SchemaNode("schema", catalogNode);
        node = new SchemaProcedureNode("procedure1", schemaNode);
    }

    @Test
    public void rootEmpty() {
        config = config().build();

        assertIncluded(node);
    }

    @Test
    public void rootInclude() {
        config = config().includeProcedure("procedure1").build();

        assertIncluded(node);
    }

    @Test
    public void rootNoInclude() {
        config = config().includeProcedure("procedure2").build();

        assertExcludedImplicitly(node);
    }

    @Test
    public void rootExclude() {
        config = config().excludeProcedure("procedure1").build();

        assertExcludedExplicitly(node);
    }

    @Test
    public void schemaIncludeAll() {
        config = config().schema(schema("schema")).build();

        assertIncluded(node);
    }

    @Test
    public void schemaInclude() {
        config = config().schema(schema("schema").includeProcedure("procedure1")).build();

        assertIncluded(node);
    }

    @Test
    public void schemaNoInclude() {
        config = config().schema(schema("schema").includeProcedure("procedure2")).build();

        assertExcludedImplicitly(node);
    }

    @Test
    public void schemaExclude() {
        config = config().schema(schema("schema").excludeProcedure("procedure1").includeProcedure("procedure2")).build();

        assertExcludedExplicitly(node);
    }

    @Test
    public void schemaIncludeRootNoInclude() {
        config = config()
                .includeProcedure("procedure2")
                .schema(schema("schema").includeProcedure("procedure1")).build();

        assertIncluded(node);
    }

    @Test
    public void schemaNoIncludeRootNoInclude() {
        config = config()
                .includeProcedure("procedure2")
                .schema(schema("schema")).build();

        assertExcludedImplicitly(node);
    }
}