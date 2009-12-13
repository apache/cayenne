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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;

/**
 * A descriptor of a DataChannel normally loaded from XML configuration.
 * 
 * @since 3.1
 */
public class DataChannelDescriptor implements ConfigurationNode {

    protected String name;
    protected String version;
    protected Map<String, String> properties;
    protected Collection<DataMap> dataMaps;
    protected Collection<DataNodeDescriptor> dataNodeDescriptors;
    protected Resource configurationSource;

    public DataChannelDescriptor() {
        properties = new HashMap<String, String>();
        dataMaps = new ArrayList<DataMap>(5);
        dataNodeDescriptors = new ArrayList<DataNodeDescriptor>(3);
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Collection<DataMap> getDataMaps() {
        return dataMaps;
    }

    public Collection<DataNodeDescriptor> getDataNodeDescriptors() {
        return dataNodeDescriptors;
    }

    public Resource getConfigurationSource() {
        return configurationSource;
    }

    public void setConfigurationSource(Resource configurationSource) {
        this.configurationSource = configurationSource;
    }
}
