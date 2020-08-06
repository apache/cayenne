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

public class ASTGreaterOrEqualTest {

	@Test
	public void testEvaluate() {
		Expression e = new ASTGreaterOrEqual(new ASTObjPath("estimatedPrice"), new BigDecimal(10000d));

		Painting noMatch = new Painting();
		noMatch.setEstimatedPrice(new BigDecimal(9999));
		assertFalse(e.match(noMatch));

		Painting match1 = new Painting();
		match1.setEstimatedPrice(new BigDecimal(10000));
		assertTrue(e.match(match1));

		Painting match = new Painting();
		match.setEstimatedPrice(new BigDecimal(10001));
		assertTrue("Failed: " + e, e.match(match));
	}

	@Test
	public void testEvaluate_Null() {
		Expression gtNull = new ASTGreaterOrEqual(new ASTObjPath("estimatedPrice"), null);
		Expression gtNotNull = new ASTGreaterOrEqual(new ASTObjPath("estimatedPrice"), new BigDecimal(10000d));

		Painting noMatch = new Painting();
		assertFalse(gtNull.match(noMatch));
		assertFalse(gtNotNull.match(noMatch));
	}

}
