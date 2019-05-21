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
package org.apache.cayenne.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Test;

public class SelectByIdTest {

	@Test
	public void testSerializabilityWithHessian() throws Exception {
		SelectById<Artist> o = SelectById.query(Artist.class, 5);
		Object clone = HessianUtil.cloneViaClientServerSerialization(o, new EntityResolver());

		assertTrue(clone instanceof SelectById);
		SelectById<?> c1 = (SelectById<?>) clone;

		assertNotSame(o, c1);
		assertEquals(o.entityType, c1.entityType);
		assertEquals(o.singleId, c1.singleId);
	}
}
