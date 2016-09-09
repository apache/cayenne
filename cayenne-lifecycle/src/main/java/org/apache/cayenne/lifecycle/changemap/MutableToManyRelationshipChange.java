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
package org.apache.cayenne.lifecycle.changemap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.ObjectId;

/**
 * @since 4.0
 */
public class MutableToManyRelationshipChange implements ToManyRelationshipChange {

	private Collection<ObjectId> added;
	private Collection<ObjectId> removed;

	@Override
	public Collection<ObjectId> getAdded() {
		return added == null ? Collections.<ObjectId> emptyList() : added;
	}

	@Override
	public Collection<ObjectId> getRemoved() {
		return removed == null ? Collections.<ObjectId> emptyList() : removed;
	}

	public void connected(ObjectId o) {

		// TODO: cancel previously removed ?
		if (added == null) {
			added = new ArrayList<>();
		}

		added.add(o);
	}

	public void disconnected(ObjectId o) {

		// TODO: cancel previously added ?
		if (removed == null) {
			removed = new ArrayList<>();
		}

		removed.add(o);
	}
}
