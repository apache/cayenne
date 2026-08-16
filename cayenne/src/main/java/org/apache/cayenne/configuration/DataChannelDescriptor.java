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
package org.apache.cayenne.configuration;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A descriptor of a DataChannel normally loaded from XML configuration.
 * 
 * @since 3.1
 */
public class DataChannelDescriptor implements ConfigurationNode, Serializable, XMLSerializable {

	private static final long serialVersionUID = 6567527544207035602L;

	/**
	 * The namespace in which the data map XML file will be created.
	 */
	public static final String SCHEMA_XSD = "http://cayenne.apache.org/schema/13/domain";
	public static final String SCHEMA_XSD_LOCATION = "https://cayenne.apache.org/schema/13/domain.xsd";

	protected String name;
	protected Map<String, String> properties;
	protected Collection<DataMap> dataMaps;
	protected transient Resource configurationSource;

	public DataChannelDescriptor() {
		properties = new HashMap<>();
		dataMaps = new ArrayList<>(5);
	}

	@Override
	public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor<?> delegate) {

		encoder.start("domain")
				.attribute("xmlns", SCHEMA_XSD)
				.attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", true)
				.attribute("xsi:schemaLocation", SCHEMA_XSD + " " + SCHEMA_XSD_LOCATION, true)
				.projectVersion();

		if (!properties.isEmpty()) {
			List<String> keys = new ArrayList<>(properties.keySet());
			Collections.sort(keys);

			for (String key : keys) {
				encoder.property(key, properties.get(key));
			}
		}

		if (!dataMaps.isEmpty()) {
			List<DataMap> maps = new ArrayList<>(this.dataMaps);
			Collections.sort(maps);

			for (DataMap dataMap : maps) {
				encoder.start("map").attribute("name", dataMap.getName().trim()).end();
			}
		}

		delegate.visitDataChannelDescriptor(this);
		encoder.end();
	}

	public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
		return visitor.visitDataChannelDescriptor(this);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getProperties() {
		return properties;
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
