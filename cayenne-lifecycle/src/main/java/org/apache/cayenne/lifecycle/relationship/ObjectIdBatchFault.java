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
package org.apache.cayenne.lifecycle.relationship;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.lifecycle.id.StringIdFetcher;

import java.util.List;
import java.util.Map;

/**
 * Provides lazy faulting functionality for a map of objects identified by
 * String ObjectId.
 * 
 * @since 3.1
 */
class ObjectIdBatchFault {

	private final ObjectContext context;
	private final List<ObjectIdBatchSourceItem> sources;
	private volatile Map<String, Persistent> resolved;

	ObjectIdBatchFault(ObjectContext context, List<ObjectIdBatchSourceItem> sources) {
		this.context = context;
		this.sources = sources;
	}

	Map<String, Persistent> getObjects() {

		if (resolved == null) {

			synchronized (this) {

				if (resolved == null) {
					resolved = fetchObjects();
				}
			}
		}

		return resolved;
	}

	private Map<String, Persistent> fetchObjects() {

		if (sources == null) {
			return Map.of();
		}

		List<String> ids = sources.stream().map(ObjectIdBatchSourceItem::getId).toList();
		return StringIdFetcher.fetch(context, ids);
	}
}
