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
package org.apache.cayenne.reflect;

import java.util.Map;

/**
 * @since 4.0
 */
public class MapAccessor implements Accessor {

	private static final long serialVersionUID = 6032801387641617011L;
	
	private String propertyName;

	public MapAccessor(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	public String getName() {
		return propertyName;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getValue(Object object) {
		return ((Map) object).get(propertyName);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void setValue(Object object, Object newValue) {
		((Map) object).put(propertyName, newValue);
	}
}
