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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.annotation.CommitLog;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compiles {@link CommitLogEntity}'s based on {@link CommitLog} annotation.
 *
 * @since 4.0
 */
public class AnnotationCommitLogEntityFactory implements CommitLogEntityFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationCommitLogEntityFactory.class);

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

	private Provider<DataDomain> domainProvider;
	private ConcurrentMap<String, CommitLogEntity> entities;

	public AnnotationCommitLogEntityFactory(@Inject Provider<DataDomain> domainProvider) {
		this.entities = new ConcurrentHashMap<>();

		// injecting provider instead of DataDomain, as otherwise we end up
		// with circular dependency.
		this.domainProvider = domainProvider;
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
		return domainProvider.get().getEntityResolver();
	}

	private CommitLogEntity createDescriptor(String entityName) {
		EntityResolver entityResolver = getEntityResolver();
		ClassDescriptor classDescriptor = entityResolver.getClassDescriptor(entityName);
		Class<?> objectClass = classDescriptor.getObjectClass();

		CommitLog a = objectClass.getAnnotation(CommitLog.class);
		if (a != null) {
			ObjEntity entity = entityResolver.getObjEntity(entityName);
			return new MutableCommitLogLogEntity(entity).setConfidential(a.confidential())
					.setIgnoreProperties(a.ignoredProperties()).setIgnoreAttributes(a.ignoreAttributes())
					.setIgnoreToOneRelationships(a.ignoreToOneRelationships())
					.setIgnoreToManyRelationships(a.ignoreToManyRelationships());
		}

		@SuppressWarnings("deprecation")
		org.apache.cayenne.commitlog.CommitLog legacyA = objectClass.getAnnotation(org.apache.cayenne.commitlog.CommitLog.class);
		if (legacyA != null) {
			LOGGER.warn("Entity class '{}' uses deprecated @org.apache.cayenne.commitlog.CommitLog annotation. " +
					"Replace with @org.apache.cayenne.annotation.CommitLog.", objectClass.getName());
			ObjEntity entity = entityResolver.getObjEntity(entityName);
			return new MutableCommitLogLogEntity(entity).setConfidential(legacyA.confidential())
					.setIgnoreProperties(legacyA.ignoredProperties()).setIgnoreAttributes(legacyA.ignoreAttributes())
					.setIgnoreToOneRelationships(legacyA.ignoreToOneRelationships())
					.setIgnoreToManyRelationships(legacyA.ignoreToManyRelationships());
		}

		return BLOCKED_ENTITY;
	}
}
