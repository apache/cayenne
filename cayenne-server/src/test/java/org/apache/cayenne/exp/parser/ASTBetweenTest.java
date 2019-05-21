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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.testdo.testmap.Painting;
import org.junit.Test;

public class ASTBetweenTest {

	@Test
	public void testEvaluate() {
		// evaluate both BETWEEN and NOT_BETWEEN
		Expression between = new ASTBetween(new ASTObjPath("estimatedPrice"), new BigDecimal(10d), new BigDecimal(20d));
		Expression notBetween = new ASTNotBetween(new ASTObjPath("estimatedPrice"), new BigDecimal(10d),
				new BigDecimal(20d));

		Painting noMatch = new Painting();
		noMatch.setEstimatedPrice(new BigDecimal(21));
		assertFalse(between.match(noMatch));
		assertTrue(notBetween.match(noMatch));

		Painting match1 = new Painting();
		match1.setEstimatedPrice(new BigDecimal(20));
		assertTrue(between.match(match1));
		assertFalse(notBetween.match(match1));

		Painting match2 = new Painting();
		match2.setEstimatedPrice(new BigDecimal(10));
		assertTrue("Failed: " + between, between.match(match2));
		assertFalse("Failed: " + notBetween, notBetween.match(match2));

		Painting match3 = new Painting();
		match3.setEstimatedPrice(new BigDecimal(11));
		assertTrue("Failed: " + between, between.match(match3));
		assertFalse("Failed: " + notBetween, notBetween.match(match3));
	}

	@Test
	public void testEvaluate_Null() {
		Expression btNull = new ASTBetween(new ASTObjPath("estimatedPrice"), new BigDecimal(10d), new BigDecimal(20d));
		Expression btNotNull = new ASTNotBetween(new ASTObjPath("estimatedPrice"), new BigDecimal(10d), new BigDecimal(
				20d));

		Painting noMatch = new Painting();
		assertFalse(btNull.match(noMatch));
		assertFalse(btNotNull.match(noMatch));
	}

}
