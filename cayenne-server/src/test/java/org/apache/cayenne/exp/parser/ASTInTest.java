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
import java.math.BigDecimal;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.testdo.testmap.Painting;
import org.junit.Test;

public class ASTInTest {

	@Test
	public void testToEJBQL_in() throws IOException {
		ASTIn e = new ASTIn(new ASTObjPath("consignment.parts"), new ASTList(new Object[] { 91, 23 }));
		assertEquals("x.consignment.parts in (91, 23)", e.toEJBQL("x"));
	}

	@Test
	public void testEvaluate() {
		Expression in = new ASTIn(new ASTObjPath("estimatedPrice"), new ASTList(new Object[] { new BigDecimal("10"),
				new BigDecimal("20") }));

		Expression notIn = new ASTNotIn(new ASTObjPath("estimatedPrice"), new ASTList(new Object[] {
				new BigDecimal("10"), new BigDecimal("20") }));

		Painting noMatch1 = new Painting();
		noMatch1.setEstimatedPrice(new BigDecimal("21"));
		assertFalse(in.match(noMatch1));
		assertTrue(notIn.match(noMatch1));

		Painting noMatch2 = new Painting();
		noMatch2.setEstimatedPrice(new BigDecimal("11"));
		assertFalse("Failed: " + in, in.match(noMatch2));
		assertTrue("Failed: " + notIn, notIn.match(noMatch2));

		Painting match1 = new Painting();
		match1.setEstimatedPrice(new BigDecimal("20"));
		assertTrue(in.match(match1));
		assertFalse(notIn.match(match1));

		Painting match2 = new Painting();
		match2.setEstimatedPrice(new BigDecimal("10"));
		assertTrue("Failed: " + in, in.match(match2));
		assertFalse("Failed: " + notIn, notIn.match(match2));
	}

	@Test
	public void testEvaluate_Null() {
		Expression in = new ASTIn(new ASTObjPath("estimatedPrice"), new ASTList(new Object[] { new BigDecimal("10"),
				new BigDecimal("20") }));
		Expression notIn = new ASTNotIn(new ASTObjPath("estimatedPrice"), new ASTList(new Object[] {
				new BigDecimal("10"), new BigDecimal("20") }));

		Painting noMatch = new Painting();
		assertFalse(in.match(noMatch));
		assertFalse(notIn.match(noMatch));
	}

}
