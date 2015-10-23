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
package org.apache.cayenne.lifecycle.postcommit.meta;

import java.util.Collection;
import java.util.HashSet;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * @since 4.0
 */
public class MutablePostCommitEntity implements PostCommitEntity {

	private Collection<String> ignoredProperties;
	private Collection<String> confidentialProperties;
	private ObjEntity entity;

	public MutablePostCommitEntity(ObjEntity entity) {
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

	MutablePostCommitEntity setConfidential(String[] confidentialProperties) {

		if (confidentialProperties != null) {
			for (String property : confidentialProperties) {
				this.confidentialProperties.add(property);
			}
		}

		return this;
	}
	
	MutablePostCommitEntity setIgnoreProperties(String[] properties) {
		if (properties != null) {
			for (String property : properties) {
				this.ignoredProperties.add(property);
			}
		}

		return this;
	}

	MutablePostCommitEntity setIgnoreAttributes(boolean ignore) {
		if (ignore) {
			for (ObjAttribute a : entity.getAttributes()) {
				this.ignoredProperties.add(a.getName());
			}
		}

		return this;
	}

	MutablePostCommitEntity setIgnoreToOneRelationships(boolean ignore) {
		if (ignore) {
			for (ObjRelationship r : entity.getRelationships()) {
				if (!r.isToMany()) {
					this.ignoredProperties.add(r.getName());
				}
			}
		}

		return this;
	}

	MutablePostCommitEntity setIgnoreToManyRelationships(boolean ignore) {
		if (ignore) {
			for (ObjRelationship r : entity.getRelationships()) {
				if (r.isToMany()) {
					this.ignoredProperties.add(r.getName());
				}
			}
		}

		return this;
	}
}
