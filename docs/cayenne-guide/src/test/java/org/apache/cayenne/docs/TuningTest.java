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
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.docs.persistent.Artist;
import org.apache.cayenne.docs.persistent.Painting;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLSelect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TuningTest extends BaseTest {

    @Test
    public void prefetch() {
        createArtistsDataSet();

        // tag::prefetch[]
        List<Artist> artists = ObjectSelect
                .query(Artist.class)
                .prefetch(Artist.PAINTINGS.disjoint()) // <1>
                .select(context); // <2>
        // end::prefetch[]

        assertEquals(3, artists.size());
    }

    @Test
    public void prefetchPath() {
        createArtistsDataSet();
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class);

        // tag::prefetchPath[]
        query.prefetch(Artist.PAINTINGS.dot(Painting.GALLERY).disjoint());
        // end::prefetchPath[]

        assertEquals(3, query.select(context).size());
    }

    @Test
    public void prefetchMultiple() {
        createArtistsDataSet();
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class);

        // tag::prefetchMultiple[]
        query.prefetch(Artist.PAINTINGS.disjoint())
                .prefetch(Artist.PAINTINGS.dot(Painting.GALLERY).disjoint());
        // end::prefetchMultiple[]

        assertEquals(3, query.select(context).size());
    }

    @Test
    public void sqlSelectPrefetch() {
        createArtistsDataSet();

        // tag::sqlSelectPrefetch[]
        List<Artist> objects = SQLSelect.query(Artist.class, "SELECT "
                        + "#result('ESTIMATED_PRICE' 'BigDecimal' '' 'paintings.ESTIMATED_PRICE'), "
                        + "#result('TITLE' 'String' '' 'paintings.TITLE'), "
                        + "#result('GALLERY_ID' 'int' '' 'paintings.GALLERY_ID'), "
                        + "#result('t1.ID' 'int' '' 'paintings.ID'), "
                        + "#result('t1.ARTIST_ID' 'int' '' 'paintings.ARTIST_ID'), "
                        + "#result('NAME' 'String'), "
                        + "#result('DATE_OF_BIRTH' 'java.time.LocalDate'), "
                        + "#result('t0.ID' 'int' '' 'ID') "
                        + "FROM ARTIST t0, PAINTING t1 "
                        + "WHERE t0.ID = t1.ARTIST_ID")
                .addPrefetch(Artist.PAINTINGS.joint())
                .select(context);
        // end::sqlSelectPrefetch[]

        assertEquals(2, objects.size());
        assertEquals(2, objects.get(0).getPaintings().size());
    }

    @Test
    public void dataRows() {
        createArtistsDataSet();

        // tag::dataRows[]
        ObjectSelect<DataRow> query = ObjectSelect.dataRowQuery(Artist.class);

        List<DataRow> rows = query.select(context);
        // end::dataRows[]

        assertEquals(3, rows.size());

        int[] converted = new int[1];

        // tag::objectFromDataRow[]
        // you need to cast ObjectContext to DataContext to get access to 'objectFromDataRow'
        DataContext dataContext = (DataContext) context;

        for (DataRow row : rows) {
            if (row.get("DATE_OF_BIRTH") != null) {
                Artist artist = dataContext.objectFromDataRow(Artist.class, row);
                // do something with Artist...
                // end::objectFromDataRow[]
                converted[0]++;
                // tag::objectFromDataRow[]
            }
        }
        // end::objectFromDataRow[]

        assertEquals(3, converted[0]);
    }

    @Test
    public void sqlDataRows() {
        createArtistsDataSet();

        // tag::sqlDataRows[]
        SQLSelect<DataRow> query = SQLSelect.dataRowQuery("SELECT * FROM ARTIST");
        List<DataRow> rows = query.select(context);
        // end::sqlDataRows[]

        assertEquals(3, rows.size());
    }

    @Test
    public void iterator() {
        createArtistsDataSet();
        int[] iterated = new int[1];

        // tag::iterator[]
        try (ResultIterator<Artist> it = ObjectSelect.query(Artist.class).iterator(context)) {
            for (Artist a : it) {
                // do something with the object...
                // end::iterator[]
                iterated[0]++;
                // tag::iterator[]
            }
        }
        // end::iterator[]

        assertEquals(3, iterated[0]);
    }

    @Test
    public void iterate() {
        createArtistsDataSet();
        int[] iterated = new int[1];

        // tag::iterate[]
        ObjectSelect.query(Artist.class).iterate(context, (Artist a) -> {
            // do something with the object...
            // end::iterate[]
            iterated[0]++;
            // tag::iterate[]
        });
        // end::iterate[]

        assertEquals(3, iterated[0]);
    }

    @Test
    public void batchIterator() {
        createArtistsDataSet();
        int[] iterated = new int[1];

        // tag::batchIterator[]
        try (ResultBatchIterator<Artist> it = ObjectSelect.query(Artist.class).batchIterator(context, 100)) {
            for (List<Artist> list : it) {
                // do something with each list
                // end::batchIterator[]
                iterated[0] += list.size();
                // tag::batchIterator[]
                // possibly commit your changes
                context.commitChanges();
            }
        }
        // end::batchIterator[]

        assertEquals(3, iterated[0]);
    }

    @Test
    public void pagination() {
        createArtistsDataSet();

        // tag::pagination[]
        // the fact that result is paginated is transparent
        List<Artist> artists =
                ObjectSelect.query(Artist.class).pageSize(50).select(context);
        // end::pagination[]

        assertEquals(3, artists.size());
    }

    @Test
    public void dataRowsPagination() {
        createArtistsDataSet();

        // tag::dataRowsPagination[]
        List<DataRow> rows =
                ObjectSelect.dataRowQuery(Artist.class).pageSize(50).select(context);
        // end::dataRowsPagination[]

        assertEquals(3, rows.size());
    }

    @Test
    public void localCache() {
        createArtistsDataSet();

        // tag::localCache[]
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class).localCache("artists");
        // end::localCache[]

        assertEquals(3, query.select(context).size());

        // tag::removeGroup[]
        QueryCache cache = runtime.getDataDomain().getQueryCache();
        cache.removeGroup("artists");
        // end::removeGroup[]
    }
}
