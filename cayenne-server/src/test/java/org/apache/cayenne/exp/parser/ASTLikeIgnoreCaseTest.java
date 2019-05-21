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

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.junit.Test;

public class ASTLikeIgnoreCaseTest {

	@Test
	public void testToEJBQL() {
		Expression like = new ASTLikeIgnoreCase(new ASTObjPath("a"), "%b%");
		assertEquals(like.toEJBQL("p"), "upper(p.a) like '%B%'");
	}

	@Test
	public void testEvaluate() {
		Expression like = new ASTLikeIgnoreCase(new ASTObjPath("artistName"), "aBcD");
		Expression notLike = new ASTNotLikeIgnoreCase(new ASTObjPath("artistName"), "aBcD");

		Artist noMatch1 = new Artist();
		noMatch1.setArtistName("dabc");
		assertFalse(like.match(noMatch1));
		assertTrue(notLike.match(noMatch1));

		Artist match1 = new Artist();
		match1.setArtistName("abcd");
		assertTrue("Failed: " + like, like.match(match1));
		assertFalse("Failed: " + notLike, notLike.match(match1));

		Artist match2 = new Artist();
		match2.setArtistName("ABcD");
		assertTrue("Failed: " + like, like.match(match2));
		assertFalse("Failed: " + notLike, notLike.match(match2));
	}

	@Test
	public void testEvaluateWithCollection() {
		Expression like = new ASTLikeIgnoreCase(new ASTObjPath("paintingArray.paintingTitle"), "aBcD");
		Expression notLike = new ASTNotLikeIgnoreCase(new ASTObjPath("paintingArray.paintingTitle"), "aBcD");

		Artist noMatch1 = new Artist();
		noMatch1.writePropertyDirectly("paintingArray",
				Arrays.asList(createPainting("xyz"), createPainting("abc")));

		assertFalse("Failed: " + like, like.match(noMatch1));
		assertTrue("Failed: " + like, notLike.match(noMatch1));

		Artist match1 = new Artist();
		match1.writePropertyDirectly("paintingArray",
				Arrays.asList(createPainting("AbCd"), createPainting("abcd")));

		assertTrue("Failed: " + like, like.match(match1));
		assertFalse("Failed: " + like, notLike.match(match1));

		Artist match2 = new Artist();
		match2.writePropertyDirectly("paintingArray",
				Arrays.asList(createPainting("Xzy"), createPainting("abcd")));

		assertTrue("Failed: " + like, like.match(match2));
		assertTrue("Failed: " + like, notLike.match(match2));
	}

	@Test
	public void testEvaluateUnicode() {
		Expression like = new ASTLikeIgnoreCase(new ASTObjPath("artistName"), "ÀБĞÞ%");
		Expression notLike = new ASTNotLikeIgnoreCase(new ASTObjPath("artistName"), "ÀБĞÞ%");

		Artist noMatch1 = new Artist();
		noMatch1.setArtistName("àbğþ");
		assertFalse(like.match(noMatch1));
		assertTrue(notLike.match(noMatch1));

		Artist match1 = new Artist();
		match1.setArtistName("àбğþd");
		assertTrue("Failed: " + like, like.match(match1));
		assertFalse("Failed: " + notLike, notLike.match(match1));

		Artist match2 = new Artist();
		match2.setArtistName("àБğÞ");
		assertTrue("Failed: " + like, like.match(match2));
		assertFalse("Failed: " + notLike, notLike.match(match2));
	}

	private Painting createPainting(String name) {
		Painting p = new Painting();
		p.setPaintingTitle(name);
		return p;
	}
}
