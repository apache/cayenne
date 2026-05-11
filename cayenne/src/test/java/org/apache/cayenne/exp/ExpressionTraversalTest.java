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

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExpressionTraversalTest {

	private TstTraversalHandler handler;

	@BeforeEach
	public void setUp() throws Exception {
		handler = new TstTraversalHandler();
	}

	@Test
	public void unary_Negative1() {
		doExpressionTest("-5", 1, 1);
	}

	@Test
	public void unary_Negative2() {
		doExpressionTest("-estimatedPrice", 2, 1);
	}

	@Test
	public void unary_Negative3() {
		doExpressionTest("-toGallery.paintingArray.estimatedPrice", 2, 1);
	}

	@Test
	public void binary_In1() {
		doExpressionTest("toGallery.galleryName in ('g1', 'g2', 'g3')", 3, 2);
	}

	@Test
	public void binary_In2() {
		Expression exp = ExpressionFactory.inExp("toGallery.galleryName", Arrays.asList("g1", "g2", "g3"));
		doExpressionTest(exp, 3, 2);
	}

	@Test
	public void binary_In3() {
		Expression exp = ExpressionFactory.inExp("toGallery.galleryName", "g1", "g2", "g3");
		doExpressionTest(exp, 3, 2);
	}

	@Test
	public void binary_Like() {
		doExpressionTest("toGallery.galleryName like 'a%'", 2, 2);
	}

	@Test
	public void binary_LikeIgnoreCase() {
		doExpressionTest("toGallery.galleryName likeIgnoreCase 'a%'", 2, 2);
	}

	@Test
	public void binary_IsNull() {
		doExpressionTest("toGallery.galleryName = null", 2, 2);
	}

	@Test
	public void binary_IsNotNull() {
		doExpressionTest("toGallery.galleryName != null", 2, 2);
	}

	@Test
	public void ternary_Between() {
		doExpressionTest("estimatedPrice between 3000 and 15000", 2, 3);
	}

	private void doExpressionTest(String expression, int totalNodes, int totalLeaves) {
		doExpressionTest(ExpressionFactory.exp(expression), totalNodes, totalLeaves);
	}

	private void doExpressionTest(Expression expression, int totalNodes, int totalLeaves) {
		handler.reset();
		expression.traverse(handler);

		// assert statistics
		handler.assertConsistency();
		assertEquals(totalNodes, handler.getNodeCount());
		assertEquals(totalLeaves, handler.getLeafs());
	}
}
