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
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.ExpressionParameter;
import org.apache.cayenne.exp.SubqueryExp;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.compiled.TestMappings;
import org.apache.cayenne.ql.ParseException;
import org.apache.cayenne.query.resolved.ResolvedSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.cayenne.exp.AllExp;
import org.apache.cayenne.exp.AnyExp;
import org.apache.cayenne.exp.ExistsExp;
import org.apache.cayenne.exp.NotExistsExp;
import org.apache.cayenne.exp.NotInExp;

public class ObjectSelect_ParseTest {

    private static List<Property<?>> columns(ObjectSelect<?> query) {
        return new ArrayList<>(query.columns);
    }

    private static List<Ordering> orderings(ObjectSelect<?> query) {
        return new ArrayList<>(query.orderings);
    }

    private static ObjectSelect<?> subquery(Expression parent, int operand) {
        return assertInstanceOf(SubqueryExp.class, parent.getOperand(operand)).getQuery();
    }

    // whether a query fetches DataRows is only visible once the query is resolved against a model
    private static ResolvedSelect resolve(ObjectSelect<?> query) {
        DataMap map = new DataMap("map");
        map.addObjEntity(new ObjEntity("Artist"));
        map.addDbEntity(new DbEntity("ARTIST"));
        return query.resolve(TestMappings.compile(map));
    }

    @Test
    public void fromOnly() {
        ObjectSelect<?> q = ObjectSelect.parse("from Artist");

        assertInstanceOf(ObjectSelect.class, q);
        assertEquals("Artist", q.entityName);
        assertNull(q.dbEntityName);
        assertNull(q.where);
        assertNull(q.columns);
        assertFalse(resolve(q).fetchingDataRows());
    }

    @Test
    public void selectSelf() {
        ObjectSelect<?> q = ObjectSelect.parse("select self from Artist");

        assertInstanceOf(ObjectSelect.class, q);
        assertEquals("Artist", q.entityName);
        assertNull(q.columns);
    }

    @Test
    public void selectDistinctSelf() {
        ObjectSelect<?> q = ObjectSelect.parse("select distinct self from Artist");

        assertInstanceOf(ObjectSelect.class, q);
        assertEquals("select distinct self from Artist", q.toQL());
    }

    @Test
    public void offsetWithoutLimit() {
        ObjectSelect<?> q = ObjectSelect.parse("from Artist offset 20");
        assertEquals("from Artist offset 20", q.toQL());
    }

    @Test
    public void whereOrderLimitOffset() {
        ObjectSelect<?> q = ObjectSelect.parse(
                "from Artist where artistName like 'A%' order by dateOfBirth desc, artistName asc insensitive "
                        + "limit 10 offset 20");

        assertEquals(Artist.ARTIST_NAME.like("A%"), q.where);
        assertTrue(q.toQL().endsWith(" limit 10 offset 20"), q.toQL());

        List<Ordering> orderings = orderings(q);
        assertEquals(2, orderings.size());
        assertEquals(Artist.DATE_OF_BIRTH.desc(), orderings.get(0));
        assertEquals(Artist.ARTIST_NAME.ascInsensitive(), orderings.get(1));
    }

    @Test
    public void orderByDefaultsAndInsensitive() {
        List<Ordering> orderings = orderings(ObjectSelect.parse(
                "from Artist order by a, b insensitive, c desc insensitive, count(paintings) desc"));

        assertEquals(4, orderings.size());
        assertEquals(SortOrder.ASCENDING, orderings.get(0).getSortOrder());
        assertEquals(SortOrder.ASCENDING_INSENSITIVE, orderings.get(1).getSortOrder());
        assertEquals(SortOrder.DESCENDING_INSENSITIVE, orderings.get(2).getSortOrder());
        assertEquals(SortOrder.DESCENDING, orderings.get(3).getSortOrder());
        assertEquals(Artist.PAINTINGS.count().getExpression(), orderings.get(3).getSortSpec());
    }

