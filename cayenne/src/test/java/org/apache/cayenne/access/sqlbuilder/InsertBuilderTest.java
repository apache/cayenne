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

import org.apache.cayenne.access.sqlbuilder.sqltree.InsertNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class InsertBuilderTest extends BaseSqlBuilderTest  {

    @Test
    public void testInsert() {
        InsertBuilder builder = new InsertBuilder("test");
        Node node = builder.build();
        assertThat(node, instanceOf(InsertNode.class));
        assertSQL("INSERT INTO test", node);
    }

    @Test
    public void testInsertDbEntityCatalog() {
        DbEntity entity = new DbEntity("test");
        entity.setCatalog("catalog");
        InsertBuilder builder = new InsertBuilder(entity);
        Node node = builder.build();
        assertThat(node, instanceOf(InsertNode.class));
        assertSQL("INSERT INTO catalog.test", node);
        assertQuotedSQL("INSERT INTO `catalog`.`test`", node);
    }

    @Test
    public void testInsertDbEntityCatalogAndSchema() {
        DbEntity entity = new DbEntity("test");
        entity.setSchema("schema");
        entity.setCatalog("catalog");
        InsertBuilder builder = new InsertBuilder(entity);
        Node node = builder.build();
        assertThat(node, instanceOf(InsertNode.class));
        assertSQL("INSERT INTO catalog.schema.test", node);
        assertQuotedSQL("INSERT INTO `catalog`.`schema`.`test`", node);
    }

    @Test
    public void testInsertWithColumns() {
        InsertBuilder builder = new InsertBuilder("test");
        builder
                .column(column("col1"))
                .column(column("col2"))
                .column(column("col3"));
        Node node = builder.build();

        assertThat(node, instanceOf(InsertNode.class));
        assertSQL("INSERT INTO test( col1, col2, col3)", node);
    }

    @Test
    public void testInsertWithValues() {
        InsertBuilder builder = new InsertBuilder("test");
        builder
                .value(value(1))
                .value(value("test"))
                .value(value(null));
        Node node = builder.build();

        assertThat(node, instanceOf(InsertNode.class));
        assertSQL("INSERT INTO test VALUES( 1, 'test', NULL)", node);
    }

    @Test
    public void testInsertWithColumnsAndValues() {
        InsertBuilder builder = new InsertBuilder("test");
        builder
                .column(column("col1"))
                .value(value(1))
                .column(column("col2"))
                .value(value("test"))
                .column(column("col3"))
                .value(value(null));
        Node node = builder.build();

        assertThat(node, instanceOf(InsertNode.class));
        assertSQL("INSERT INTO test( col1, col2, col3) VALUES( 1, 'test', NULL)", node);
    }

}