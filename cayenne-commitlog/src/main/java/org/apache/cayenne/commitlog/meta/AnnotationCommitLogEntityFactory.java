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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.commitlog.CommitLog;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Compiles {@link CommitLogEntity}'s based on {@link CommitLog} annotation.
 * 
 * @since 4.0
 */
public class AnnotationCommitLogEntityFactory implements CommitLogEntityFactory {

	private static final CommitLogEntity BLOCKED_ENTITY = new CommitLogEntity() {

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
	private ConcurrentMap<String, CommitLogEntity> entities;

	public AnnotationCommitLogEntityFactory(@Inject Provider<DataChannel> channelProvider) {
		this.entities = new ConcurrentHashMap<>();

		// injecting provider instead of DataChannel, as otherwise we end up
		// with circular dependency.
		this.channelProvider = channelProvider;
	}

	@Override
	public CommitLogEntity getEntity(ObjectId id) {
		String entityName = id.getEntityName();

		CommitLogEntity descriptor = entities.get(entityName);
		if (descriptor == null) {
			CommitLogEntity newDescriptor = createDescriptor(entityName);
			CommitLogEntity existingDescriptor = entities.putIfAbsent(entityName, newDescriptor);
			descriptor = (existingDescriptor != null) ? existingDescriptor : newDescriptor;
		}

		return descriptor;

	}

	private EntityResolver getEntityResolver() {
		return channelProvider.get().getEntityResolver();
	}

	private CommitLogEntity createDescriptor(String entityName) {
		EntityResolver entityResolver = getEntityResolver();
		ClassDescriptor classDescriptor = entityResolver.getClassDescriptor(entityName);

		CommitLog a = classDescriptor.getObjectClass().getAnnotation(CommitLog.class);
		if (a == null) {
			return BLOCKED_ENTITY;
		}

		ObjEntity entity = entityResolver.getObjEntity(entityName);
		return new MutableCommitLogLogEntity(entity).setConfidential(a.confidential())
				.setIgnoreProperties(a.ignoredProperties()).setIgnoreAttributes(a.ignoreAttributes())
				.setIgnoreToOneRelationships(a.ignoreToOneRelationships())
				.setIgnoreToManyRelationships(a.ignoreToManyRelationships());
	}

}
