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
package org.apache.cayenne.docs;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.docs.persistent.Artist;
import org.apache.cayenne.docs.persistent.Gallery;
import org.apache.cayenne.docs.persistent.Painting;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SortOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * The query Strings for these examples are in "objectselect-parsed.txt", that is also included in the docs. Each String
 * is run together with the fluent API query that the docs show as its equivalent, and the results are compared.
 */
public class ObjectSelectParsedTest extends BaseTest {

    private static String ql(String tag) {
        return example("objectselect-parsed.txt", tag);
    }

    private void assertSameResult(FluentSelect<?, ?> parsed, FluentSelect<?, ?> fluent) {
        assertSameResult(parsed, fluent, false);
    }

    private void assertSameResult(FluentSelect<?, ?> parsed, FluentSelect<?, ?> fluent, boolean emptyExpected) {
        List<?> parsedResult = parsed.select(context);
        List<?> fluentResult = fluent.select(context);

        assertEquals(emptyExpected, fluentResult.isEmpty());
        assertEquals(fluentResult.size(), parsedResult.size());

        for (int i = 0; i < fluentResult.size(); i++) {
            if (fluentResult.get(i) instanceof Object[] row) {
                assertArrayEquals(row, (Object[]) parsedResult.get(i));
            } else {
                assertEquals(fluentResult.get(i), parsedResult.get(i));
            }
        }
    }

    @Test
    public void parse() {
        createArtistsDataSet();

        // tag::parse[]
        List<Artist> artists = ObjectSelect
                .parse(Artist.class, "from Artist where name like $name order by dateOfBirth desc limit 10", "A%")
                .select(context);
        // end::parse[]

        assertEquals(1, artists.size());
    }

    @Test
    public void parseAndCustomize() {
        createArtistsDataSet();

        // tag::parseAndCustomize[]
        List<Artist> artists = ObjectSelect
                .parse(Artist.class, "from Artist where name like $name", "A%")
                .localCache("artists")
                .pageSize(50)
                .select(context);
        // end::parseAndCustomize[]

        assertEquals(1, artists.size());
    }

    @Test
    public void params() {
        createArtistsDataSet();
        Artist artist = ObjectSelect.query(Artist.class).where(Artist.NAME.eq("Dali")).selectOne(context);

        // tag::params[]
        List<Painting> paintings = ObjectSelect
                .parse(Painting.class,
                        "from Painting where artist = $artist and estimatedPrice between $low and $high",
                        artist, 10, 500)
                .select(context);
        // end::params[]

        assertEquals(1, paintings.size());
    }

    @Test
    public void api() {
        createArtistsDataSet();

        // tag::api[]
        // any query. Returns either an ObjectSelect or a ColumnSelect
        FluentSelect<?, ?> q1 = ObjectSelect.parse("from Artist");

        // a query of the root objects. "DataRow.class" is used to fetch the data rows instead of objects
        ObjectSelect<Artist> q2 = ObjectSelect.parse(Artist.class, "from Artist");
        ObjectSelect<DataRow> q3 = ObjectSelect.parse(DataRow.class, "from Artist");

        // a query with a single column
        ColumnSelect<String> q4 = ObjectSelect.parseColumn(String.class, "select name from Artist");

        // a query with multiple columns
        ColumnSelect<Object[]> q5 = ObjectSelect.parseColumns("select name, dateOfBirth from Artist");
        // end::api[]

        assertInstanceOf(ObjectSelect.class, q1);
        assertEquals(3, q1.select(context).size());
        assertEquals(3, q2.select(context).size());
        assertInstanceOf(DataRow.class, q3.select(context).get(0));
        assertEquals(3, q4.select(context).size());
        assertEquals(2, q5.select(context).get(0).length);
    }

    @Test
    public void filterOrderLimit() {
        createArtistsDataSet();

        // tag::filterOrderLimit[]
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .where(Artist.NAME.like("A%"))
                .orderBy(Artist.DATE_OF_BIRTH.desc(), Artist.NAME.ascInsensitive())
                .limit(10)
                .offset(20);
        // end::filterOrderLimit[]

        assertSameResult(ObjectSelect.parse(ql("filterOrderLimit")), query, true);
        assertSameResult(ObjectSelect.parse(ql("filterOrderLimit")).offset(0), query.offset(0));
    }

    @Test
    public void outerJoin() {
        createArtistsDataSet();

        // tag::outerJoin[]
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class).where(Artist.PAINTINGS.outer().isNull());
        // end::outerJoin[]

