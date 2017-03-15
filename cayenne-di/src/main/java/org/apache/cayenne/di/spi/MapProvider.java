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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Provider;

/**
 * @since 3.1
 */
class MapProvider<T> implements Provider<Map<String, T>> {

	private Map<String, Provider<? extends T>> providers;

	public MapProvider() {
		this.providers = new HashMap<>();
	}

	@Override
	public Map<String, T> get() throws DIRuntimeException {
		Map<String, T> map = new HashMap<>();

		for (Entry<String, Provider<? extends T>> entry : providers.entrySet()) {
			map.put(entry.getKey(), entry.getValue().get());
		}

		return map;
	}

	void put(String key, Provider<? extends T> provider) {
		providers.put(key, provider);
	}
}
