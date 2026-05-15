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
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.parser.ASTIn;
import org.apache.cayenne.exp.parser.ASTList;
import org.apache.cayenne.exp.parser.ASTNotIn;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
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
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
	public void match() {

		assertTrue(env.context() instanceof DataContext);

		DataContext context2 = (DataContext) env.runtime().newContext();

		Artist a1 = env.context().newObject(Artist.class);
		a1.setArtistName("Equals");
		Painting p1 = env.context().newObject(Painting.class);
		p1.setToArtist(a1);
		p1.setPaintingTitle("painting1");

		env.context().commitChanges();

		assertNotSame(context2, env.context());

		List<Painting> objects = ObjectSelect.query(Painting.class, Painting.TO_ARTIST.eq(a1)).select(context2);
		assertEquals(1, objects.size());

		// 2 same objects in different contexts
		assertTrue(Painting.TO_ARTIST.eq(a1).match(objects.get(0)));

		// we change one object - so the objects are different now
		// (PersistenceState different)
		a1.setArtistName("newName");

		assertTrue(Painting.TO_ARTIST.eq(a1).match(objects.get(0)));

		Artist a2 = env.context().newObject(Artist.class);
		a2.setArtistName("Equals");

		env.context().commitChanges();

		// 2 different objects in different contexts
		assertFalse(Painting.TO_ARTIST.eq(a2).match(objects.get(0)));
	}

    @Test
	public void first() {
		List<Painting> paintingList = new ArrayList<>();
		Painting p1 = env.context().newObject(Painting.class);
		p1.setPaintingTitle("x1");
		paintingList.add(p1);

		Painting p2 = env.context().newObject(Painting.class);
		p2.setPaintingTitle("x2");
		paintingList.add(p2);

		Painting p3 = env.context().newObject(Painting.class);
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
		Artist a1 = env.context().newObject(Artist.class);
		a1.setArtistName("Picasso");
		env.context().commitChanges();

        List<Artist> artists;
		try {
            artists = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.lt((String) null)).select(env.context());
        } catch (CayenneRuntimeException ex) {
		    if(env.testDbAdapter().supportsNullComparison()) {
		        throw ex;
            } else {
		        return;
            }
        }

        assertTrue(artists.isEmpty(), "Less than 'NULL' never matches anything");
	}

	@Test
	public void inNull() {
		Artist a1 = env.context().newObject(Artist.class);
		a1.setArtistName("Picasso");
		env.context().commitChanges();

        List<Artist> artists;
        try {
            artists = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.in("Picasso", (String) null)).select(env.context());
        } catch (CayenneRuntimeException ex) {
            if(env.testDbAdapter().supportsNullComparison()) {
                throw ex;
            } else {
                return;
            }
        }
		assertEquals(1, artists.size());
	}

	@Test
	public void inEmpty() {
		Artist a1 = env.context().newObject(Artist.class);
		a1.setArtistName("Picasso");
		env.context().commitChanges();

		List<Artist> artists = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.in(List.of())).select(env.context());
		assertTrue(artists.isEmpty());
	}

	@Test
	public void explicitInEmpty() {
		Artist a1 = env.context().newObject(Artist.class);
		a1.setArtistName("Picasso");
		env.context().commitChanges();

		ASTIn in = new ASTIn((SimpleNode) Artist.ARTIST_NAME.getExpression(), new ASTList(List.of()));
		List<Artist> artists = ObjectSelect.query(Artist.class, in).select(env.context());
		assertTrue(artists.isEmpty());
	}

	@Test
	public void explicitNotInEmpty() {
		Artist a1 = env.context().newObject(Artist.class);
		a1.setArtistName("Picasso");
		env.context().commitChanges();

		ASTNotIn notIn = new ASTNotIn((SimpleNode) Artist.ARTIST_NAME.getExpression(), new ASTList(List.of()));
		List<Artist> artists = ObjectSelect.query(Artist.class, notIn).select(env.context());
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
		if (env.testDbAdapter().supportScalarAsExpression()){
			return;
		}
		ObjectSelect<Artist> objectSelect = ObjectSelect.query(Artist.class).where(ExpressionFactory.wrapScalarValue("abc"));
		CayenneRuntimeException exception = assertThrows(CayenneRuntimeException.class, () -> objectSelect.select(env.context()));
		assertTrue(exception.getMessage().contains("Query exception."));
	}

}
