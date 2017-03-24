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
package org.apache.cayenne.pref;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.cayenne.CayenneRuntimeException;

public class ChildrenMapPreference extends PreferenceDecorator {

	private Map<String, Object> children;
	private List<String> removeObject;

	public ChildrenMapPreference(CayennePreference delegate) {
		super(delegate);
		this.children = new HashMap<>();
		this.removeObject = new ArrayList<>();
	}

	public ChildrenMapPreference(CayennePreference delegate, Preferences preferences) {
		super(delegate);
		delegate.setCurrentPreference(preferences);
		this.children = new HashMap<>();
	}

	public void initChildrenPreferences() {
		Map<String, Object> children = new HashMap<>();
		try {
			String[] names = getCurrentPreference().childrenNames();
			for (String name : names) {
				try {
					Object newInstance = delegate.getClass()
							.getConstructor(String.class, boolean.class)
							.newInstance(name, true);
					children.put(name, newInstance);
				} catch (Throwable e) {
					throw new CayenneRuntimeException("Error initializing preference", e);
				}
			}

			this.children.putAll(children);
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	public Map getChildrenPreferences() {
		return children;
	}

	public CayennePreference getObject(String key) {
		return (CayennePreference) children.get(key);
	}

	public void remove(String key) {
		removeObject.add(key);
		children.remove(key);
	}

	public CayennePreference create(String nodeName) {
		try {
			Object newInstance = delegate.getClass()
					.getConstructor(String.class, boolean.class)
					.newInstance(nodeName, false);
			children.put(nodeName, newInstance);
		} catch (Throwable e) {
			throw new CayenneRuntimeException("Error creating preference");
		}
		return (CayennePreference) children.get(nodeName);
	}

	public void create(String nodeName, Object obj) {
		children.put(nodeName, obj);
	}

	public void save() {
		if (removeObject.size() > 0) {
			for (String aRemoveObject : removeObject) {
				try {
					delegate.getCurrentPreference().node(aRemoveObject).removeNode();
				} catch (BackingStoreException e) {
					throw new CayenneRuntimeException("Error saving preference");
				}
			}
		}

		for (Map.Entry<String, Object> pairs : children.entrySet()) {
			delegate.getCurrentPreference().node(pairs.getKey());
			((CayennePreference) pairs.getValue()).saveObjectPreference();
		}
	}

	public void cancel() {
		this.children.clear();
		initChildrenPreferences();
	}
}
