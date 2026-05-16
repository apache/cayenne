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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ASTEqualIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

	@Test
	public void evaluate_PersistentObject() {
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

		env.context().commitChanges();

		p1.setToArtist(a1);
		p2.setToArtist(a2);

		Expression e = new ASTEqual(new ASTObjPath("toArtist"), a1);

		assertTrue(e.match(p1));
		assertFalse(e.match(p2));
		assertFalse(e.match(p3));
	}

	@Test
	public void evaluate_TempId() {
		Artist a1 = env.context().newObject(Artist.class);
		Artist a2 = env.context().newObject(Artist.class);
		Painting p1 = env.context().newObject(Painting.class);
		Painting p2 = env.context().newObject(Painting.class);
		Painting p3 = env.context().newObject(Painting.class);

		p1.setToArtist(a1);
		p2.setToArtist(a2);

		Expression e = new ASTEqual(new ASTObjPath("toArtist"), a1.getObjectId());

		assertTrue(e.match(p1));
		assertFalse(e.match(p2));
		assertFalse(e.match(p3));
	}

	@Test
	public void evaluate_Id() throws Exception {

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

		Expression e = new ASTEqual(new ASTObjPath("toArtist"), Cayenne.longPKForObject(a1));

		assertTrue(e.match(p1));
		assertFalse(e.match(p2));
		assertFalse(e.match(p3));
	}

}
