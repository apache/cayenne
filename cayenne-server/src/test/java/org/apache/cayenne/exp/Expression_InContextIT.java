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

package org.apache.cayenne.exp;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class Expression_InContextIT extends ServerCase {

	@Inject
	private DBHelper dbHelper;

	@Inject
	private ObjectContext context;

	@Inject
	private ServerRuntime runtime;

	@Override
	protected void setUpAfterInjection() throws Exception {
		dbHelper.deleteAll("PAINTING_INFO");
		dbHelper.deleteAll("PAINTING");
		dbHelper.deleteAll("ARTIST_EXHIBIT");
		dbHelper.deleteAll("ARTIST_GROUP");
		dbHelper.deleteAll("ARTIST");
		dbHelper.deleteAll("EXHIBIT");
		dbHelper.deleteAll("GALLERY");
	}

    @Test
	public void testMatch() {

		assertTrue(context instanceof DataContext);

		DataContext context2 = (DataContext) runtime.newContext();

		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("Equals");
		Painting p1 = context.newObject(Painting.class);
		p1.setToArtist(a1);
		p1.setPaintingTitle("painting1");

		context.commitChanges();

		SelectQuery<Painting> query = new SelectQuery<Painting>(Painting.class);
		Expression e = Painting.TO_ARTIST.eq(a1);
		query.setQualifier(e);

		assertNotSame(context2, context);

		List<Painting> objects = context2.select(query);
		assertEquals(1, objects.size());

		// 2 same objects in different contexts
		assertTrue(e.match(objects.get(0)));

		// we change one object - so the objects are different now
		// (PersistenceState different)
		a1.setArtistName("newName");

		SelectQuery<Painting> q2 = new SelectQuery<Painting>(Painting.class);
		Expression ex2 = Painting.TO_ARTIST.eq(a1);
		q2.setQualifier(ex2);

		assertTrue(ex2.match(objects.get(0)));

		Artist a2 = context.newObject(Artist.class);
		a2.setArtistName("Equals");

		context.commitChanges();

		SelectQuery<Painting> q = new SelectQuery<Painting>(Painting.class);
		Expression ex = Painting.TO_ARTIST.eq(a2);
		q.setQualifier(ex);

		// 2 different objects in different contexts
		assertFalse(ex.match(objects.get(0)));
	}

    @Test
	public void testFirst() {
		List<Painting> paintingList = new ArrayList<Painting>();
		Painting p1 = context.newObject(Painting.class);
		p1.setPaintingTitle("x1");
		paintingList.add(p1);

		Painting p2 = context.newObject(Painting.class);
		p2.setPaintingTitle("x2");
		paintingList.add(p2);

		Painting p3 = context.newObject(Painting.class);
		p3.setPaintingTitle("x3");
		paintingList.add(p3);

		Expression e1 = ExpressionFactory.likeExp("paintingTitle", "x%");
		assertSame(p1, e1.first(paintingList));

		Expression e3 = ExpressionFactory.matchExp("paintingTitle", "x3");
		assertSame(p3, e3.first(paintingList));

		Expression e4 = ExpressionFactory.matchExp("paintingTitle", "x4");
		assertNull(e4.first(paintingList));
	}

}