        assertSameResult(ObjectSelect.parse(ql("outerJoin")), query);
    }

    @Test
    public void splitJoins() {
        createArtistsDataSet();

        // tag::splitJoins[]
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .where(Artist.PAINTINGS.alias("p1").dot(Painting.TITLE).eq("P1"))
                .and(Artist.PAINTINGS.alias("p2").dot(Painting.TITLE).eq("P2"));
        // end::splitJoins[]

        assertSameResult(ObjectSelect.parse(ql("splitJoins")), query);
    }

    @Test
    public void columns() {
        createArtistsDataSet();

        // tag::columns[]
        ColumnSelect<Object[]> query = ObjectSelect
                .columnQuery(Artist.class, Artist.NAME, Artist.DATE_OF_BIRTH)
                .orderBy(Artist.NAME.asc());
        // end::columns[]

        assertSameResult(ObjectSelect.parse(ql("columns")), query);
    }

    @Test
    public void column() {
        createArtistsDataSet();

        // tag::column[]
        ColumnSelect<String> query = ObjectSelect
                .columnQuery(Painting.class, Painting.GALLERY.dot(Gallery.NAME))
                .distinct();
        // end::column[]

        assertEquals(2, query.select(context).size());
        assertEquals(2, ObjectSelect.parse(ql("column")).select(context).size());
    }

    @Test
    public void columnExpressions() {
        createArtistsDataSet();

        List<Object[]> result = ObjectSelect.parseColumns(ql("columnExpressions")).select(context);
        assertEquals(3, result.size());
        assertInstanceOf(String.class, result.get(0)[0]);
        assertInstanceOf(Number.class, result.get(0)[1]);
    }

    @Test
    public void aggregates() {
        createArtistsDataSet();
        BigDecimal min = BigDecimal.valueOf(1000);

        // tag::aggregates[]
        ColumnSelect<Object[]> query = ObjectSelect
                .columnQuery(Painting.class,
                        Painting.ARTIST.dot(Artist.NAME),
                        Painting.ESTIMATED_PRICE.sum(),
                        PropertyFactory.COUNT)
                .having(Painting.ESTIMATED_PRICE.sum().gt(min))
                .orderBy(Painting.ARTIST.dot(Artist.NAME).asc());
        // end::aggregates[]

        assertSameResult(ObjectSelect.parse(ql("aggregates"), min), query);
    }

    @Test
    public void self() {
        createArtistsDataSet();

        // no two artists should have the same number of paintings for the order to be predictable
        Artist dali = ObjectSelect.query(Artist.class).where(Artist.NAME.eq("Dali")).selectOne(context);
        createPainting("P3", 50, dali, null);
        context.commitChanges();

        // tag::self[]
        ColumnSelect<Object[]> query = ObjectSelect
                .columnQuery(Artist.class, Artist.SELF, Artist.PAINTINGS.outer().count())
                .orderBy(Artist.PAINTINGS.outer().count().desc());
        // end::self[]

        assertSameResult(ObjectSelect.parse(ql("self")), query);
    }

    @Test
    public void toOne() {
        createArtistsDataSet();

        // tag::toOne[]
        ColumnSelect<Object[]> query = ObjectSelect
                .columnQuery(Painting.class, Painting.TITLE, Painting.ARTIST, Painting.GALLERY);
        // end::toOne[]

        assertEquals(4, query.select(context).size());
        assertEquals(4, ObjectSelect.parse(ql("toOne")).select(context).size());
    }

    @Test
    public void toMany() {
        createArtistsDataSet();

        // tag::toMany[]
        ColumnSelect<Object[]> query = ObjectSelect
                .columnQuery(Artist.class, Artist.NAME, Artist.PAINTINGS.flat());
        // end::toMany[]

        assertEquals(5, query.select(context).size());
        assertEquals(5, ObjectSelect.parse(ql("toMany")).select(context).size());
    }

    @Test
    public void exists() {
        createArtistsDataSet();
        BigDecimal price = BigDecimal.valueOf(100000);

        // tag::exists[]
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.exists(ObjectSelect.query(Painting.class)
                        .where(Painting.ARTIST.eq(Artist.SELF.enclosing()))
                        .and(Painting.ESTIMATED_PRICE.gt(price))));
        // end::exists[]

        assertSameResult(ObjectSelect.parse(ql("exists"), price), query);
    }

    @Test
    public void in() {
        createArtistsDataSet();
        LocalDate date = LocalDate.of(1900, 1, 1);

        // tag::in[]
        ObjectSelect<Painting> query = ObjectSelect.query(Painting.class)
                .where(Painting.ARTIST.dot(Artist.NAME).in(
                        ObjectSelect.columnQuery(Artist.class, Artist.NAME)
                                .where(Artist.DATE_OF_BIRTH.lt(date))));
        // end::in[]

        assertSameResult(ObjectSelect.parse(ql("in"), date), query);
    }

    @Test
    public void allAndAny() {
        createArtistsDataSet();

        assertFalse(ObjectSelect.parse(ql("all")).select(context).isEmpty());
        assertFalse(ObjectSelect.parse(ql("any")).select(context).isEmpty());
    }

    @Test
    public void dbRoot() {
        createArtistsDataSet();

        // tag::dbRoot[]
        ObjectSelect<DataRow> query = ObjectSelect.dbQuery("ARTIST")
                .where(ExpressionFactory.likeDbExp("NAME", "A%"))
                .orderBy(new Ordering(ExpressionFactory.dbPathExp("ID"), SortOrder.DESCENDING));
        // end::dbRoot[]

        assertSameResult(ObjectSelect.parse(ql("dbRoot")), query);
    }

    @Test
    public void prefetch() {
        createArtistsDataSet();
        String name = "Dali";

        // tag::prefetch[]
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .where(Artist.NAME.eq(name))
                .prefetch(Artist.PAINTINGS.joint())
                .prefetch("paintings.gallery", PrefetchTreeNode.UNDEFINED_SEMANTICS);
        // end::prefetch[]

        assertSameResult(ObjectSelect.parse(ql("prefetch"), name), query);
    }
}
