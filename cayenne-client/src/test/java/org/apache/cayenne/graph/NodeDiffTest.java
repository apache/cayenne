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
package org.apache.cayenne.graph;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NodeDiffTest {

	@Test
	public void testHessianSerialization() throws Exception {

		// id must be a serializable object...
		String id = "abcd";
		NodeDiff diff = new ConcreteNodeDiff(id);

		Object d = HessianUtil.cloneViaClientServerSerialization(diff, new EntityResolver());
		assertNotNull(d);
		assertNotNull(((NodeDiff) d).getNodeId());
		assertEquals(id, ((NodeDiff) d).getNodeId());
	}

	@SuppressWarnings("serial")
	static class ConcreteNodeDiff extends NodeDiff {

		ConcreteNodeDiff(Object id) {
			super(id);
		}

		@Override
		public void apply(GraphChangeHandler tracker) {
		}

		@Override
		public void undo(GraphChangeHandler tracker) {
		}
	}
}
