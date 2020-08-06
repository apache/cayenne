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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Test;

public class ASTLikeTest {

	@Test
	public void testToEJBQL_likeEscape() throws IOException {
		Expression like = new ASTLike(new ASTObjPath("mainName"), "|%|%?|_title|%", '|');
		assertEquals("x.mainName like '|%|%?|_title|%' escape '|'", like.toEJBQL("x"));
	}

	@Test
	public void testEvaluate_MultiCharMatch() {
		Expression like = new ASTLike(new ASTObjPath("artistName"), "abc%d");
		Expression notLike = new ASTNotLike(new ASTObjPath("artistName"), "abc%d");

		Artist noMatch = new Artist();
		noMatch.setArtistName("dabc");
		assertFalse(like.match(noMatch));
		assertTrue(notLike.match(noMatch));

		Artist match1 = new Artist();
		match1.setArtistName("abc123d");
		assertTrue("Failed: " + like, like.match(match1));
		assertFalse("Failed: " + notLike, notLike.match(match1));

		Artist match2 = new Artist();
		match2.setArtistName("abcd");
		assertTrue("Failed: " + like, like.match(match2));
		assertFalse("Failed: " + notLike, notLike.match(match2));
	}

	@Test
	public void testEvaluate_SingleCharMatch() {
		Expression like = new ASTLike(new ASTObjPath("artistName"), "abc?d");
		Expression notLike = new ASTNotLike(new ASTObjPath("artistName"), "abc?d");

		Artist noMatch1 = new Artist();
		noMatch1.setArtistName("dabc");
		assertFalse(like.match(noMatch1));
		assertTrue(notLike.match(noMatch1));

		Artist noMatch2 = new Artist();
		noMatch2.setArtistName("abc123d");
		assertFalse("Failed: " + like, like.match(noMatch2));
		assertTrue("Failed: " + notLike, notLike.match(noMatch2));

		Artist match = new Artist();
		match.setArtistName("abcXd");
		assertTrue("Failed: " + like, like.match(match));
		assertFalse("Failed: " + notLike, notLike.match(match));
	}

	@Test
	public void testEvaluate_NotSoSpecialChars() {
		// test special chars that are not LIKE pattern match chars
		Expression like = new ASTLike(new ASTObjPath("artistName"), "/./");

		Artist noMatch1 = new Artist();
		noMatch1.setArtistName("/a/");
		assertFalse(like.match(noMatch1));

		Artist match = new Artist();
		match.setArtistName("/./");
		assertTrue("Failed: " + like, like.match(match));
	}

	@Test
	public void testEvaluateUnicode() {
		Expression like = new ASTLike(new ASTObjPath("artistName"), "àбğþ%");
		Expression notLike = new ASTNotLike(new ASTObjPath("artistName"), "àбğþ%");

		Artist noMatch1 = new Artist();
		noMatch1.setArtistName("àbğþd");
		assertFalse(like.match(noMatch1));
		assertTrue(notLike.match(noMatch1));

		Artist match1 = new Artist();
		match1.setArtistName("àбğþ");
		assertTrue("Failed: " + like, like.match(match1));
		assertFalse("Failed: " + notLike, notLike.match(match1));

		Artist match2 = new Artist();
		match2.setArtistName("àбğþa");
		assertTrue("Failed: " + like, like.match(match2));
		assertFalse("Failed: " + notLike, notLike.match(match2));
	}

}
