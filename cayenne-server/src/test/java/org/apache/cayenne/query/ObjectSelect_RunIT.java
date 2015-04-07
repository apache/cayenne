/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.query;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ObjectSelect_RunIT extends ServerCase {

	@Inject
	private DataContext context;

	@Inject
	private DBHelper dbHelper;

	protected void createArtistsDataSet() throws Exception {
		TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

		long dateBase = System.currentTimeMillis();

		for (int i = 1; i <= 20; i++) {
			tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 10000 * i));
		}
	}

	@Test
	public void test_SelectObjects() throws Exception {

		createArtistsDataSet();

		List<Artist> result = ObjectSelect.query(Artist.class).select(context);
		assertEquals(20, result.size());
		assertThat(result.get(0), instanceOf(Artist.class));

		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist14")).selectOne(context);
		assertNotNull(a);
		assertEquals("artist14", a.getArtistName());
	}

    @Test
    public void test_Iterate() throws Exception {
        createArtistsDataSet();

        final int[] count = new int[1];
        ObjectSelect.query(Artist.class).iterate(context, new ResultIteratorCallback<Artist>() {

            @Override
            public void next(Artist object) {
                assertNotNull(object.getArtistName());
                count[0]++;
            }
        });

        assertEquals(20, count[0]);
    }

    @Test
    public void test_Iterator() throws Exception {
        createArtistsDataSet();

        ResultIterator<Artist> it = ObjectSelect.query(Artist.class).iterator(context);

        try {
            int count = 0;

            for (Artist a : it) {
                count++;
            }

            assertEquals(20, count);
        } finally {
            it.close();
        }
    }

    @Test
    public void test_BatchIterator() throws Exception {
        createArtistsDataSet();

        ResultBatchIterator<Artist> it = ObjectSelect.query(Artist.class).batchIterator(context, 5);

        try {
            int count = 0;

            for (List<Artist> artistList : it) {
                count++;
                assertEquals(5, artistList.size());
            }

            assertEquals(4, count);
        } finally {
            it.close();
        }
    }

	@Test
	public void test_SelectDataRows() throws Exception {

		createArtistsDataSet();

		List<DataRow> result = ObjectSelect.dataRowQuery(Artist.class).select(context);
		assertEquals(20, result.size());
		assertThat(result.get(0), instanceOf(DataRow.class));

		DataRow a = ObjectSelect.dataRowQuery(Artist.class).where(Artist.ARTIST_NAME.eq("artist14")).selectOne(context);
		assertNotNull(a);
		assertEquals("artist14", a.get("ARTIST_NAME"));
	}

	@Test
	public void test_SelectOne() throws Exception {
		createArtistsDataSet();

		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13")).selectOne(context);
		assertNotNull(a);
		assertEquals("artist13", a.getArtistName());
	}

	@Test
	public void test_SelectOne_NoMatch() throws Exception {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13")).selectOne(context);
		assertNull(a);
	}

	@Test(expected = CayenneRuntimeException.class)
	public void test_SelectOne_MoreThanOneMatch() throws Exception {
		createArtistsDataSet();
		ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.like("artist%")).selectOne(context);
	}
	
	@Test
	public void test_SelectFirst() throws Exception {
		createArtistsDataSet();

		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13")).selectFirst(context);
		assertNotNull(a);
		assertEquals("artist13", a.getArtistName());
	}

    @Test
    public void test_SelectFirstByContext() throws Exception {
        createArtistsDataSet();

        ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13"));
        Artist a = context.selectFirst(q);
        assertNotNull(a);
        assertEquals("artist13", a.getArtistName());
    }

	@Test
	public void test_SelectFirst_NoMatch() throws Exception {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13")).selectFirst(context);
		assertNull(a);
	}

	@Test
	public void test_SelectFirst_MoreThanOneMatch() throws Exception {
		createArtistsDataSet();
		
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.like("artist%")).orderBy("db:ARTIST_ID").selectFirst(context);
		assertNotNull(a);
		assertEquals("artist1", a.getArtistName());
	}
}
