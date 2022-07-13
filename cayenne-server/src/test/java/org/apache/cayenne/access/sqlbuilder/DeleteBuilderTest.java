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

import org.apache.cayenne.access.sqlbuilder.sqltree.DeleteNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class DeleteBuilderTest extends BaseSqlBuilderTest {

    @Test
    public void testDelete() {
        DeleteBuilder builder = new DeleteBuilder("test");
        Node node = builder.build();
        assertThat(node, instanceOf(DeleteNode.class));
        assertSQL("DELETE FROM test", node);
    }

    @Test
    public void testDeleteWithQualifier() {
        DeleteBuilder builder = new DeleteBuilder("test");
        Node node = builder.where(
                column("col1").eq(value(1))
                        .and(column("col2").eq(value("test")))
                        .and(column("col3").eq(value(null)))
        ).build();
        assertThat(node, instanceOf(DeleteNode.class));
        assertSQL("DELETE FROM test WHERE ( ( col1 = 1 ) AND ( col2 = 'test' ) ) AND ( col3 IS NULL )", node);
    }

    @Test
    public void testDeleteDbEntityCatalog() {
        DbEntity entity = new DbEntity("test");
        entity.setCatalog("catalog");
        DeleteBuilder builder = new DeleteBuilder(entity);
        Node node = builder.build();
        assertThat(node, instanceOf(DeleteNode.class));
        assertSQL("DELETE FROM catalog.test", node);
        assertQuotedSQL("DELETE FROM `catalog`.`test`", node);
    }

    @Test
    public void testDeleteDbEntityCatalogAndSchema() {
        DbEntity entity = new DbEntity("test");
        entity.setSchema("schema");
        entity.setCatalog("catalog");
        DeleteBuilder builder = new DeleteBuilder(entity);
        Node node = builder.build();
        assertThat(node, instanceOf(DeleteNode.class));
        assertSQL("DELETE FROM catalog.schema.test", node);
        assertQuotedSQL("DELETE FROM `catalog`.`schema`.`test`", node);
    }

}