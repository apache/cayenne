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
package org.apache.cayenne.configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A descriptor of a DataChannel normally loaded from XML configuration.
 * 
 * @since 3.1
 */
public class DataChannelDescriptor implements ConfigurationNode, Serializable,
        XMLSerializable {

    protected String name;
    protected Map<String, String> properties;
    protected Collection<DataMap> dataMaps;
    protected Collection<DataNodeDescriptor> nodeDescriptors;
    protected Resource configurationSource;
    protected String defaultNodeName;

    public DataChannelDescriptor() {
        properties = new HashMap<String, String>();
        dataMaps = new ArrayList<DataMap>(5);
        nodeDescriptors = new ArrayList<DataNodeDescriptor>(3);
    }

    public void encodeAsXML(XMLEncoder encoder) {

        encoder.print("<domain");
        encoder.printProjectVersion();
        encoder.println(">");

        encoder.indent(1);
        boolean breakNeeded = false;

        if (!properties.isEmpty()) {
            breakNeeded = true;

            List<String> keys = new ArrayList<String>(properties.keySet());
            Collections.sort(keys);

            for (String key : keys) {
                encoder.printProperty(key, properties.get(key));
            }
        }

        if (!dataMaps.isEmpty()) {
            if (breakNeeded) {
                encoder.println();
            }
            else {
                breakNeeded = true;
            }

            List<DataMap> maps = new ArrayList<DataMap>(this.dataMaps);
            Collections.sort(maps);

            for (DataMap dataMap : maps) {

                encoder.print("<map");
                encoder.printAttribute("name", dataMap.getName().trim());
                encoder.println("/>");
            }
        }

        if (!nodeDescriptors.isEmpty()) {
            if (breakNeeded) {
                encoder.println();
            }
            else {
                breakNeeded = true;
            }

            List<DataNodeDescriptor> nodes = new ArrayList<DataNodeDescriptor>(
                    nodeDescriptors);
            Collections.sort(nodes);
            encoder.print(nodes);
        }

        encoder.indent(-1);
        encoder.println("</domain>");
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

    public Collection<DataNodeDescriptor> getNodeDescriptors() {
        return nodeDescriptors;
    }

    public DataNodeDescriptor getNodeDescriptor(String name) {
        for (DataNodeDescriptor node : nodeDescriptors) {
            if (name.equals(node.getName())) {
                return node;
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

    /**
     * Returns the name of the DataNode that should be used as the default if a DataMap is
     * not explicitly linked to a node.
     */
    public String getDefaultNodeName() {
        return defaultNodeName;
    }

    public void setDefaultNodeName(String defaultDataNodeName) {
        this.defaultNodeName = defaultDataNodeName;
    }
}
