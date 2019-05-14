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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ExpressionIT extends ServerCase {

	@Inject
	private ObjectContext context;

	@Inject
	private ServerRuntime runtime;

	@Inject
    private UnitDbAdapter adapter;

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

	@Test
	public void testLessThanNull() {
		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("Picasso");
		context.commitChanges();

        List<Artist> artists;
		try {
            artists = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.lt((String) null)).select(context);
        } catch (CayenneRuntimeException ex) {
		    if(adapter.supportsNullComparision()) {
		        throw ex;
            } else {
		        return;
            }
        }

        assertTrue("Less than 'NULL' never matches anything", artists.isEmpty());
	}

	@Test
	public void testInNull() {
		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("Picasso");
		context.commitChanges();

        List<Artist> artists;
        try {
            artists = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.in("Picasso", (String) null)).select(context);
        } catch (CayenneRuntimeException ex) {
            if(adapter.supportsNullComparision()) {
                throw ex;
            } else {
                return;
            }
        }
		assertEquals(1, artists.size());
	}

}
