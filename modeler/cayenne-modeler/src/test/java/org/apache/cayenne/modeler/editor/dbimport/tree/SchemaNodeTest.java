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

public class SchemaNodeTest extends BaseNodeTest {

    private SchemaNode node;

    @Before
    public void createNode() {
        CatalogNode catalogNode = new CatalogNode("catalog");
        node = new SchemaNode("schema", catalogNode);
    }

    @Test
    public void testIncludeEmptyConfig() {
        config = config().build();
        assertIncluded(node);
    }

    @Test
    public void testIncludeSchema() {
        config = config().schema(schema("schema")).build();
        assertIncluded(node);
    }

    @Test
    public void testIncludeMultipleSchemas() {
        config = config().schema(schema("schema")).schema(schema("schema1")).build();
        assertIncluded(node);
    }

    @Test
    public void testNoIncludeSchema() {
        config = config().schema(schema("schema1")).build();
        assertExcludedImplicitly(node);
    }
}
