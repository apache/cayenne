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
package org.apache.cayenne.access.sqlbuilder;

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.SchemaCompiler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BaseSqlBuilderTest {

    /**
     * Compiles a standalone authored table into a CompiledTable, the type the builders take.
     */
    protected static CompiledTable compiledTable(String name, String catalog, String schema) {
        DbEntity entity = new DbEntity(name);
        entity.setCatalog(catalog);
        entity.setSchema(schema);
        DataMap dataMap = new DataMap("test");
        dataMap.addDbEntity(entity);
        return SchemaCompiler.compile(List.of(dataMap)).table(name);
    }

    protected void assertSQL(String expected, Node node) {
        assertSQL(expected, node, new DefaultSQLAppendable(null));
    }

    protected void assertQuotedSQL(String expected, Node node) {
        assertSQL(expected, node, new TestSQLAppendable());
    }

    protected void assertSQL(String expected, Node node, SQLAppendable appendable) {
        SQLGenerationVisitor visitor = new SQLGenerationVisitor(appendable, null);
        node.visit(visitor);
        assertEquals(expected, visitor.getSQLString());
    }

    protected static class TestSQLAppendable extends DefaultSQLAppendable {

        public TestSQLAppendable() {
            super(null);
        }

        @Override
        public SQLAppendable appendQuoted(String str) {
            append('`').append(str).append('`');
            return this;
        }
    }
}
