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
package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTSubquery;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObjectSelect_ParseTest {

    private static List<Property<?>> columns(FluentSelect<?, ?> query) {
        return new ArrayList<>(query.getColumns());
    }

    private static List<Ordering> orderings(FluentSelect<?, ?> query) {
        return new ArrayList<>(query.getOrderings());
    }

    private static FluentSelect<?, ?> subquery(Expression parent, int operand) {
        return assertInstanceOf(ASTSubquery.class, parent.getOperand(operand)).getQuery();
    }

    @Test
    public void fromOnly() {
        FluentSelect<?, ?> q = ObjectSelect.parse("from Artist");

        assertInstanceOf(ObjectSelect.class, q);
        assertEquals("Artist", q.getEntityName());
        assertNull(q.getDbEntityName());
        assertNull(q.getWhere());
        assertNull(q.getColumns());
        assertFalse(q.isFetchingDataRows());
    }

    @Test
    public void selectSelf() {
        FluentSelect<?, ?> q = ObjectSelect.parse("select self from Artist");

        assertInstanceOf(ObjectSelect.class, q);
        assertEquals("Artist", q.getEntityName());
        assertNull(q.getColumns());
    }

    @Test
    public void selectDistinctSelf() {
        FluentSelect<?, ?> q = ObjectSelect.parse("select distinct self from Artist");

        assertInstanceOf(ObjectSelect.class, q);
        assertTrue(q.isDistinct());
    }

    @Test
    public void whereOrderLimitOffset() {
        FluentSelect<?, ?> q = ObjectSelect.parse(
                "from Artist where artistName like 'A%' order by dateOfBirth desc, artistName asc insensitive "
                        + "limit 10 offset 20");

        assertEquals(Artist.ARTIST_NAME.like("A%"), q.getWhere());
        assertEquals(10, q.getLimit());
        assertEquals(20, q.getOffset());

        List<Ordering> orderings = orderings(q);
        assertEquals(2, orderings.size());
        assertEquals(Artist.DATE_OF_BIRTH.desc(), orderings.get(0));
        assertEquals(Artist.ARTIST_NAME.ascInsensitive(), orderings.get(1));
    }

    @Test
    public void orderByDefaultsAndInsensitive() {
        List<Ordering> orderings = orderings(ObjectSelect.parse(
                "from Artist order by a, b insensitive, c desc insensitive, count(paintingArray) desc"));

        assertEquals(4, orderings.size());
        assertEquals(SortOrder.ASCENDING, orderings.get(0).getSortOrder());
        assertEquals(SortOrder.ASCENDING_INSENSITIVE, orderings.get(1).getSortOrder());
        assertEquals(SortOrder.DESCENDING_INSENSITIVE, orderings.get(2).getSortOrder());
        assertEquals(SortOrder.DESCENDING, orderings.get(3).getSortOrder());
        assertEquals(Artist.PAINTING_ARRAY.count().getExpression(), orderings.get(3).getSortSpec());
    }

    @Test
    public void keywordsAreValidPropertyNames() {
        FluentSelect<?, ?> q = ObjectSelect.parse("from from where where = 1 and order > limit order by offset, as");

        assertEquals("from", q.getEntityName());
        assertEquals(ExpressionFactory.exp("where = 1 and order > limit"), q.getWhere());
        assertEquals(2, orderings(q).size());

        // expressions are not affected by the query keywords
        assertEquals("(select = 1) and (from = 2)", ExpressionFactory.exp("select = 1 and from = 2").toString());
    }

    @Test
    public void positionalParameters() {
        FluentSelect<?, ?> q = ObjectSelect.parse(
                "from Painting where paintingTitle = $t and estimatedPrice between $low and $high "
                        + "or paintingTitle = $t", "T", 10, 500);

        assertEquals(ExpressionFactory.exp(
                "paintingTitle = 'T' and estimatedPrice between 10 and 500 or paintingTitle = 'T'"), q.getWhere());
    }

    @Test
    public void parametersAcrossClausesAndSubqueries() {
        FluentSelect<?, ?> q = ObjectSelect.parse(
                "select artistName from Artist where artistName in (select artistName from Artist where x = $a) "
                        + "and y = $b having count(*) > $c", 1, 2, 3L);

        assertEquals(ExpressionFactory.exp("y = 2"), q.getWhere().getOperand(1));
        assertEquals(ExpressionFactory.exp("x = 1"), subquery((Expression) q.getWhere().getOperand(0), 1).getWhere());
        assertEquals(ExpressionFactory.exp("count(*) > 3L"), q.getHaving());
    }

    @Test
    public void parameterCountMismatch() {
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist where a = $a"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist where a = $a", 1, 2));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist", 1));
    }

    @Test
    public void collectionParameter() {
        FluentSelect<?, ?> q = ObjectSelect.parse("from Artist where db:ARTIST_ID in $ids", List.of(1, 2));
        assertEquals(ExpressionFactory.inDbExp("ARTIST_ID", 1, 2), q.getWhere());
    }

    @Test
    public void columns() {
        FluentSelect<?, ?> q = ObjectSelect.parse("select artistName, dateOfBirth from Artist order by artistName");

        ColumnSelect<?> columnSelect = assertInstanceOf(ColumnSelect.class, q);
        assertFalse(columnSelect.isSingleColumn());
        assertEquals("Artist", q.getEntityName());

        List<Property<?>> columns = columns(q);
        assertEquals(2, columns.size());
        assertEquals(Artist.ARTIST_NAME.getExpression(), columns.get(0).getExpression());
        assertNull(columns.get(0).getType());
        assertEquals(Artist.DATE_OF_BIRTH.getExpression(), columns.get(1).getExpression());
    }

    @Test
    public void singleDistinctColumn() {
        FluentSelect<?, ?> q = ObjectSelect.parse("select distinct paintingArray.paintingTitle from Artist");

        ColumnSelect<?> columnSelect = assertInstanceOf(ColumnSelect.class, q);
        assertTrue(columnSelect.isSingleColumn());
        assertTrue(q.isDistinct());
        assertEquals(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).getExpression(),
                columns(q).get(0).getExpression());
    }

    @Test
    public void selfWithAggregateAndHaving() {
        FluentSelect<?, ?> q = ObjectSelect.parse(
                "select self, count(paintingArray) as n from Artist having count(paintingArray) > 2");

        List<Property<?>> columns = columns(q);
        assertEquals(2, columns.size());
        assertEquals(Artist.SELF.getExpression(), columns.get(0).getExpression());
        assertEquals(Artist.PAINTING_ARRAY.count().getExpression(), columns.get(1).getExpression());
        assertEquals("n", columns.get(1).getAlias());
        assertEquals(Artist.PAINTING_ARRAY.count().gt(2L), ExpressionFactory.exp("count(paintingArray) > 2L"));
        assertEquals(ExpressionFactory.exp("count(paintingArray) > 2"), q.getHaving());
    }

    @Test
    public void functionsAndOuterJoins() {
        List<Property<?>> columns = columns(ObjectSelect.parse(
                "select year(dateOfBirth), upper(artistName), count(*), count(distinct(artistName)), toGallery+, "
                        + "max(estimatedPrice) from Painting"));

        assertEquals(6, columns.size());
        assertEquals(ExpressionFactory.exp("year(dateOfBirth)"), columns.get(0).getExpression());
        assertEquals(ExpressionFactory.exp("upper(artistName)"), columns.get(1).getExpression());
        assertEquals(ExpressionFactory.exp("count(*)"), columns.get(2).getExpression());
        assertEquals(ExpressionFactory.exp("count(distinct(artistName))"), columns.get(3).getExpression());
        assertEquals(Painting.TO_GALLERY.outer().getExpression(), columns.get(4).getExpression());
        assertEquals(ExpressionFactory.exp("max(estimatedPrice)"), columns.get(5).getExpression());
    }

    @Test
    public void prefetch() {
        FluentSelect<?, ?> q = ObjectSelect.parse(
                "from Artist where artistName = $name limit 10 prefetch paintingArray joint, "
                        + "artistExhibitArray disjointById, paintingArray.toGallery", "X");

        PrefetchTreeNode root = q.getPrefetches();
        assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, root.getNode("paintingArray").getSemantics());
        assertEquals(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS,
                root.getNode("artistExhibitArray").getSemantics());
        assertEquals(PrefetchTreeNode.UNDEFINED_SEMANTICS, root.getNode("paintingArray.toGallery").getSemantics());
    }

    @Test
    public void existsSubquery() {
        FluentSelect<?, ?> q = ObjectSelect.parse(
                "from Artist where exists (select self from Painting where toArtist = enclosing(self) "
                        + "and estimatedPrice > 1000)");

        assertEquals(Expression.EXISTS, q.getWhere().getType());
        FluentSelect<?, ?> subquery = subquery(q.getWhere(), 0);
        assertInstanceOf(ObjectSelect.class, subquery);
        assertEquals("Painting", subquery.getEntityName());
        Expression expected = Painting.TO_ARTIST.eq(Artist.SELF.enclosing())
                .andExp(ExpressionFactory.greaterExp("estimatedPrice", 1000));
        assertEquals(expected.toString(), subquery.getWhere().toString());
    }

    @Test
    public void multilineQuery() {
        FluentSelect<?, ?> q = ObjectSelect.parse("""
                from Artist where exists (
                    from Painting where toArtist = enclosing(self) and estimatedPrice > $price)
                order by artistName""", 1000);

        assertEquals("Painting", subquery(q.getWhere(), 0).getEntityName());
        assertEquals(1, orderings(q).size());
    }

    @Test
    public void notExistsSubquery() {
        FluentSelect<?, ?> q = ObjectSelect.parse(
                "from Artist where artistName = 'a' and not exists (from Painting where toArtist = enclosing(self))");

        Expression notExists = (Expression) q.getWhere().getOperand(1);
        assertEquals(Expression.NOT_EXISTS, notExists.getType());
        assertEquals("Painting", subquery(notExists, 0).getEntityName());
    }

    @Test
    public void existsPathIsPreserved() {
        FluentSelect<?, ?> q = ObjectSelect.parse("from Artist where exists paintingArray and not exists (a = 1)");
        assertEquals(ExpressionFactory.exp("exists paintingArray and not exists (a = 1)"), q.getWhere());
    }

    @Test
    public void inSubquery() {
        FluentSelect<?, ?> q = ObjectSelect.parse(
                "from Painting where toArtist.artistName not in "
                        + "(select artistName from Artist where dateOfBirth < $d)", 5);

        assertEquals(Expression.NOT_IN, q.getWhere().getType());
        ColumnSelect<?> subquery = assertInstanceOf(ColumnSelect.class, subquery(q.getWhere(), 1));
        assertTrue(subquery.isSingleColumn());
        assertEquals(ExpressionFactory.exp("dateOfBirth < 5"), subquery.getWhere());
    }

    @Test
    public void allAndAnySubqueries() {
        FluentSelect<?, ?> q = ObjectSelect.parse(
                "from Painting where estimatedPrice > all (select estimatedPrice from Painting where x = 'P2') "
                        + "or estimatedPrice = any (select max(estimatedPrice) from Painting)");

        Expression all = (Expression) ((Expression) q.getWhere().getOperand(0)).getOperand(1);
        assertEquals(Expression.ALL, all.getType());
        assertEquals(ExpressionFactory.exp("x = 'P2'"), subquery(all, 0).getWhere());

        Expression any = (Expression) ((Expression) q.getWhere().getOperand(1)).getOperand(1);
        assertEquals(Expression.ANY, any.getType());
        assertEquals("Painting", subquery(any, 0).getEntityName());
    }

    @Test
    public void subqueryInExpression() {
        Expression exp = ExpressionFactory.exp(
                "artistName in (select toArtist.artistName from Painting where estimatedPrice > $p limit 5)", 100);

        FluentSelect<?, ?> subquery = subquery(exp, 1);
        assertEquals(ExpressionFactory.greaterExp("estimatedPrice", 100), subquery.getWhere());
        assertEquals(5, subquery.getLimit());
    }

    @Test
    public void subqueryRejectsPrefetch() {
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse(
                "from Artist where exists (from Painting where toArtist = enclosing(self) prefetch toGallery)"));
    }

    @Test
    public void nestedEnclosing() {
        Expression exp = ExpressionFactory.exp("a = enclosing(enclosing(artistName))");
        Expression enclosing = (Expression) exp.getOperand(1);
        assertEquals(Artist.ARTIST_NAME.enclosing().enclosing().getExpression(), enclosing);
    }

    @Test
    public void selfAndEnclosingRoundTrip() {
        Expression exp = Painting.TO_ARTIST.eq(Artist.SELF.enclosing());
        assertEquals("toArtist = enclosing(self)", exp.toString());
        assertEquals(exp.toString(), ExpressionFactory.exp(exp.toString()).toString());

        // "self" stays usable as a property name with a prefix
        assertEquals(ExpressionFactory.pathExp("self"), ExpressionFactory.exp("obj:self"));
    }

    @Test
    public void dbRoot() {
        FluentSelect<?, ?> q = ObjectSelect.parse("from db:ARTIST where ARTIST_NAME like 'A%' order by ARTIST_ID desc");

        assertInstanceOf(ObjectSelect.class, q);
        assertEquals("ARTIST", q.getDbEntityName());
        assertNull(q.getEntityName());
        assertTrue(q.isFetchingDataRows());
        assertEquals(ExpressionFactory.likeDbExp("ARTIST_NAME", "A%"), q.getWhere());
        assertEquals(ExpressionFactory.dbPathExp("ARTIST_ID"), orderings(q).get(0).getSortSpec());
    }

    @Test
    public void typedParse() {
        ObjectSelect<Artist> objects = ObjectSelect.parse(Artist.class, "from Artist where artistName = $n", "a");
        assertEquals(Artist.ARTIST_NAME.eq("a"), objects.getWhere());
        assertFalse(objects.isFetchingDataRows());

        ObjectSelect<DataRow> rows = ObjectSelect.parse(DataRow.class, "from Artist");
        assertTrue(rows.isFetchingDataRows());

        ColumnSelect<String> column = ObjectSelect.parseColumn(String.class, "select artistName from Artist");
        assertTrue(column.isSingleColumn());

        ColumnSelect<Object[]> columns = ObjectSelect.parseColumns("select artistName, count(*) from Artist");
        assertEquals(2, columns.getColumns().size());
    }

    @Test
    public void typedParseShapeMismatch() {
        assertThrows(ExpressionException.class,
                () -> ObjectSelect.parse(Artist.class, "select artistName from Artist"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parseColumn(String.class, "from Artist"));
        assertThrows(ExpressionException.class,
                () -> ObjectSelect.parseColumn(String.class, "select a, b from Artist"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parseColumns("select a from Artist"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parseColumns("from Artist"));
    }

    @Test
    public void syntaxErrors() {
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("select a"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist.name"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist where"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist limit x"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist limit 1 where a = 1"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist prefetch a lazy"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist order artistName"));
    }
}
