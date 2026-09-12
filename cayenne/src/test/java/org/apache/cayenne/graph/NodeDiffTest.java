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

import org.apache.cayenne.ObjectId;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class NodeDiffTest {

	@Test
	public void getNodeId() {
		ObjectId id = ObjectId.of("E");
		NodeDiff diff = new ConcreteNodeDiff(id);
		assertSame(id, diff.getNodeId());
	}

	@Test
	public void compareTo() {
		NodeDiff d1 = new ConcreteNodeDiff(ObjectId.of("x"), 1);
		NodeDiff d2 = new ConcreteNodeDiff(ObjectId.of("y"), 2);
		NodeDiff d3 = new ConcreteNodeDiff(ObjectId.of("z"), 3);
		NodeDiff d4 = new ConcreteNodeDiff(ObjectId.of("a"), 2);

		assertTrue(d1.compareTo(d2) < 0);
		assertTrue(d2.compareTo(d1) > 0);
		assertTrue(d1.compareTo(d3) < 0);
		assertTrue(d2.compareTo(d4) == 0);
		assertTrue(d2.compareTo(d3) < 0);
	}

	@SuppressWarnings("serial")
	class ConcreteNodeDiff extends NodeDiff {

		public ConcreteNodeDiff(ObjectId id) {
			super(id);
		}

		public ConcreteNodeDiff(ObjectId id, int diffId) {
			super(id, diffId);
		}

		@Override
		public void apply(GraphChangeHandler tracker) {
		}

		@Override
		public void undo(GraphChangeHandler tracker) {
		}
	}
}