    @Test
    public void keywordsAreValidPropertyNames() {
        ObjectSelect<?> q = ObjectSelect.parse("from from where where = 1 and order > limit order by offset, as");

        assertEquals("from", q.entityName);
        assertEquals(ExpressionFactory.exp("where = 1 and order > limit"), q.where);
        assertEquals(2, orderings(q).size());

        // expressions are not affected by the query keywords
        assertEquals("(select = 1) and (from = 2)", ExpressionFactory.exp("select = 1 and from = 2").toString());
    }

    @Test
    public void positionalParameters() {
        ObjectSelect<?> q = ObjectSelect.parse(
                "from Painting where paintingTitle = $t and estimatedPrice between $low and $high "
                        + "or paintingTitle = $t", "T", 10, 500);

        assertEquals(ExpressionFactory.exp(
                "paintingTitle = 'T' and estimatedPrice between 10 and 500 or paintingTitle = 'T'"), q.where);
    }

    @Test
    public void parametersAcrossClausesAndSubqueries() {
        ObjectSelect<?> q = ObjectSelect.parse(
                "select artistName from Artist where artistName in (select artistName from Artist where x = $a) "
                        + "and y = $b having count(*) > $c", 1, 2, 3L);

        assertEquals(ExpressionFactory.exp("y = 2"), q.where.getOperand(1));
        assertEquals(ExpressionFactory.exp("x = 1"), subquery((Expression) q.where.getOperand(0), 1).where);
        assertEquals(ExpressionFactory.exp("count(*) > 3L"), q.having);
    }

