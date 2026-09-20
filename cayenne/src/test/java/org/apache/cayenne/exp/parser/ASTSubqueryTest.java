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

package org.apache.cayenne.exp.parser;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ASTSubqueryTest {

    private static final ColumnSelect<String> NAMES = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME);

    @Test
    public void existsToString() {
        Expression exists = ExpressionFactory.exists(ObjectSelect.query(Painting.class));
        assertEquals("exists (from Painting)", exists.toString());

        Expression notExists = ExpressionFactory.notExists(ObjectSelect.query(Painting.class)
                .where(Painting.TO_ARTIST.eq(Artist.SELF.enclosing()))
                .and(ExpressionFactory.greaterExp("estimatedPrice", 1000)));
        assertEquals("not exists (from Painting where (toArtist = enclosing(self)) and (estimatedPrice > 1000))",
                notExists.toString());

        assertEquals("(a = 1) and (exists (from Painting))", ExpressionFactory.exp("a = 1").andExp(exists).toString());
    }

    @Test
    public void inToString() {
        assertEquals("a in (select artistName from Artist)",
                ExpressionFactory.inExp(ExpressionFactory.pathExp("a"), NAMES).toString());
        assertEquals("a not in (select artistName from Artist)",
                ExpressionFactory.notInExp(ExpressionFactory.pathExp("a"), NAMES).toString());
    }

    @Test
    public void allAndAnyToString() {
        assertEquals("a > all (select artistName from Artist)",
                ExpressionFactory.greaterExp("a", ExpressionFactory.all(NAMES)).toString());
        assertEquals("a = any (select artistName from Artist)",
                ExpressionFactory.matchExp("a", ExpressionFactory.any(NAMES)).toString());
    }

    @Test
    public void allClausesToString() {
        ColumnSelect<Object[]> subquery = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME.alias("n"), Artist.PAINTING_ARRAY.count())
                .distinct()
                .where(Artist.ARTIST_NAME.like("a%"))
                .having(Artist.PAINTING_ARRAY.count().gt(2L))
                .orderBy(Artist.ARTIST_NAME.descInsensitive(), Artist.DATE_OF_BIRTH.asc())
                .limit(5)
                .offset(10);

        assertEquals("exists (select distinct artistName as n, count(paintingArray) from Artist "
                        + "where artistName like \"a%\" having count(paintingArray) > 2L "
                        + "order by artistName desc insensitive, dateOfBirth limit 5 offset 10)",
                ExpressionFactory.exists(subquery).toString());
    }

    @Test
    public void rootsToString() {
        assertEquals("exists (from MyEntity)",
                ExpressionFactory.exists(ObjectSelect.query(Object.class, "MyEntity")).toString());
        assertEquals("exists (from db:MY_TABLE where db:A = 1)",
                ExpressionFactory.exists(ObjectSelect.dbQuery("MY_TABLE").where(ExpressionFactory.matchDbExp("A", 1)))
                        .toString());
        assertEquals("exists (select distinct self from Painting)",
                ExpressionFactory.exists(ObjectSelect.query(Painting.class).distinct()).toString());
    }

    @Test
    public void roundTrip() {
        for (String expString : new String[]{
                "exists (from Painting)",
                "not exists (from Painting where (toArtist = enclosing(self)) and (estimatedPrice > 1000))",
                "(a = 1) and (exists (from Painting where toArtist = enclosing(self)))",
                "a in (select artistName from Artist where dateOfBirth < 5)",
                "a not in (select distinct toArtist.artistName from Painting limit 5)",
                "a > all (select estimatedPrice from Painting where paintingTitle = \"P2\")",
                "(a = 1) or (a = any (select max(estimatedPrice) from Painting))",
                "exists (select artistName as n, count(paintingArray) from Artist having count(paintingArray) > 2 "
                        + "order by artistName desc insensitive, dateOfBirth limit 5 offset 10)",
                "exists (from db:ARTIST where db:ARTIST_NAME like \"a%\")",
                "exists (from Painting where exists (from Gallery where paintingArray = enclosing(self)))"}) {

            Expression exp = ExpressionFactory.exp(expString);
            assertEquals(expString, exp.toString());
            assertEquals(expString, ExpressionFactory.exp(exp.toString()).toString());
        }
    }
}
