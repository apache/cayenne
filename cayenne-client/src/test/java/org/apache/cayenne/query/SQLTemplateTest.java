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
package org.apache.cayenne.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.junit.Test;

public class SQLTemplateTest {

	@Test
	public void testSerializabilityWithHessian() throws Exception {
		SQLTemplate o = new SQLTemplate("Test", "DO SQL");
		Object clone = HessianUtil.cloneViaClientServerSerialization(o, new EntityResolver());

		assertTrue(clone instanceof SQLTemplate);
		SQLTemplate c1 = (SQLTemplate) clone;

		assertNotSame(o, c1);
		assertEquals(o.getRoot(), c1.getRoot());
		assertEquals(o.getDefaultTemplate(), c1.getDefaultTemplate());

		// set immutable parameters ... query must recast them to mutable
		// version
		@SuppressWarnings("unchecked")
		Map<String, Object>[] parameters = new Map[] { Collections.EMPTY_MAP };
		o.setParameters(parameters);

		HessianUtil.cloneViaClientServerSerialization(o, new EntityResolver());
	}
}
