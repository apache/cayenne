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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.Test;

public class ASTLikeTest {

	@Test
	public void toEJBQL_likeEscape() throws IOException {
		Expression like = new ASTLike(new ASTObjPath("mainName"), "|%|%?|_title|%", '|');
		assertEquals("x.mainName like '|%|%?|_title|%' escape '|'", like.toEJBQL("x"));
	}

	@Test
	public void evaluate_MultiCharMatch() {
		Expression like = new ASTLike(new ASTObjPath("artistName"), "abc%d");
		Expression notLike = new ASTNotLike(new ASTObjPath("artistName"), "abc%d");

		Artist noMatch = new Artist();
		noMatch.setArtistName("dabc");
		assertFalse(like.match(noMatch));
		assertTrue(notLike.match(noMatch));

		Artist match1 = new Artist();
		match1.setArtistName("abc123d");
		assertTrue(like.match(match1), "Failed: " + like);
		assertFalse(notLike.match(match1), "Failed: " + notLike);

		Artist match2 = new Artist();
		match2.setArtistName("abcd");
		assertTrue(like.match(match2), "Failed: " + like);
		assertFalse(notLike.match(match2), "Failed: " + notLike);
	}

	@Test
	public void evaluate_SingleCharMatch() {
		Expression like = new ASTLike(new ASTObjPath("artistName"), "abc?d");
		Expression notLike = new ASTNotLike(new ASTObjPath("artistName"), "abc?d");

		Artist noMatch1 = new Artist();
		noMatch1.setArtistName("dabc");
		assertFalse(like.match(noMatch1));
		assertTrue(notLike.match(noMatch1));

		Artist noMatch2 = new Artist();
		noMatch2.setArtistName("abc123d");
		assertFalse(like.match(noMatch2), "Failed: " + like);
		assertTrue(notLike.match(noMatch2), "Failed: " + notLike);

		Artist match = new Artist();
		match.setArtistName("abcXd");
		assertTrue(like.match(match), "Failed: " + like);
		assertFalse(notLike.match(match), "Failed: " + notLike);
	}

	@Test
	public void evaluate_NotSoSpecialChars() {
		// test special chars that are not LIKE pattern match chars
		Expression like = new ASTLike(new ASTObjPath("artistName"), "/./");

		Artist noMatch1 = new Artist();
		noMatch1.setArtistName("/a/");
		assertFalse(like.match(noMatch1));

		Artist match = new Artist();
		match.setArtistName("/./");
		assertTrue(like.match(match), "Failed: " + like);
	}

	@Test
	public void evaluateUnicode() {
		Expression like = new ASTLike(new ASTObjPath("artistName"), "àбğþ%");
		Expression notLike = new ASTNotLike(new ASTObjPath("artistName"), "àбğþ%");

		Artist noMatch1 = new Artist();
		noMatch1.setArtistName("àbğþd");
		assertFalse(like.match(noMatch1));
		assertTrue(notLike.match(noMatch1));

		Artist match1 = new Artist();
		match1.setArtistName("àбğþ");
		assertTrue(like.match(match1), "Failed: " + like);
		assertFalse(notLike.match(match1), "Failed: " + notLike);

		Artist match2 = new Artist();
		match2.setArtistName("àбğþa");
		assertTrue(like.match(match2), "Failed: " + like);
		assertFalse(notLike.match(match2), "Failed: " + notLike);
	}

}
