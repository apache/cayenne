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
package org.apache.cayenne.access;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.List;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextOuterJoinsIT extends RuntimeCase {

	@Inject
	protected ObjectContext context;

	@Inject
	protected DBHelper dbHelper;

	protected TableHelper artistHelper;
	protected TableHelper paintingHelper;
	protected TableHelper artgroupHelper;
	protected TableHelper artistGroupHelper;

	@Override
	public void cleanUpDB() throws Exception {
		dbHelper.update("ARTGROUP").set("PARENT_GROUP_ID", null, Types.INTEGER).execute();
		super.cleanUpDB();
	}

	@Before
	public void setUp() throws Exception {
		artistHelper = new TableHelper(dbHelper, "ARTIST", "ARTIST_ID", "ARTIST_NAME");
		paintingHelper = new TableHelper(dbHelper, "PAINTING", "PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE")
				.setColumnTypes(Types.INTEGER, Types.BIGINT, Types.VARCHAR);

		artgroupHelper = new TableHelper(dbHelper, "ARTGROUP", "GROUP_ID", "NAME");
		artistGroupHelper = new TableHelper(dbHelper, "ARTIST_GROUP", "GROUP_ID", "ARTIST_ID");
	}

	@Test
	public void testSelectWithOuterJoinFlattened() throws Exception {

		artistHelper.insert(33001, "AA1");
		artistHelper.insert(33002, "AA2");
		artistHelper.insert(33003, "BB1");
		artistHelper.insert(33004, "BB2");

		artgroupHelper.insert(1, "G1");

		artistGroupHelper.insert(1, 33001);
		artistGroupHelper.insert(1, 33002);
		artistGroupHelper.insert(1, 33004);

		List<Artist> artists = ObjectSelect.query(Artist.class)
				.where(Artist.GROUP_ARRAY.outer().isNull())
				.orderBy(Artist.ARTIST_NAME.asc())
				.select(context);
		assertEquals(1, artists.size());
		assertEquals("BB1", artists.get(0).getArtistName());
	}

	@Test
	public void testSelectWithOuterJoin() throws Exception {

		artistHelper.insert(33001, "AA1");
		artistHelper.insert(33002, "AA2");
		artistHelper.insert(33003, "BB1");
		artistHelper.insert(33004, "BB2");

		paintingHelper.insert(33001, 33001, "P1");
		paintingHelper.insert(33002, 33002, "P2");

		List<Artist> artists = ObjectSelect.query(Artist.class)
				.where(Artist.PAINTING_ARRAY.outer().isNull())
				.orderBy(Artist.ARTIST_NAME.asc())
				.select(context);
		assertEquals(2, artists.size());
		assertEquals("BB1", artists.get(0).getArtistName());

		artists = ObjectSelect.query(Artist.class)
				.where(Artist.PAINTING_ARRAY.outer().isNull())
				.or(Artist.ARTIST_NAME.eq("AA1"))
				.orderBy(Artist.ARTIST_NAME.asc())
				.select(context);
		assertEquals(3, artists.size());
		assertEquals("AA1", artists.get(0).getArtistName());
		assertEquals("BB1", artists.get(1).getArtistName());
		assertEquals("BB2", artists.get(2).getArtistName());
	}

	@Test
	public void testSelectWithOuterJoinFromString() throws Exception {

		artistHelper.insert(33001, "AA1");
		artistHelper.insert(33002, "AA2");
		artistHelper.insert(33003, "BB1");
		artistHelper.insert(33004, "BB2");

		paintingHelper.insert(33001, 33001, "P1");
		paintingHelper.insert(33002, 33002, "P2");

		List<Artist> artists = ObjectSelect.query(Artist.class)
				.where(ExpressionFactory.exp("paintingArray+ = null"))
				.orderBy(Artist.ARTIST_NAME.asc())
				.select(context);
		assertEquals(2, artists.size());
		assertEquals("BB1", artists.get(0).getArtistName());

		artists = ObjectSelect.query(Artist.class)
				.where(ExpressionFactory.exp("paintingArray+ = null"))
				.or(Artist.ARTIST_NAME.eq("AA1"))
				.orderBy(Artist.ARTIST_NAME.asc())
				.select(context);
		assertEquals(3, artists.size());
		assertEquals("AA1", artists.get(0).getArtistName());
		assertEquals("BB1", artists.get(1).getArtistName());
		assertEquals("BB2", artists.get(2).getArtistName());
	}

	@Test
	public void testSelectWithOuterOrdering() throws Exception {

		artistHelper.insert(33001, "AA1");
		artistHelper.insert(33002, "AA2");

		paintingHelper.insert(33001, 33001, "P1");
		paintingHelper.insert(33002, 33002, "P2");
		paintingHelper.insert(33003, null, "P3");

		List<Painting> paintings = ObjectSelect.query(Painting.class)
				.orderBy(Painting.TO_ARTIST.outer().dot(Artist.ARTIST_NAME).desc())
				.select(context);
		assertEquals(3, paintings.size());
	}
}
