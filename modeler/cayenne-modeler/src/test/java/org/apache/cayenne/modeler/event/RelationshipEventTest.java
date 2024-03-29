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

package org.apache.cayenne.modeler.event;

import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 */
public class RelationshipEventTest {

    @Test
	public void testRelationship() {
		Object src = new Object();
		DbRelationship r = new DbRelationship();
		r.setName("xyz");
		RelationshipEvent e = new RelationshipEvent(src, null, null);

		e.setRelationship(r);
		assertSame(r, e.getRelationship());
	}
}

