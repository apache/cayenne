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
import org.apache.cayenne.query.ObjectSelect;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectSelectTest extends BaseTest {

    @Test
    public void selectAll() {
        createArtistsDataSet();

        // tag::selectAll[]
        List<Artist> objects = ObjectSelect.query(Artist.class).select(context);
        // end::selectAll[]

        assertEquals(3, objects.size());
    }

    @Test
    public void where() {
        createArtistsDataSet();

        // tag::where[]
        List<Artist> objects = ObjectSelect.query(Artist.class)
                .where(Artist.NAME.like("Pablo%"))
                .select(context);
        // end::where[]

        assertEquals(1, objects.size());
    }

    @Test
    public void and() {
        createArtistsDataSet();
        LocalDate someDate = LocalDate.of(1800, 1, 1);

        // tag::and[]
        List<Artist> objects = ObjectSelect.query(Artist.class)
                .where(Artist.NAME.like("A%"))
                .and(Artist.DATE_OF_BIRTH.gt(someDate))
                .select(context);
        // end::and[]

        assertEquals(1, objects.size());
    }

    @Test
    public void orderBy() {
        createArtistsDataSet();

        // tag::orderBy[]
        List<Artist> objects = ObjectSelect.query(Artist.class)
                .orderBy(Artist.DATE_OF_BIRTH.desc())
                .orderBy(Artist.NAME.asc())
                .select(context);
        // end::orderBy[]

        assertEquals("Dali", objects.get(0).getName());
    }

    @Test
    public void column() {
        createArtistsDataSet();

        // tag::column[]
        List<String> names = ObjectSelect.columnQuery(Artist.class, Artist.NAME)
                .select(context);
        // end::column[]

        assertEquals(3, names.size());
    }

    @Test
    public void columns() {
        createArtistsDataSet();

        // tag::columns[]
        List<Object[]> nameAndDate = ObjectSelect
                .columnQuery(Artist.class, Artist.NAME, Artist.DATE_OF_BIRTH)
                .select(context);
        // end::columns[]

        assertEquals(3, nameAndDate.size());
        assertEquals(2, nameAndDate.get(0).length);
    }

    @Test
    public void count() {
        createArtistsDataSet();

        // tag::count[]
        long count = ObjectSelect.query(Artist.class).selectCount(context);
        // end::count[]

        assertEquals(3, count);
    }

    @Test
    public void aggregates() {
        createArtistsDataSet();

        // tag::aggregates[]
        // Artist.SELF - is a special property that denotes a full object in this case
        List<Object[]> artistAndPaintingCount = ObjectSelect.columnQuery(Artist.class,
                        Artist.SELF,
                        Artist.PAINTINGS.count())
                .where(Artist.NAME.like("P%"))
                .having(Artist.PAINTINGS.count().lt(5L))
                .orderBy(Artist.PAINTINGS.count().desc(), Artist.NAME.asc())
                .select(context);

        for (Object[] next : artistAndPaintingCount) {
            Artist artist = (Artist) next[0];
            long paintings = (Long) next[1];
            System.out.println(artist.getName() + " has " + paintings + " paintings");
        }
        // end::aggregates[]

        assertEquals(1, artistAndPaintingCount.size());
        assertEquals(2L, artistAndPaintingCount.get(0)[1]);
    }

    @Test
    public void notExists() {
        createArtistsDataSet();

        // tag::notExists[]
        long count = ObjectSelect.query(Artist.class)
                .where(Artist.PAINTINGS.notExists())
                .selectCount(context);
        // end::notExists[]

        assertEquals(1, count);
    }
}
