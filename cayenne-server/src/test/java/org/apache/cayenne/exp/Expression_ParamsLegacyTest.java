/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.exp;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;

/**
 * Testing deprecated Expression.expWithParameters(..) API.
 * 
 * @deprecated since 4.0
 */
@Deprecated
public class Expression_ParamsLegacyTest {

	@SuppressWarnings("serial")
	@Test
	public void testExpWithParams_Prune() throws Exception {
		Expression e = ExpressionFactory
				.exp("k1 = $test or k2 = $v2 or k3 = $v3");

		Expression ep = e.expWithParameters(new HashMap<String, Object>() {
			{
				put("test", "T");
				put("v2", "K");
				put("v3", 5);
			}
		}, true);

		assertEquals("(k1 = \"T\") or (k2 = \"K\") or (k3 = 5)", ep.toString());
	}

	@SuppressWarnings("serial")
	@Test
	public void testExpWithParams_PrunePartial() throws Exception {
		Expression e = ExpressionFactory
				.exp("k1 = $test or k2 = $v2 or k3 = $v3");

		Expression ep = e.expWithParameters(new HashMap<String, Object>() {
			{
				put("test", "T");
				put("v3", 5);
			}
		}, true);

		assertEquals("(k1 = \"T\") or (k3 = 5)", ep.toString());
	}

	@SuppressWarnings("serial")
	@Test
	public void testExpWithParams_NoPrune() throws Exception {
		Expression e = ExpressionFactory
				.exp("k1 = $test or k2 = $v2 or k3 = $v3");

		Expression ep = e.expWithParameters(new HashMap<String, Object>() {
			{
				put("test", "T");
				put("v2", "K");
				put("v3", 5);
			}
		}, false);

		assertEquals("(k1 = \"T\") or (k2 = \"K\") or (k3 = 5)", ep.toString());
	}

	@Test(expected = ExpressionException.class)
	public void testExpWithParams_NoPrune_Partial() throws Exception {
		Expression e = ExpressionFactory
				.exp("k1 = $test or k2 = $v2 or k3 = $v3");
		e.expWithParameters(new HashMap<String, Object>(), false);
	}
}
