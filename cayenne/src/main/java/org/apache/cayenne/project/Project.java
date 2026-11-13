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
package org.apache.cayenne.project;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A Cayenne mapping project, normally loaded from XML configuration. It is the root of the project nodes tree,
 * and contains DataMaps and the settings of a runtime DataChannel.
 *
 * @since 5.0
 */
public class Project implements ProjectNode, XMLSerializable {

	/**
	 * The namespace in which the data map XML file will be created.
	 */
	public static final String SCHEMA_XSD = "http://cayenne.apache.org/schema/15/project";
	public static final String SCHEMA_XSD_LOCATION = "https://cayenne.apache.org/schema/15/project.xsd";

	protected String name;
	protected boolean sharedCacheEnabled;
	protected boolean validatingObjectsOnCommit;
	protected Collection<DataMap> dataMaps;
	protected Resource configurationSource;

	/**
	 * @since 6.0
	 */
	protected Set<String> disabledInspections;

	public Project() {
		sharedCacheEnabled = true;
		validatingObjectsOnCommit = true;
		dataMaps = new ArrayList<>(5);
		disabledInspections = new LinkedHashSet<>();
	}

	@Override
	public void encodeAsXML(XMLEncoder encoder, ProjectNodeVisitor<?> delegate) {

		encoder.start("project")
				.attribute("xmlns", SCHEMA_XSD)
				.attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", true)
				.attribute("xsi:schemaLocation", SCHEMA_XSD + " " + SCHEMA_XSD_LOCATION, true)
				// both are true by default
				.attribute("sharedCache", sharedCacheEnabled ? null : "false", true)
				.attribute("validateOnCommit", validatingObjectsOnCommit ? null : "false", true);

		if (!dataMaps.isEmpty()) {
			List<DataMap> maps = new ArrayList<>(this.dataMaps);
			Collections.sort(maps);

			for (DataMap dataMap : maps) {
				encoder.start("map").attribute("name", dataMap.getName().trim()).end();
			}
		}

		if (!disabledInspections.isEmpty()) {
			encoder.start("validation");
			for (String inspection : disabledInspections) {
				encoder.textTag("exclude", inspection);
			}
			encoder.end();
		}

		encoder.end();
	}

	@Override
	public <T> T acceptVisitor(ProjectNodeVisitor<T> visitor) {
		return visitor.visitProject(this);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns whether the object snapshots are cached in a cache shared between all ObjectContexts. True by default.
	 */
	public boolean isSharedCacheEnabled() {
		return sharedCacheEnabled;
	}

	public void setSharedCacheEnabled(boolean sharedCacheEnabled) {
		this.sharedCacheEnabled = sharedCacheEnabled;
	}

	/**
	 * Returns whether the objects are validated before they are committed. True by default.
	 */
	public boolean isValidatingObjectsOnCommit() {
		return validatingObjectsOnCommit;
	}

	public void setValidatingObjectsOnCommit(boolean validatingObjectsOnCommit) {
		this.validatingObjectsOnCommit = validatingObjectsOnCommit;
	}

	/**
	 * Returns the names of the disabled project validation inspections. Not used by the runtime.
	 *
	 * @since 6.0
	 */
	public Set<String> getDisabledInspections() {
		return Collections.unmodifiableSet(disabledInspections);
	}

	/**
	 * Replaces the names of the disabled project validation inspections. The names are trimmed, deduplicated and
	 * otherwise unchecked, and the blank ones are skipped.
	 *
	 * @since 6.0
	 */
	public void setDisabledInspections(Collection<String> disabledInspections) {
		Set<String> names = new LinkedHashSet<>();
		if (disabledInspections != null) {
			for (String name : disabledInspections) {
				if (name != null && !name.isBlank()) {
					names.add(name.trim());
				}
			}
		}
		this.disabledInspections = names;
	}

	public Collection<DataMap> getDataMaps() {
		return dataMaps;
	}

	public DataMap getDataMap(String name) {
		for (DataMap map : dataMaps) {
			if (name.equals(map.getName())) {
				return map;
			}
		}
		return null;
	}

	public Resource getConfigurationSource() {
		return configurationSource;
	}

	public void setConfigurationSource(Resource configurationSource) {
		this.configurationSource = configurationSource;
	}
}
