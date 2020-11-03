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
import org.apache.cayenne.access.sqlbuilder.sqltree.SelectNode;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class SelectBuilderTest extends BaseSqlBuilderTest  {

    @Test
    public void testSelect() {
        SelectBuilder builder = new SelectBuilder();
        Node node = builder.build();
        assertThat(node, instanceOf(SelectNode.class));
        assertSQL("SELECT", node);
    }

    @Test
    public void testSelectColumns() {
        SelectBuilder builder = new SelectBuilder(column("a"), table("c").column("b"));
        Node node = builder.build();
        assertThat(node, instanceOf(SelectNode.class));
        assertSQL("SELECT a, c.b", node);
        assertQuotedSQL("SELECT `a`, `c`.`b`", node);
    }

    @Test
    public void testSelectFrom() {
        SelectBuilder builder = new SelectBuilder(column("a")).from(table("b"));
        Node node = builder.build();
        assertThat(node, instanceOf(SelectNode.class));
        assertSQL("SELECT a FROM b", node);
        assertQuotedSQL("SELECT `a` FROM `b`", node);
    }

    @Test
    public void testSelectFromDbEntity() {
        DbEntity entity = new DbEntity("b");
        entity.setSchema("d");
        entity.setCatalog("c");
        SelectBuilder builder = new SelectBuilder(column("a")).from(table(entity));
        Node node = builder.build();
        assertThat(node, instanceOf(SelectNode.class));
        assertSQL("SELECT a FROM c.d.b", node);
        assertQuotedSQL("SELECT `a` FROM `c`.`d`.`b`", node);
    }

    @Test
    public void testSelectFromWhere() {
        SelectBuilder builder = new SelectBuilder(column("a"))
                .from(table("b"))
                .where(column("a").eq(value(123)));
        Node node = builder.build();
        assertThat(node, instanceOf(SelectNode.class));
        assertSQL("SELECT a FROM b WHERE a = 123", node);
    }

    @Test
    public void testSelectFromWhereNull() {
        SelectBuilder builder = new SelectBuilder(column("a"))
                .from(table("b"))
                .where(column("a").eq(value(null)));
        Node node = builder.build();
        assertThat(node, instanceOf(SelectNode.class));
        assertSQL("SELECT a FROM b WHERE a IS NULL", node);
    }

    @Test
    public void testSelectFromWhereComplex() {
        SelectBuilder builder = new SelectBuilder(column("a"))
                .from(table("b"))
                .where(column("a").eq(value(123)).and(column("c").lt(column("d"))));
        Node node = builder.build();
        assertThat(node, instanceOf(SelectNode.class));
        assertSQL("SELECT a FROM b WHERE ( a = 123 ) AND ( c < d )", node);
    }

    @Test
    public void testComplexQuery() {
        Node node = select(
                        table("a").column("ARTIST_ID").as("a_id"),
                        count(table("p").column("PAINTING_TITLE")).as("p_count"))
                .distinct()
                .from(table("ARTIST").as("a"))
                .from(leftJoin(table("PAINTING").as("p"))
                        .on(table("a").column("ARTIST_ID")
                                .eq(table("p").column("ARTIST_ID"))
                                .and(table("p").column("ESTIMATED_PRICE").gt(value(10)))))
                .where(
                        table("a").column("ARTIST_NAME")
                                .eq(value("Picasso"))
                                .and(exists(select(all())
                                        .from(table("GALLERY").as("g"))
                                        .where(table("g").column("GALLERY_ID").eq(table("p").column("GALLERY_ID")))))
                                .and(value(1).eq(value(1)))
                                .or(value(false))
                )
                .groupBy(table("a").column("ARTIST_ID"))
                .having(not(count(table("p").column("PAINTING_TITLE")).gt(value(3))))
                .orderBy(column("p_count").desc(), column("a_id").asc())
                .build();
        assertThat(node, instanceOf(SelectNode.class));
        assertSQL("SELECT DISTINCT" +
                    " a.ARTIST_ID a_id, COUNT( p.PAINTING_TITLE ) AS p_count" +
                " FROM ARTIST a" +
                " LEFT JOIN PAINTING p ON ( a.ARTIST_ID = p.ARTIST_ID ) AND ( p.ESTIMATED_PRICE > 10 )" +
                " WHERE ( ( ( a.ARTIST_NAME = 'Picasso' )" +
                    " AND EXISTS (SELECT * FROM GALLERY g WHERE g.GALLERY_ID = p.GALLERY_ID) )" +
                    " AND ( 1 = 1 ) ) OR false" +
                " GROUP BY a.ARTIST_ID" +
                " HAVING NOT ( COUNT( p.PAINTING_TITLE ) > 3 )" +
                " ORDER BY p_count DESC, a_id", node);
    }

}