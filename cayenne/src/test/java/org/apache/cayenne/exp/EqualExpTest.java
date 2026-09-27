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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;

import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.junit.jupiter.api.Test;

public class EqualExpTest {

	@Test
	public void toEJBQL() {
		EqualExp e = new EqualExp(ExpressionFactory.pathExp("artistName"), "bla");

		// note single quotes - EJBQL does not support doublequotes...
		assertEquals("x.artistName = 'bla'", e.toEJBQL("x"));
	}

	@Test
	public void appendAsEJBQL() throws IOException {

		EqualExp e = new EqualExp(ExpressionFactory.pathExp("artistName"), "bla");

		StringBuilder buffer = new StringBuilder();
		e.appendAsEJBQL(buffer, "x");
		String ejbql = buffer.toString();
		assertEquals("x.artistName = 'bla'", ejbql);
	}

	@Test
	public void evaluate() {
		EqualExp equalTo = new EqualExp(ExpressionFactory.pathExp("artistName"), "abc");

		Artist match = new Artist();
		match.setArtistName("abc");
		assertTrue(equalTo.match(match));

		Artist noMatch = new Artist();
		noMatch.setArtistName("123");
		assertFalse(equalTo.match(noMatch), "Failed: " + equalTo);
	}

	@Test
	public void evaluate_Null() {
		EqualExp equalToNull = new EqualExp(ExpressionFactory.pathExp("artistName"), null);
		EqualExp equalToNotNull = new EqualExp(ExpressionFactory.pathExp("artistName"), "abc");

		Artist match = new Artist();
		assertTrue(equalToNull.match(match));
		assertFalse(equalToNotNull.match(match));

		Artist noMatch = new Artist();
		noMatch.setArtistName("abc");
		assertFalse(equalToNull.match(noMatch));
	}

	@Test
	public void evaluate_BigDecimal() {
		BigDecimal bd1 = new BigDecimal("2.0");
		BigDecimal bd2 = new BigDecimal("2.0");
		BigDecimal bd3 = new BigDecimal("2.00");
		BigDecimal bd4 = new BigDecimal("2.01");

		EqualExp equalTo = new EqualExp(ExpressionFactory.pathExp(Painting.ESTIMATED_PRICE.getName()), bd1);

		Painting p = new Painting();
		p.setEstimatedPrice(bd2);
		assertTrue(equalTo.match(p));

		// BigDecimals must compare regardless of the number of trailing zeros
		// (see CAY-280)
		p.setEstimatedPrice(bd3);
		assertTrue(equalTo.match(p));

		p.setEstimatedPrice(bd4);
		assertFalse(equalTo.match(p));
	}

	@Test
	public void evaluateUnicodeChars() {
		EqualExp equalToFull = new EqualExp(ExpressionFactory.pathExp("artistName"), "àбçğþ");
		EqualExp equalToSimple = new EqualExp(ExpressionFactory.pathExp("artistName"), "àğç");

		Artist noMatch = new Artist();
		noMatch.setArtistName("agc");
		assertFalse(equalToSimple.match(noMatch));
		assertFalse(equalToFull.match(noMatch));

		Artist matchSimple = new Artist();
		matchSimple.setArtistName("àğç");
		assertTrue(equalToSimple.match(matchSimple));
		assertFalse(equalToFull.match(matchSimple));

		Artist matchFull = new Artist();
		matchFull.setArtistName("àбçğþ");
		assertFalse(equalToSimple.match(matchFull));
		assertTrue(equalToFull.match(matchFull));
	}
}
