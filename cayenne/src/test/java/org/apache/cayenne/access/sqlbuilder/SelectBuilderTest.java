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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.SelectNode;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

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
    public void testValidSelectCaseWhen() {
        SelectBuilder builder = new SelectBuilder(column("OrderID"), column("Quantity"),
                caseWhen(column("Quantity").gt(value(30)).and(column("Quantity").lt(value(100))))
                        .then(value("The quantity from 30 to 100"))
                        .elseResult(value("The quantity is under 30"))
                        .when(column("Quantity").eq(value(30)))
                        .then(value("The quantity is 30"))
                        .as("QuantityText"))
                .from(table("OrderDetails"));

        Node node = builder.build();
        assertThat(node, instanceOf(SelectNode.class));
        assertSQL("SELECT OrderID, Quantity, " +
                "CASE " +
                    "WHEN ( ( Quantity > 30 ) AND ( Quantity < 100 ) ) THEN 'The quantity from 30 to 100' " +
                    "WHEN ( Quantity = 30 ) THEN 'The quantity is 30' " +
                    "ELSE 'The quantity is under 30' " +
                    "END QuantityText " +
                "FROM OrderDetails", node);
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testInvalidSelectCaseWhen() {
        select(column("OrderID"), column("Quantity"),
                caseWhen(column("Quantity").gt(value(30)))
                        .then(value("The quantity is greater than 30"))
                        .when(column("Quantity").eq(value(30))))
                .from(table("OrderDetails"));
    }

    @Test
    public void testQueryAlias() {
        SelectBuilder innerSelect = select(table("p").column("PAINTING_TITLE"))
                .from(table("PAINTING").as("p"));
        Node node = select(table("t").column("*"))
                .from(aliased(innerSelect, "t"))
                .build();

        assertThat(node, instanceOf(SelectNode.class));
        assertSQL("SELECT t.* FROM (SELECT p.PAINTING_TITLE FROM PAINTING p) t", node);
    }

    @Test
    public void testFunctionAlias() {
        Node node = select(function("test", table("p").column("PAINTING_TITLE")).as("f"))
                .from(table("PAINTING").as("p"))
                .orderBy(function("test", table("p").column("PAINTING_TITLE")).as("f"))
                .build();

        assertThat(node, instanceOf(SelectNode.class));
        assertSQL("SELECT test( p.PAINTING_TITLE ) f FROM PAINTING p ORDER BY f", node);
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
                .orderBy(count(table("p").column("PAINTING_TITLE")).as("p_count").desc(), column("a_id").asc())
                .build();
        assertThat(node, instanceOf(SelectNode.class));
        assertSQL("SELECT DISTINCT" +
                    " a.ARTIST_ID a_id, COUNT( p.PAINTING_TITLE ) p_count" +
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