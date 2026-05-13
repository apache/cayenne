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

package org.apache.cayenne.exp;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.parser.ASTIn;
import org.apache.cayenne.exp.parser.ASTList;
import org.apache.cayenne.exp.parser.ASTNotIn;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExpressionIT {

	@RegisterExtension
	static final CayenneTestsExt env = CayenneTestsExt.forProject(CayenneProjects.TESTMAP_PROJECT);

	private ObjectContext context;
	private CayenneRuntime runtime;
	private UnitDbAdapter adapter;

	@BeforeEach
	public void setUp() {
		context = env.context();
		runtime = env.runtime();
		adapter = env.getInstance(UnitDbAdapter.class);
	}

    @Test
	public void match() {

		assertTrue(context instanceof DataContext);

		DataContext context2 = (DataContext) runtime.newContext();

		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("Equals");
		Painting p1 = context.newObject(Painting.class);
		p1.setToArtist(a1);
		p1.setPaintingTitle("painting1");

		context.commitChanges();

		assertNotSame(context2, context);

		List<Painting> objects = ObjectSelect.query(Painting.class, Painting.TO_ARTIST.eq(a1)).select(context2);
		assertEquals(1, objects.size());

		// 2 same objects in different contexts
		assertTrue(Painting.TO_ARTIST.eq(a1).match(objects.get(0)));

		// we change one object - so the objects are different now
		// (PersistenceState different)
		a1.setArtistName("newName");

		assertTrue(Painting.TO_ARTIST.eq(a1).match(objects.get(0)));

		Artist a2 = context.newObject(Artist.class);
		a2.setArtistName("Equals");

		context.commitChanges();

		// 2 different objects in different contexts
		assertFalse(Painting.TO_ARTIST.eq(a2).match(objects.get(0)));
	}

    @Test
	public void first() {
		List<Painting> paintingList = new ArrayList<>();
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
	public void lessThanNull() {
		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("Picasso");
		context.commitChanges();

        List<Artist> artists;
		try {
            artists = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.lt((String) null)).select(context);
        } catch (CayenneRuntimeException ex) {
		    if(adapter.supportsNullComparison()) {
		        throw ex;
            } else {
		        return;
            }
        }

        assertTrue(artists.isEmpty(), "Less than 'NULL' never matches anything");
	}

	@Test
	public void inNull() {
		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("Picasso");
		context.commitChanges();

        List<Artist> artists;
        try {
            artists = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.in("Picasso", (String) null)).select(context);
        } catch (CayenneRuntimeException ex) {
            if(adapter.supportsNullComparison()) {
                throw ex;
            } else {
                return;
            }
        }
		assertEquals(1, artists.size());
	}

	@Test
	public void inEmpty() {
		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("Picasso");
		context.commitChanges();

		List<Artist> artists = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.in(List.of())).select(context);
		assertTrue(artists.isEmpty());
	}

	@Test
	public void explicitInEmpty() {
		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("Picasso");
		context.commitChanges();

		ASTIn in = new ASTIn((SimpleNode) Artist.ARTIST_NAME.getExpression(), new ASTList(List.of()));
		List<Artist> artists = ObjectSelect.query(Artist.class, in).select(context);
		assertTrue(artists.isEmpty());
	}

	@Test
	public void explicitNotInEmpty() {
		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("Picasso");
		context.commitChanges();

		ASTNotIn notIn = new ASTNotIn((SimpleNode) Artist.ARTIST_NAME.getExpression(), new ASTList(List.of()));
		List<Artist> artists = ObjectSelect.query(Artist.class, notIn).select(context);
		assertEquals(1, artists.size());
		assertEquals("Picasso", artists.get(0).getArtistName());
	}

	/**
	 * We are waiting invalid SQL here:
	 * 	data type of expression is not boolean in statement
	 * 	[SELECT RTRIM(t0.ARTIST_NAME), t0.DATE_OF_BIRTH, t0.ARTIST_ID FROM ARTIST t0 WHERE 'abc']
	 */
	@Test
	public void selectWithScalarAsWhereCondition() {
		if (adapter.supportScalarAsExpression()){
			return;
		}
		ObjectSelect<Artist> objectSelect = ObjectSelect.query(Artist.class).where(ExpressionFactory.wrapScalarValue("abc"));
		CayenneRuntimeException exception = assertThrows(CayenneRuntimeException.class, () -> objectSelect.select(context));
		assertTrue(exception.getMessage().contains("Query exception."));
	}

}
