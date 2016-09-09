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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.lifecycle.audit.Auditable;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Compiles {@link PostCommitEntity}'s based on {@link Auditable} annotation.
 * 
 * @since 4.0
 */
public class AuditablePostCommitEntityFactory implements PostCommitEntityFactory {

	private static final PostCommitEntity BLOCKED_ENTITY = new PostCommitEntity() {

		@Override
		public boolean isIncluded(String property) {
			return false;
		}

		@Override
		public boolean isConfidential(String property) {
			return false;
		}

		@Override
		public boolean isIncluded() {
			return false;
		}
	};

	private Provider<DataChannel> channelProvider;
	private ConcurrentMap<String, PostCommitEntity> entities;

	public AuditablePostCommitEntityFactory(@Inject Provider<DataChannel> channelProvider) {
		this.entities = new ConcurrentHashMap<>();

		// injecting provider instead of DataChannel, as otherwise we end up
		// with circular dependency.
		this.channelProvider = channelProvider;
	}

	@Override
	public PostCommitEntity getEntity(ObjectId id) {
		String entityName = id.getEntityName();

		PostCommitEntity descriptor = entities.get(entityName);
		if (descriptor == null) {
			PostCommitEntity newDescriptor = createDescriptor(entityName);
			PostCommitEntity existingDescriptor = entities.putIfAbsent(entityName, newDescriptor);
			descriptor = (existingDescriptor != null) ? existingDescriptor : newDescriptor;
		}

		return descriptor;

	}

	private EntityResolver getEntityResolver() {
		return channelProvider.get().getEntityResolver();
	}

	private PostCommitEntity createDescriptor(String entityName) {
		EntityResolver entityResolver = getEntityResolver();
		ClassDescriptor classDescriptor = entityResolver.getClassDescriptor(entityName);

		Auditable annotation = classDescriptor.getObjectClass().getAnnotation(Auditable.class);
		if (annotation == null) {
			return BLOCKED_ENTITY;
		}

		ObjEntity entity = entityResolver.getObjEntity(entityName);
		return new DefaultPostCommitEntity(entity, annotation.ignoredProperties(), annotation.confidential());
	}

}
