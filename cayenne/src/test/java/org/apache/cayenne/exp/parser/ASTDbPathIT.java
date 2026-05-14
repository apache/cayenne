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
package org.apache.cayenne.exp.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ASTDbPathIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

	@Test
	public void evaluate_PersistentObject() {

		Artist a1 = env.context().newObject(Artist.class);
		a1.setArtistName("a1");
		env.context().commitChanges();

		Expression idExp = ExpressionFactory.exp("db:ARTIST_ID");
		assertEquals(Cayenne.longPKForObject(a1), idExp.evaluate(a1));

		Expression columnExp = ExpressionFactory.exp("db:ARTIST_NAME");
		assertEquals("a1", columnExp.evaluate(a1));
	}

	@Test
	public void evaluate_DbEntity() {
		Expression e = ExpressionFactory.exp("db:paintingArray.PAINTING_TITLE");

		ObjEntity ae = env.context().getEntityResolver().getObjEntity(Artist.class);
		DbEntity ade = ae.getDbEntity();

		Object objTarget = e.evaluate(ae);
		assertTrue(objTarget instanceof DbAttribute);

		Object dbTarget = e.evaluate(ade);
		assertTrue(dbTarget instanceof DbAttribute);
	}

	@Test
	public void evaluate_Related_PersistentObject() {

		Artist a1 = env.context().newObject(Artist.class);
		Artist a2 = env.context().newObject(Artist.class);
		Painting p1 = env.context().newObject(Painting.class);
		Painting p2 = env.context().newObject(Painting.class);
		Painting p3 = env.context().newObject(Painting.class);

		a1.setArtistName("a1");
		a2.setArtistName("a2");
		p1.setPaintingTitle("p1");
		p2.setPaintingTitle("p2");
		p3.setPaintingTitle("p3");

		p1.setToArtist(a1);
		p2.setToArtist(a2);

		env.context().commitChanges();

		Expression attributeOnlyPath = new ASTDbPath("PAINTING_TITLE");
		Expression singleStepPath = new ASTDbPath("toArtist.ARTIST_NAME");
		Expression multiStepPath = new ASTDbPath("toArtist.paintingArray.PAINTING_TITLE");

		// attribute only path
		assertEquals(p1.getPaintingTitle(), attributeOnlyPath.evaluate(p1));
		assertEquals(p2.getPaintingTitle(), attributeOnlyPath.evaluate(p2));

		// attribute only path - not in cache
		p1.getObjectContext().invalidateObjects(p1, p2);
		assertEquals(p1.getPaintingTitle(), attributeOnlyPath.evaluate(p1));
		assertEquals(p2.getPaintingTitle(), attributeOnlyPath.evaluate(p2));

		// single step relationship path
		assertEquals(a1.getArtistName(), singleStepPath.evaluate(p1));
		assertEquals(a2.getArtistName(), singleStepPath.evaluate(p2));
		assertNull(singleStepPath.evaluate(p3));

		assertEquals(p1.getPaintingTitle(), multiStepPath.evaluate(p1));
		assertEquals(p2.getPaintingTitle(), multiStepPath.evaluate(p2));
	}
}
