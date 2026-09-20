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

import org.apache.cayenne.docs.persistent.Artist;
import org.apache.cayenne.docs.persistent.Painting;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExpressionsTest extends BaseTest {

    private Artist dali() {
        return ObjectSelect.query(Artist.class).where(Artist.NAME.eq("Dali")).selectOne(context);
    }

    @Test
    public void exp() {
        // tag::exp[]
        String expString = "name like 'A%' and price < 1000";
        Expression exp = ExpressionFactory.exp(expString);
        // end::exp[]

        assertEquals("(name like \"A%\") and (price < 1000)", exp.toString());
    }

    /**
     * Checks the expression Strings from "expressions.txt", that is included in the docs.
     */
    @Test
    public void expStrings() {
        List<String> valid = List.of(
                "caseValid", "grouping", "objPath", "objPrefixPath", "multiSegmentPath", "dbPath", "like");
        for (String tag : valid) {
            assertNotNull(ExpressionFactory.exp(example("expressions.txt", tag)), tag);
        }

        assertEquals(ExpressionFactory.exp(example("expressions.txt", "objPath")),
                ExpressionFactory.exp(example("expressions.txt", "objPrefixPath")));

        assertThrows(ExpressionException.class, () -> ExpressionFactory.exp(example("expressions.txt", "caseInvalid")));
    }

    @Test
    public void quotes() {
        // tag::quotes[]
        Expression e1 = ExpressionFactory.exp("name = 'ABC'");

        // double quotes are escaped inside Java Strings of course
        Expression e2 = ExpressionFactory.exp("name = \"ABC\"");
        // end::quotes[]

        assertEquals(e1, e2);
    }

    @Test
    public void aliases() {
        createArtistsDataSet();

        // each alias results in a separate join, so this matches the artists that have both paintings
        Expression e = ExpressionFactory.exp(
                "paintings#p1.title = 'P1' and paintings#p2.title = 'P2'");
        assertEquals(1, ObjectSelect.query(Artist.class).where(e).select(context).size());
    }

    @Test
    public void matchAllExp() {
        createArtistsDataSet();

        // tag::matchAllExp[]
        // matches the artists that have both "P1" and "P2" paintings
        Expression e = ExpressionFactory.matchAllExp("|paintings.title", "P1", "P2");
        // end::matchAllExp[]

        assertEquals(1, ObjectSelect.query(Artist.class).where(e).select(context).size());
    }

    @Test
    public void params() {
        // tag::params[]
        Expression template = ExpressionFactory.exp("name = $name");

        // name binding
        Map<String, Object> p1 = Map.of("name", "Salvador Dali");
        Expression qualifier1 = template.params(p1);

        // positional binding
        Expression qualifier2 = template.paramsArray("Monet");
        // end::params[]

        assertEquals("name = \"Salvador Dali\"", qualifier1.toString());
        assertEquals("name = \"Monet\"", qualifier2.toString());
    }

    @Test
    public void expWithParams() {
        // tag::expWithParams[]
        Expression qualifier = ExpressionFactory.exp("name = $name", "Monet");
        // end::expWithParams[]

        assertEquals("name = \"Monet\"", qualifier.toString());
    }

    @Test
    public void likeParam() {
        // tag::likeParam[]
        Expression qualifier = ExpressionFactory.exp("name like $name", "Salvador%");
        // end::likeParam[]

        assertEquals("name like \"Salvador%\"", qualifier.toString());
    }

    @Test
    public void objectParam() {
        createArtistsDataSet();

        // tag::objectParam[]
        Artist dali = dali(); // assume we fetched this one already
        Expression qualifier = ExpressionFactory.exp("artist = $artist", dali);
        // end::objectParam[]

        assertEquals(2, ObjectSelect.query(Painting.class).where(qualifier).select(context).size());
    }

    @Test
    public void pruning() {
        // tag::pruning[]
        Expression template = ExpressionFactory.exp("name like $name and dateOfBirth > $date");

        Map<String, Object> p1 = Map.of("name", "Salvador%");
        Expression qualifier1 = template.params(p1);

        // "qualifier1" is now "name like 'Salvador%'".
        // 'dateOfBirth > $date' condition was pruned, as no value was specified for
        // the $date parameter
        // end::pruning[]

        assertEquals("(name like \"Salvador%\")", qualifier1.toString());
    }

    @Test
    public void expressionFactory() {
        // tag::expressionFactory[]
        // String expression: name like 'A%' and price < 1000
        Expression e1 = ExpressionFactory.likeExp("name", "A%");
        Expression e2 = ExpressionFactory.lessExp("price", 1000);
        Expression finalExp = e1.andExp(e2);
        // end::expressionFactory[]

        assertEquals(ExpressionFactory.exp("name like 'A%' and price < 1000"), finalExp);
    }

    @Test
    public void properties() {
        // tag::properties[]
        // Artist.NAME is generated by Cayenne and has a type of StringProperty<String>
        Expression e1 = Artist.NAME.eq("Pablo");

        // Chaining multiple properties into a path.
        // Painting.ARTIST is generated by Cayenne and has a type of EntityProperty<Artist>
        Expression e2 = Painting.ARTIST.dot(Artist.NAME).eq("Pablo");
        // end::properties[]

        assertEquals(ExpressionFactory.exp("name = 'Pablo'"), e1);
        assertEquals(ExpressionFactory.exp("artist.name = 'Pablo'"), e2);
    }

    @Test
    public void match() {
        createArtistsDataSet();
        boolean[] matched = new boolean[1];

        // tag::match[]
        Expression e = Artist.NAME.in("Dali", "Monet");
        Artist artist = dali();
        if (e.match(artist)) {
            // do something with the matching object...
            // end::match[]
            matched[0] = true;
            // tag::match[]
        }
        // end::match[]

        assertTrue(matched[0]);
    }

    @Test
    public void evaluate() {
        createArtistsDataSet();
        Artist artist = dali();

        // tag::evaluate[]
        String name = (String) Artist.NAME.getExpression().evaluate(artist);
        // end::evaluate[]

        assertEquals("Dali", name);
    }

    @Test
    public void filterObjects() {
        createArtistsDataSet();

        // tag::filterObjects[]
        Expression e = Artist.NAME.in("Dali", "Monet");
        List<Artist> unfiltered = ObjectSelect.query(Artist.class).select(context);
        List<Artist> filtered = e.filterObjects(unfiltered);
        // end::filterObjects[]

        assertEquals(3, unfiltered.size());
        assertEquals(1, filtered.size());
    }
}
