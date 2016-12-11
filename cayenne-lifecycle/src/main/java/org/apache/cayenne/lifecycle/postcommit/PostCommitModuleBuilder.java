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

import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.lifecycle.audit.Auditable;
import org.apache.cayenne.lifecycle.postcommit.meta.AuditablePostCommitEntityFactory;
import org.apache.cayenne.lifecycle.postcommit.meta.IncludeAllPostCommitEntityFactory;
import org.apache.cayenne.lifecycle.postcommit.meta.PostCommitEntity;
import org.apache.cayenne.lifecycle.postcommit.meta.PostCommitEntityFactory;
import org.apache.cayenne.tx.TransactionFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.HashSet;

/**
 * A builder of a module that integrates {@link PostCommitFilter} and
 * {@link PostCommitListener} in Cayenne.
 * 
 * @since 4.0
 */
public class PostCommitModuleBuilder {

	private static final Log LOGGER = LogFactory.getLog(PostCommitModuleBuilder.class);

	public static PostCommitModuleBuilder builder() {
		return new PostCommitModuleBuilder();
	}

	private Class<? extends PostCommitEntityFactory> entityFactoryType;
	private Collection<Class<? extends PostCommitListener>> listenerTypes;
	private Collection<PostCommitListener> listenerInstances;
	private boolean excludeFromTransaction;

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
	 * If called, events will be dispatched outside of the main commit
	 * transaction. By default events are dispatched within the transaction, so
	 * listeners can commit their code together with the main commit.
	 */
	public PostCommitModuleBuilder excludeFromTransaction() {
		this.excludeFromTransaction = true;
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

				if (listenerTypes.isEmpty() && listenerInstances.isEmpty()) {
					LOGGER.info("No listeners configured. Skipping PostCommitFilter registration");
					return;
				}

				binder.bind(PostCommitEntityFactory.class).to(entityFactoryType);

				ListBuilder<PostCommitListener> listeners = binder.<PostCommitListener> bindList(
						PostCommitFilter.POST_COMMIT_LISTENERS_LIST).addAll(listenerInstances);

				// types have to be added one-by-one
				for (Class type : listenerTypes) {

					// TODO: temp hack - need to bind each type before adding to
					// collection...
					binder.bind(type).to(type);

					listeners.add(type);
				}

				binder.bind(PostCommitFilter.class).to(PostCommitFilter.class);

				if (excludeFromTransaction) {
					ServerModule.contributeDomainFilters(binder).add(PostCommitFilter.class).after(TransactionFilter.class);
				} else {
					ServerModule.contributeDomainFilters(binder).add(PostCommitFilter.class).before(TransactionFilter.class);
				}
			}
		};
	}
}
