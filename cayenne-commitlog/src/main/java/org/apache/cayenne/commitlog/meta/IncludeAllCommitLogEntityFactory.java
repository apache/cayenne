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
package org.apache.cayenne.commitlog.meta;

import org.apache.cayenne.ObjectId;

/**
 * @since 4.0
 */
public class IncludeAllCommitLogEntityFactory implements CommitLogEntityFactory {

	private static final CommitLogEntity ALLOWED_ENTITY = new CommitLogEntity() {

		@Override
		public boolean isIncluded(String property) {
			return true;
		}

		@Override
		public boolean isConfidential(String property) {
			return false;
		}

		@Override
		public boolean isIncluded() {
			return true;
		}
	};

	@Override
	public CommitLogEntity getEntity(ObjectId id) {
		return ALLOWED_ENTITY;

	}
}
