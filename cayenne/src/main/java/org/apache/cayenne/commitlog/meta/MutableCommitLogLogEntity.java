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

import java.util.Collection;
import java.util.HashSet;

import org.apache.cayenne.map.compiled.CompiledAttribute;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledRelationship;

/**
 * @since 4.0
 */
public class MutableCommitLogLogEntity implements CommitLogEntity {

	private Collection<String> ignoredProperties;
	private Collection<String> confidentialProperties;
	private CompiledEntity entity;

	public MutableCommitLogLogEntity(CompiledEntity entity) {
		this.entity = entity;
		this.ignoredProperties = new HashSet<>();
		this.confidentialProperties = new HashSet<>();
	}

	@Override
	public boolean isIncluded(String property) {
		return !ignoredProperties.contains(property);
	}

	@Override
	public boolean isIncluded() {
		return true;
	}

	@Override
	public boolean isConfidential(String property) {
		return confidentialProperties.contains(property);
	}

	MutableCommitLogLogEntity setConfidential(String[] confidentialProperties) {

		if (confidentialProperties != null) {
			for (String property : confidentialProperties) {
				this.confidentialProperties.add(property);
			}
		}

		return this;
	}
	
	MutableCommitLogLogEntity setIgnoreProperties(String[] properties) {
		if (properties != null) {
			for (String property : properties) {
				this.ignoredProperties.add(property);
			}
		}

		return this;
	}

	MutableCommitLogLogEntity setIgnoreAttributes(boolean ignore) {
		if (ignore) {
			for (CompiledAttribute a : entity.attributes()) {
				this.ignoredProperties.add(a.name());
			}
		}

		return this;
	}

	MutableCommitLogLogEntity setIgnoreToOneRelationships(boolean ignore) {
		if (ignore) {
			for (CompiledRelationship r : entity.relationships()) {
				if (!r.isToMany()) {
					this.ignoredProperties.add(r.name());
				}
			}
		}

		return this;
	}

	MutableCommitLogLogEntity setIgnoreToManyRelationships(boolean ignore) {
		if (ignore) {
			for (CompiledRelationship r : entity.relationships()) {
				if (r.isToMany()) {
					this.ignoredProperties.add(r.name());
				}
			}
		}

		return this;
	}
}