    @Test
    public void parameterCountMismatch() {
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist where a = $a and b = $b", 1));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist where a = $a", 1, 2));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist", 1));
    }

    @Test
    public void noValuesPreserveParameters() {
        ObjectSelect<?> q = ObjectSelect.parse(
                "from Artist where a = $a and exists (from Painting where b = $b) having count(*) > $c");

        assertEquals(new ExpressionParameter("a"), ((Expression) q.where.getOperand(0)).getOperand(1));
        assertEquals(ExpressionFactory.exp("b = $b"), subquery((Expression) q.where.getOperand(1), 0).where);
        assertEquals(ExpressionFactory.exp("count(*) > $c"), q.having);
    }

    @Test
    public void namedParameters() {
        ObjectSelect<?> q = ObjectSelect.parse(
                "select artistName from Artist where (artistName = $name or artistName = $name "
                        + "and db:ARTIST_ID in $ids) and exists (from Painting where x = $x and y = $y) "
                        + "having count(*) > $c",
                Map.of("name", "T", "ids", List.of(1, 2), "y", 2));

        // a repeated name binds once, a collection is a list, a missing name prunes its condition, in a subquery too
        assertEquals(ExpressionFactory.exp("artistName = 'T' or artistName = 'T' and db:ARTIST_ID in (1, 2)"),
                q.where.getOperand(0));
        assertEquals(ExpressionFactory.exp("y = 2"), subquery((Expression) q.where.getOperand(1), 0).where);
        assertNull(q.having);

        assertNull(ObjectSelect.parse("from Artist where a = $a", Map.of()).where);

        // no values preserve the parameters
        assertEquals(ExpressionFactory.exp("a = $a"), ObjectSelect.parse("from Artist where a = $a").where);
    }

    @Test
    public void namedParametersTyped() {
        ObjectSelect<Artist> artists = ObjectSelect.parse(Artist.class, "from Artist where artistName = $name",
                Map.of("name", "T"));
        assertEquals(ExpressionFactory.exp("artistName = 'T'"), artists.where);

        ObjectSelect<String> names = ObjectSelect.parse(String.class,
                "select artistName from Artist where artistName = $name", Map.of("name", "T"));
        assertEquals(ExpressionFactory.exp("artistName = 'T'"), names.where);

        assertThrows(ExpressionException.class,
                () -> ObjectSelect.parse(DataRow.class, "select artistName from Artist", Map.of()));
        assertThrows(ExpressionException.class,
                () -> ObjectSelect.parse(Object[].class, "select artistName from Artist", Map.of()));
    }

    @Test
    public void parseErrorCause() {
        ExpressionException e = assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist where"));
        assertInstanceOf(ParseException.class, e.getCause());
    }

    @Test
    public void nonConditionOperand() {
        ExpressionException e = assertThrows(ExpressionException.class,
                () -> ObjectSelect.parse("from Artist where a and a"));
        assertInstanceOf(ParseException.class, e.getCause());
        assertEquals("Encountered \"a\" at line 1, column 19. Was expecting: a condition", e.getUnlabeledMessage());

        // the right operand is reported with no position
        e = assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist where a = 1 or b + 1"));
        assertEquals("Or: invalid operand - b + 1", e.getUnlabeledMessage());

        e = assertThrows(ExpressionException.class, () -> ObjectSelect.parse("from Artist where not a"));
        assertEquals("Not: invalid operand - a", e.getUnlabeledMessage());

        // a lone path is still a valid column, and a valid condition when compared
        ObjectSelect.parse("select a from Artist");
        ObjectSelect.parse("from Artist where not a = 1 and (b = 2 or c = 3)");
    }

    @Test
    public void nestedErrorMessageHasNoLabel() {
        ExpressionException e = assertThrows(ExpressionException.class,
                () -> ObjectSelect.parse("from Artist where a = $x", 1, 2));
        assertFalse(e.getUnlabeledMessage().startsWith("[v."), e.getUnlabeledMessage());
    }

    @Test
    public void collectionParameter() {
        ObjectSelect<?> q = ObjectSelect.parse("from Artist where db:ARTIST_ID in $ids", List.of(1, 2));
        assertEquals(ExpressionFactory.inDbExp("ARTIST_ID", 1, 2), q.where);
    }

    @Test
    public void columns() {
        ObjectSelect<?> q = ObjectSelect.parse("select artistName, dateOfBirth from Artist order by artistName");

        assertNotNull(q.columns);
        assertFalse(q.singleColumn);
        assertEquals("Artist", q.entityName);

        List<Property<?>> columns = columns(q);
        assertEquals(2, columns.size());
        assertEquals(Artist.ARTIST_NAME.getExpression(), columns.get(0).getExpression());
        assertNull(columns.get(0).getType());
        assertEquals(Artist.DATE_OF_BIRTH.getExpression(), columns.get(1).getExpression());
    }

    @Test
    public void singleDistinctColumn() {
        ObjectSelect<?> q = ObjectSelect.parse("select distinct paintings.paintingTitle from Artist");

        assertNotNull(q.columns);
        assertTrue(q.singleColumn);
        assertTrue(q.toQL().startsWith("select distinct "), q.toQL());
        assertEquals(Artist.PAINTINGS.dot(Painting.PAINTING_TITLE).getExpression(),
                columns(q).get(0).getExpression());
    }

    @Test
    public void selfWithAggregateAndHaving() {
        ObjectSelect<?> q = ObjectSelect.parse(
                "select self, count(paintings) as n from Artist having count(paintings) > 2");

        List<Property<?>> columns = columns(q);
        assertEquals(2, columns.size());
        assertEquals(Artist.SELF.getExpression(), columns.get(0).getExpression());
        assertEquals(Artist.PAINTINGS.count().getExpression(), columns.get(1).getExpression());
        assertEquals("n", columns.get(1).getAlias());
        assertEquals(Artist.PAINTINGS.count().gt(2L), ExpressionFactory.exp("count(paintings) > 2L"));
        assertEquals(ExpressionFactory.exp("count(paintings) > 2"), q.having);
    }

    @Test
    public void functionsAndOuterJoins() {
        List<Property<?>> columns = columns(ObjectSelect.parse(
                "select year(dateOfBirth), upper(artistName), count(*), count(distinct(artistName)), gallery+, "
                        + "max(estimatedPrice) from Painting"));

        assertEquals(6, columns.size());
        assertEquals(ExpressionFactory.exp("year(dateOfBirth)"), columns.get(0).getExpression());
        assertEquals(ExpressionFactory.exp("upper(artistName)"), columns.get(1).getExpression());
        assertEquals(ExpressionFactory.exp("count(*)"), columns.get(2).getExpression());
        assertEquals(ExpressionFactory.exp("count(distinct(artistName))"), columns.get(3).getExpression());
        assertEquals(Painting.GALLERY.outer().getExpression(), columns.get(4).getExpression());
        assertEquals(ExpressionFactory.exp("max(estimatedPrice)"), columns.get(5).getExpression());
    }

    @Test
    public void prefetch() {
        ObjectSelect<?> q = ObjectSelect.parse(
                "from Artist where artistName = $name limit 10 prefetch paintings joint, "
                        + "artistExhibits disjointById, paintings.gallery", "X");

        PrefetchTreeNode root = q.prefetches;
        assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, root.getNode("paintings").getSemantics());
        assertEquals(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS,
                root.getNode("artistExhibits").getSemantics());
        assertEquals(PrefetchTreeNode.UNDEFINED_SEMANTICS, root.getNode("paintings.gallery").getSemantics());
    }

    @Test
    public void existsSubquery() {
        ObjectSelect<?> q = ObjectSelect.parse(
                "from Artist where exists (select self from Painting where artist = enclosing(self) "
                        + "and estimatedPrice > 1000)");

        assertInstanceOf(ExistsExp.class, q.where);
        ObjectSelect<?> subquery = subquery(q.where, 0);
        assertInstanceOf(ObjectSelect.class, subquery);
        assertEquals("Painting", subquery.entityName);
        Expression expected = Painting.ARTIST.eq(Artist.SELF.enclosing())
                .andExp(ExpressionFactory.greaterExp("estimatedPrice", 1000));
        assertEquals(expected.toString(), subquery.where.toString());
    }

    @Test
    public void multilineQuery() {
        ObjectSelect<?> q = ObjectSelect.parse("""
                from Artist where exists (
                    from Painting where artist = enclosing(self) and estimatedPrice > $price)
                order by artistName""", 1000);

        assertEquals("Painting", subquery(q.where, 0).entityName);
        assertEquals(1, orderings(q).size());
    }

    @Test
    public void notExistsSubquery() {
        ObjectSelect<?> q = ObjectSelect.parse(
                "from Artist where artistName = 'a' and not exists (from Painting where artist = enclosing(self))");

        Expression notExists = (Expression) q.where.getOperand(1);
        assertInstanceOf(NotExistsExp.class, notExists);
        assertEquals("Painting", subquery(notExists, 0).entityName);
    }

    @Test
    public void existsPathIsPreserved() {
        ObjectSelect<?> q = ObjectSelect.parse("from Artist where exists paintings and not exists (a = 1)");
        assertEquals(ExpressionFactory.exp("exists paintings and not exists (a = 1)"), q.where);
    }

    @Test
    public void inSubquery() {
        ObjectSelect<?> q = ObjectSelect.parse(
                "from Painting where artist.artistName not in "
                        + "(select artistName from Artist where dateOfBirth < $d)", 5);

        assertInstanceOf(NotInExp.class, q.where);
        ObjectSelect<?> subquery = subquery(q.where, 1);
        assertTrue(subquery.singleColumn);
        assertEquals(ExpressionFactory.exp("dateOfBirth < 5"), subquery.where);
    }

    @Test
    public void allAndAnySubqueries() {
        ObjectSelect<?> q = ObjectSelect.parse(
                "from Painting where estimatedPrice > all (select estimatedPrice from Painting where x = 'P2') "
                        + "or estimatedPrice = any (select max(estimatedPrice) from Painting)");

        Expression all = (Expression) ((Expression) q.where.getOperand(0)).getOperand(1);
        assertInstanceOf(AllExp.class, all);
        assertEquals(ExpressionFactory.exp("x = 'P2'"), subquery(all, 0).where);

        Expression any = (Expression) ((Expression) q.where.getOperand(1)).getOperand(1);
        assertInstanceOf(AnyExp.class, any);
        assertEquals("Painting", subquery(any, 0).entityName);
    }

    @Test
    public void subqueryInExpression() {
        Expression exp = ExpressionFactory.exp(
                "artistName in (select artist.artistName from Painting where estimatedPrice > $p limit 5)", 100);

        ObjectSelect<?> subquery = subquery(exp, 1);
        assertEquals(ExpressionFactory.greaterExp("estimatedPrice", 100), subquery.where);
        assertTrue(subquery.toQL().endsWith(" limit 5"), subquery.toQL());
    }

    @Test
    public void subqueryRejectsPrefetch() {
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse(
                "from Artist where exists (from Painting where artist = enclosing(self) prefetch gallery)"));
    }

    @Test
    public void nestedEnclosing() {
        Expression exp = ExpressionFactory.exp("a = enclosing(enclosing(artistName))");
        Expression enclosing = (Expression) exp.getOperand(1);
        assertEquals(Artist.ARTIST_NAME.enclosing().enclosing().getExpression(), enclosing);
    }

    @Test
    public void selfAndEnclosingRoundTrip() {
        Expression exp = Painting.ARTIST.eq(Artist.SELF.enclosing());
        assertEquals("artist = enclosing(self)", exp.toString());
        assertEquals(exp.toString(), ExpressionFactory.exp(exp.toString()).toString());

        // "self" stays usable as a property name with a prefix
        assertEquals(ExpressionFactory.pathExp("self"), ExpressionFactory.exp("obj:self"));
    }

    @Test
    public void dbRoot() {
        ObjectSelect<?> q = ObjectSelect.parse("from db:ARTIST where ARTIST_NAME like 'A%' order by ARTIST_ID desc");

        assertInstanceOf(ObjectSelect.class, q);
        assertEquals("ARTIST", q.dbEntityName);
        assertNull(q.entityName);
        assertTrue(resolve(q).fetchingDataRows());
        assertEquals(ExpressionFactory.likeDbExp("ARTIST_NAME", "A%"), q.where);
        assertEquals(ExpressionFactory.dbPathExp("ARTIST_ID"), orderings(q).get(0).getSortSpec());
    }

    @Test
    public void typedParse() {
        ObjectSelect<Artist> objects = ObjectSelect.parse(Artist.class, "from Artist where artistName = $n", "a");
        assertEquals(Artist.ARTIST_NAME.eq("a"), objects.where);
        assertFalse(resolve(objects).fetchingDataRows());

        ObjectSelect<DataRow> rows = ObjectSelect.parse(DataRow.class, "from Artist");
        assertTrue(resolve(rows).fetchingDataRows());

        ObjectSelect<String> column = ObjectSelect.parse(String.class, "select artistName from Artist");
        assertTrue(column.singleColumn);

        ObjectSelect<Object[]> columns = ObjectSelect.parse(Object[].class, "select artistName, count(*) from Artist");
        assertEquals(2, columns.columns.size());
    }

    @Test
    public void typedParseShapeMismatch() {
        // the parser can't tell a root query from a single-column one without the model, but the other mismatches
        // are detected
        assertThrows(ExpressionException.class,
                () -> ObjectSelect.parse(Artist.class, "select a, b from Artist"));
        assertThrows(ExpressionException.class,
                () -> ObjectSelect.parse(DataRow.class, "select artistName from Artist"));
        assertThrows(ExpressionException.class,
                () -> ObjectSelect.parse(String.class, "select a, b from Artist"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse(Object[].class, "select a from Artist"));
        assertThrows(ExpressionException.class, () -> ObjectSelect.parse(Object[].class, "from Artist"));
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
