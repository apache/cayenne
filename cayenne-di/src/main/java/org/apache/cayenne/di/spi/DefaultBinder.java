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
package org.apache.cayenne.di.spi;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.BindingBuilder;
import org.apache.cayenne.di.DecoratorBuilder;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.MapBuilder;

/**
 * @since 3.1
 */
class DefaultBinder implements Binder {

	private DefaultInjector injector;

	DefaultBinder(DefaultInjector injector) {
		this.injector = injector;
	}

	@Override
	public <T> BindingBuilder<T> bind(Class<T> interfaceType) {
		return new DefaultBindingBuilder<>(Key.get(interfaceType), injector);
	}

	@Override
	public <T> BindingBuilder<T> bind(Key<T> key) {
		return new DefaultBindingBuilder<>(key, injector);
	}

	/**
	 * @since 4.0
	 */
	@Override
	public <T> ListBuilder<T> bindList(Class<T> valueType) {
		return bindList(valueType, null);
	}

	/**
	 * @since 4.0
	 */
	@Override
	public <T> ListBuilder<T> bindList(Class<T> valueType, String bindingName) {
		return new DefaultListBuilder<>(Key.getListOf(valueType, bindingName), injector);
	}

	/**
	 * @since 4.0
	 */
	@Override
	public <T> MapBuilder<T> bindMap(Class<T> valueType) {
		return bindMap(valueType, null);
	}

	/**
	 * @since 4.0
	 */
	@Override
	public <T> MapBuilder<T> bindMap(Class<T> valueType, String bindingName) {
		return new DefaultMapBuilder<>(Key.getMapOf(String.class, valueType, bindingName), injector);
	}

	@Override
	public <T> DecoratorBuilder<T> decorate(Class<T> interfaceType) {
		return new DefaultDecoratorBuilder<>(Key.get(interfaceType), injector);
	}

	@Override
	public <T> DecoratorBuilder<T> decorate(Key<T> key) {
		return new DefaultDecoratorBuilder<>(key, injector);
	}
}
