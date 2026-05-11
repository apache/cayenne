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
import org.junit.jupiter.api.Test;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class InsertBuilderTest extends BaseSqlBuilderTest  {

    @Test
    public void insert() {
        InsertBuilder builder = new InsertBuilder("test");
        Node node = builder.build();
        assertInstanceOf(InsertNode.class, node);
        assertSQL("INSERT INTO test", node);
    }

    @Test
    public void insertDbEntityCatalog() {
        DbEntity entity = new DbEntity("test");
        entity.setCatalog("catalog");
        InsertBuilder builder = new InsertBuilder(entity);
        Node node = builder.build();
        assertInstanceOf(InsertNode.class, node);
        assertSQL("INSERT INTO catalog.test", node);
        assertQuotedSQL("INSERT INTO `catalog`.`test`", node);
    }

    @Test
    public void insertDbEntityCatalogAndSchema() {
        DbEntity entity = new DbEntity("test");
        entity.setSchema("schema");
        entity.setCatalog("catalog");
        InsertBuilder builder = new InsertBuilder(entity);
        Node node = builder.build();
        assertInstanceOf(InsertNode.class, node);
        assertSQL("INSERT INTO catalog.schema.test", node);
        assertQuotedSQL("INSERT INTO `catalog`.`schema`.`test`", node);
    }

    @Test
    public void insertWithColumns() {
        InsertBuilder builder = new InsertBuilder("test");
        builder
                .column(column("col1"))
                .column(column("col2"))
                .column(column("col3"));
        Node node = builder.build();

        assertInstanceOf(InsertNode.class, node);
        assertSQL("INSERT INTO test( col1, col2, col3)", node);
    }

    @Test
    public void insertWithValues() {
        InsertBuilder builder = new InsertBuilder("test");
        builder
                .value(value(1))
                .value(value("test"))
                .value(value(null));
        Node node = builder.build();

        assertInstanceOf(InsertNode.class, node);
        assertSQL("INSERT INTO test VALUES( 1, 'test', NULL)", node);
    }

    @Test
    public void insertWithColumnsAndValues() {
        InsertBuilder builder = new InsertBuilder("test");
        builder
                .column(column("col1"))
                .value(value(1))
                .column(column("col2"))
                .value(value("test"))
                .column(column("col3"))
                .value(value(null));
        Node node = builder.build();

        assertInstanceOf(InsertNode.class, node);
        assertSQL("INSERT INTO test( col1, col2, col3) VALUES( 1, 'test', NULL)", node);
    }

}