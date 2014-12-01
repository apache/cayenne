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

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.sql.Types;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SelectById_RunIT extends ServerCase {

	@Inject
	private DataChannelInterceptor interceptor;

	@Inject
	private DBHelper dbHelper;

	private TableHelper tArtist;
	private TableHelper tPainting;

	@Inject
	private ObjectContext context;

	@Inject
	private EntityResolver resolver;

	@Before
	public void setUp() throws Exception {
		tArtist = new TableHelper(dbHelper, "ARTIST").setColumns("ARTIST_ID", "ARTIST_NAME");
		tPainting = new TableHelper(dbHelper, "PAINTING").setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE")
				.setColumnTypes(Types.INTEGER, Types.BIGINT, Types.VARCHAR);
	}

	private void createTwoArtists() throws Exception {
		tArtist.insert(2, "artist2");
		tArtist.insert(3, "artist3");
	}

	@Test
	public void testIntPk() throws Exception {
		createTwoArtists();

		Artist a3 = SelectById.query(Artist.class, 3).selectOne(context);
		assertNotNull(a3);
		assertEquals("artist3", a3.getArtistName());

		Artist a2 = SelectById.query(Artist.class, 2).selectOne(context);
		assertNotNull(a2);
		assertEquals("artist2", a2.getArtistName());
	}

	@Test
	public void testMapPk() throws Exception {
		createTwoArtists();

		Artist a3 = SelectById.query(Artist.class, singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3)).selectOne(context);
		assertNotNull(a3);
		assertEquals("artist3", a3.getArtistName());

		Artist a2 = SelectById.query(Artist.class, singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2)).selectOne(context);
		assertNotNull(a2);
		assertEquals("artist2", a2.getArtistName());
	}

	@Test
	public void testObjectIdPk() throws Exception {
		createTwoArtists();

		ObjectId oid3 = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);
		Artist a3 = SelectById.query(Artist.class, oid3).selectOne(context);
		assertNotNull(a3);
		assertEquals("artist3", a3.getArtistName());

		ObjectId oid2 = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
		Artist a2 = SelectById.query(Artist.class, oid2).selectOne(context);
		assertNotNull(a2);
		assertEquals("artist2", a2.getArtistName());
	}

	@Test
	public void testDataRowIntPk() throws Exception {
		createTwoArtists();

		DataRow a3 = SelectById.dataRowQuery(Artist.class, 3).selectOne(context);
		assertNotNull(a3);
		assertEquals("artist3", a3.get("ARTIST_NAME"));

		DataRow a2 = SelectById.dataRowQuery(Artist.class, 2).selectOne(context);
		assertNotNull(a2);
		assertEquals("artist2", a2.get("ARTIST_NAME"));
	}

	@Test
	public void testMetadataCacheKey() throws Exception {
		SelectById<Painting> q1 = SelectById.query(Painting.class, 4).useLocalCache();
		QueryMetadata md1 = q1.getMetaData(resolver);
		assertNotNull(md1);
		assertNotNull(md1.getCacheKey());

		SelectById<Painting> q2 = SelectById.query(Painting.class, singletonMap(Painting.PAINTING_ID_PK_COLUMN, 4))
				.useLocalCache();
		QueryMetadata md2 = q2.getMetaData(resolver);
		assertNotNull(md2);
		assertNotNull(md2.getCacheKey());

		// this query is just a different form of q1, so should hit the same
		// cache entry
		assertEquals(md1.getCacheKey(), md2.getCacheKey());

		SelectById<Painting> q3 = SelectById.query(Painting.class, 5).useLocalCache();
		QueryMetadata md3 = q3.getMetaData(resolver);
		assertNotNull(md3);
		assertNotNull(md3.getCacheKey());
		assertNotEquals(md1.getCacheKey(), md3.getCacheKey());

		SelectById<Artist> q4 = SelectById.query(Artist.class, 4).useLocalCache();
		QueryMetadata md4 = q4.getMetaData(resolver);
		assertNotNull(md4);
		assertNotNull(md4.getCacheKey());
		assertNotEquals(md1.getCacheKey(), md4.getCacheKey());

		SelectById<Painting> q5 = SelectById.query(Painting.class,
				new ObjectId("Painting", Painting.PAINTING_ID_PK_COLUMN, 4)).useLocalCache();
		QueryMetadata md5 = q5.getMetaData(resolver);
		assertNotNull(md5);
		assertNotNull(md5.getCacheKey());

		// this query is just a different form of q1, so should hit the same
		// cache entry
		assertEquals(md1.getCacheKey(), md5.getCacheKey());
	}

	@Test
	public void testLocalCache() throws Exception {
		createTwoArtists();

		final Artist[] a3 = new Artist[1];

		assertEquals(1, interceptor.runWithQueryCounter(new UnitTestClosure() {

			@Override
			public void execute() {
				a3[0] = SelectById.query(Artist.class, 3).useLocalCache("g1").selectOne(context);
				assertNotNull(a3[0]);
				assertEquals("artist3", a3[0].getArtistName());
			}
		}));

		interceptor.runWithQueriesBlocked(new UnitTestClosure() {

			@Override
			public void execute() {
				Artist a3cached = SelectById.query(Artist.class, 3).useLocalCache("g1").selectOne(context);
				assertSame(a3[0], a3cached);
			}
		});

		context.performGenericQuery(new RefreshQuery("g1"));

		assertEquals(1, interceptor.runWithQueryCounter(new UnitTestClosure() {

			@Override
			public void execute() {
				SelectById.query(Artist.class, 3).useLocalCache("g1").selectOne(context);
			}
		}));
	}

	@Test
	public void testPrefetch() throws Exception {
		createTwoArtists();
		tPainting.insert(45, 3, "One");
		tPainting.insert(48, 3, "Two");

		final Artist a3 = SelectById.query(Artist.class, 3).prefetch(Artist.PAINTING_ARRAY.joint()).selectOne(context);

		interceptor.runWithQueriesBlocked(new UnitTestClosure() {

			@Override
			public void execute() {
				assertNotNull(a3);
				assertEquals("artist3", a3.getArtistName());
				assertEquals(2, a3.getPaintingArray().size());
				
				a3.getPaintingArray().get(0).getPaintingTitle();
				a3.getPaintingArray().get(1).getPaintingTitle();
			}
		});
	}
}
