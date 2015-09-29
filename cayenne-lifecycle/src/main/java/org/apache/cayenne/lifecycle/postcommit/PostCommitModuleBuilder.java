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
package org.apache.cayenne.lifecycle.postcommit;

import java.util.Collection;
import java.util.HashSet;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.lifecycle.audit.Auditable;
import org.apache.cayenne.lifecycle.postcommit.meta.AuditablePostCommitEntityFactory;
import org.apache.cayenne.lifecycle.postcommit.meta.IncludeAllPostCommitEntityFactory;
import org.apache.cayenne.lifecycle.postcommit.meta.PostCommitEntity;
import org.apache.cayenne.lifecycle.postcommit.meta.PostCommitEntityFactory;

/**
 * A builder of a module that integrates {@link PostCommitFilter} and
 * {@link PostCommitListener} in Cayenne.
 * 
 * @since 4.0
 */
public class PostCommitModuleBuilder {

	public static PostCommitModuleBuilder builder() {
		return new PostCommitModuleBuilder();
	}

	private Class<? extends PostCommitEntityFactory> entityFactoryType;
	private Collection<Class<? extends PostCommitListener>> listenerTypes;
	private Collection<PostCommitListener> listenerInstances;

	PostCommitModuleBuilder() {
		this.entityFactoryType = IncludeAllPostCommitEntityFactory.class;
		this.listenerTypes = new HashSet<>();
		this.listenerInstances = new HashSet<>();
	}

	public PostCommitModuleBuilder listener(Class<? extends PostCommitListener> type) {
		this.listenerTypes.add(type);
		return this;
	}

	public PostCommitModuleBuilder listener(PostCommitListener instance) {
		this.listenerInstances.add(instance);
		return this;
	}

	/**
	 * Installs entity filter that would only include entities annotated with
	 * {@link Auditable} on the callbacks. Also {@link Auditable#confidential()}
	 * properties will be obfuscated and {@link Auditable#ignoredProperties()} -
	 * excluded from the change collection.
	 */
	public PostCommitModuleBuilder auditableEntitiesOnly() {
		this.entityFactoryType = AuditablePostCommitEntityFactory.class;
		return this;
	}

	/**
	 * Installs a custom factory for {@link PostCommitEntity} objects that
	 * allows implementors to use their own annotations, etc.
	 */
	public PostCommitModuleBuilder entityFactory(Class<? extends PostCommitEntityFactory> entityFactoryType) {
		this.entityFactoryType = entityFactoryType;
		return this;
	}

	/**
	 * Creates a DI module that would install {@link PostCommitFilter} and its
	 * listeners in Cayenne.
	 */
	public Module build() {
		return new Module() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void configure(Binder binder) {

				ListBuilder<PostCommitListener> listeners = binder
						.<PostCommitListener> bindList(PostCommitFilter.POST_COMMIT_LISTENERS_LIST)
						.addAll(listenerInstances);

				// types have to be added one-by-one
				for (Class type : listenerTypes) {

					// TODO: temp hack - need to bind each type before adding to
					// collection...
					binder.bind(type).to(type);

					listeners.add(type);
				}

				binder.bind(PostCommitFilter.class).to(PostCommitFilter.class);

				// TODO: should be ordering the filter to go inside transaction
				// once the corresponding Jiras are available in Cayenne
				binder.bindList(Constants.SERVER_DOMAIN_FILTERS_LIST).add(PostCommitFilter.class);

				binder.bind(PostCommitEntityFactory.class).to(entityFactoryType);
			}
		};
	}
}
