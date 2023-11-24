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

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class EJBQLQueryCountIT extends RuntimeCase {

	@Inject
	private ObjectContext context;

	@Inject
	protected DBHelper dbHelper;

	protected TableHelper tArtist;
	protected TableHelper tPainting;
	protected TableHelper tGallery;

	@Before
	public void before() throws Exception {
		tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

		tPainting = new TableHelper(dbHelper, "PAINTING");
		tPainting.setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE");
	}

	@Test
	public void test_SimpleCount() throws Exception {
		tArtist.insert(1, "A1");
		tArtist.insert(2, "A2");
		tArtist.insert(3, "A3");

		EJBQLQuery query = new EJBQLQuery("SELECT COUNT(a) FROM Artist a");

		// this should be simply a count of painting/artist joins
		assertEquals(Collections.singletonList(3L), context.performQuery(query));
	}

	@Test
	public void test_ToOne() throws Exception {
		tArtist.insert(1, "A1");
		tArtist.insert(2, "A2");
		tArtist.insert(3, "A3");

		tPainting.insert(1, 1, "P1");
		tPainting.insert(2, 1, "P2");
		tPainting.insert(4, 2, "P1");

		EJBQLQuery query = new EJBQLQuery("SELECT COUNT(p.toArtist) FROM Painting p");

		// this should be simply a count of painting/artist joins
		assertEquals(Collections.singletonList(3L), context.performQuery(query));
	}

	@Test
	public void test_DistinctToOne() throws Exception {
		tArtist.insert(1, "A1");
		tArtist.insert(2, "A2");
		tArtist.insert(3, "A3");

		tPainting.insert(1, 1, "P1");
		tPainting.insert(2, 1, "P2");
		tPainting.insert(4, 2, "P1");

		EJBQLQuery query = new EJBQLQuery("SELECT COUNT(DISTINCT p.toArtist) FROM Painting p");
		// this should be a count of artists that have paintings
		assertEquals(Collections.singletonList(2L), context.performQuery(query));
	}
	
	@Test
	public void test_DistinctToOneAttribute() throws Exception {
		tArtist.insert(1, "A1");
		tArtist.insert(2, "A1");
		tArtist.insert(3, "A1");

		tPainting.insert(1, 1, "P1");
		tPainting.insert(2, 1, "P2");
		tPainting.insert(4, 2, "P1");

		EJBQLQuery query = new EJBQLQuery("SELECT COUNT(DISTINCT p.toArtist.artistName) FROM Painting p");
		// this should be a count of artists that have paintings
		assertEquals(Collections.singletonList(1L), context.performQuery(query));
	}
	
}
