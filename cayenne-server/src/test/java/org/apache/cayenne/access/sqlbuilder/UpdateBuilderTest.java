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
import org.apache.cayenne.access.sqlbuilder.sqltree.UpdateNode;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @since 4.2
 */
public class UpdateBuilderTest extends BaseSqlBuilderTest {

    @Test
    public void testUpdate() {
        UpdateBuilder builder = new UpdateBuilder("test");
        Node node = builder.build();
        assertThat(node, instanceOf(UpdateNode.class));
        assertSQL("UPDATE test", node);
    }

    @Test
    public void testUpdateDbEntityCatalog() {
        DbEntity entity = new DbEntity("test");
        entity.setCatalog("catalog");
        UpdateBuilder builder = new UpdateBuilder(entity);
        Node node = builder.build();
        assertThat(node, instanceOf(UpdateNode.class));
        assertSQL("UPDATE catalog.test", node);
        assertQuotedSQL("UPDATE `catalog`.`test`", node);
    }

    @Test
    public void testUpdateDbEntityCatalogAndSchema() {
        DbEntity entity = new DbEntity("test");
        entity.setSchema("schema");
        entity.setCatalog("catalog");
        UpdateBuilder builder = new UpdateBuilder(entity);
        Node node = builder.build();
        assertThat(node, instanceOf(UpdateNode.class));
        assertSQL("UPDATE catalog.schema.test", node);
        assertQuotedSQL("UPDATE `catalog`.`schema`.`test`", node);
    }

    @Test
    public void testUpdateWithFields() {
        UpdateBuilder builder = new UpdateBuilder("test");
        builder
                .set(column("col1").eq(value(1)))
                .set(column("col2").eq(value("test")))
                .set(column("col3").eq(value(null)));
        Node node = builder.build();

        assertThat(node, instanceOf(UpdateNode.class));
        assertSQL("UPDATE test SET col1 = 1, col2 = 'test', col3 = NULL", node);
    }

    @Test
    public void testUpdateWithWhere() {
        UpdateBuilder builder = new UpdateBuilder("test");
        builder
                .set(column("col1").eq(value(1)))
                .where(column("id").eq(value(123L)));
        Node node = builder.build();

        assertThat(node, instanceOf(UpdateNode.class));
        assertSQL("UPDATE test SET col1 = 1 WHERE id = 123", node);
    }

}