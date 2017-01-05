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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ExpressionFactoryIT extends ServerCase {

	@Inject
	private ObjectContext context;

	@Inject
	private UnitDbAdapter accessStackAdapter;

	// CAY-416
	@Test
	public void testCollectionMatch() {
		Artist artist = context.newObject(Artist.class);
		artist.setArtistName("artist");
		Painting p1 = context.newObject(Painting.class), p2 = context.newObject(Painting.class), p3 = context
				.newObject(Painting.class);
		p1.setPaintingTitle("p1");
		p2.setPaintingTitle("p2");
		p3.setPaintingTitle("p3");
		artist.addToPaintingArray(p1);
		artist.addToPaintingArray(p2);

		context.commitChanges();

		assertTrue(ExpressionFactory.matchExp("paintingArray", p1).match(artist));
		assertFalse(ExpressionFactory.matchExp("paintingArray", p3).match(artist));
		assertFalse(ExpressionFactory.noMatchExp("paintingArray", p1).match(artist));
		assertTrue(ExpressionFactory.noMatchExp("paintingArray", p3).match(artist));

		assertTrue(ExpressionFactory.matchExp("paintingArray.paintingTitle", "p1").match(artist));
		assertFalse(ExpressionFactory.matchExp("paintingArray.paintingTitle", "p3").match(artist));
		assertFalse(ExpressionFactory.noMatchExp("paintingArray.paintingTitle", "p1").match(artist));
		assertTrue(ExpressionFactory.noMatchExp("paintingArray.paintingTitle", "p3").match(artist));

		assertTrue(ExpressionFactory.inExp("paintingTitle", "p1").match(p1));
		assertFalse(ExpressionFactory.notInExp("paintingTitle", "p3").match(p3));
	}

	@Test
	public void testIn() {
		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("a1");
		Painting p1 = context.newObject(Painting.class);
		p1.setPaintingTitle("p1");
		Painting p2 = context.newObject(Painting.class);
		p2.setPaintingTitle("p2");
		a1.addToPaintingArray(p1);
		a1.addToPaintingArray(p2);

		Expression in = ExpressionFactory.inExp("paintingArray", p1);
		assertTrue(in.match(a1));
	}

	@Test
	public void testEscapeCharacter() {
		if(!accessStackAdapter.supportsEscapeInLike()) {
			return;
		}

		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("A_1");
		Artist a2 = context.newObject(Artist.class);
		a2.setArtistName("A_2");
		context.commitChanges();

		Expression ex1 = ExpressionFactory.likeIgnoreCaseDbExp("ARTIST_NAME", "A*_1", '*');
		SelectQuery<Artist> q1 = new SelectQuery<Artist>(Artist.class, ex1);
		List<Artist> artists = context.select(q1);
		assertEquals(1, artists.size());

		Expression ex2 = ExpressionFactory.likeExp("artistName", "A*_2", '*');
		SelectQuery<Artist> q2 = new SelectQuery<Artist>(Artist.class, ex2);
		artists = context.select(q2);
		assertEquals(1, artists.size());
	}
	
	@Test
	public void testContains_Escape() {

		if(!accessStackAdapter.supportsEscapeInLike()) {
			return;
		}

		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("MA_1X");
		Artist a2 = context.newObject(Artist.class);
		a2.setArtistName("CA%2Y");
		context.commitChanges();

		Expression ex1 = ExpressionFactory.containsExp(Artist.ARTIST_NAME.getName(), "A_1");
		SelectQuery<Artist> q1 = new SelectQuery<Artist>(Artist.class, ex1);
		List<Artist> artists = context.select(q1);
		assertEquals(1, artists.size());

		Expression ex2 = ExpressionFactory.containsExp(Artist.ARTIST_NAME.getName(), "A%2");
		SelectQuery<Artist> q2 = new SelectQuery<Artist>(Artist.class, ex2);
		artists = context.select(q2);
		assertEquals(1, artists.size());
	}
}
