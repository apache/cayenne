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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.resource.Resource;

/**
 * A descriptor of a {@link DataNode}, normally loaded from XML.
 * 
 * @since 3.1
 */
public class DataNodeDescriptor {

    protected String name;
    protected Collection<String> dataMapNames;

    protected String location;
    protected String adapterClass;
    protected String dataSourceFactoryClass;
    protected String schemaUpdateStrategyClass;

    protected Resource configurationResource;

    public DataNodeDescriptor() {
        dataMapNames = new ArrayList<String>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<String> getDataMapNames() {
        return dataMapNames;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAdapterClass() {
        return adapterClass;
    }

    public void setAdapterClass(String adapter) {
        this.adapterClass = adapter;
    }

    public String getDataSourceFactoryClass() {
        return dataSourceFactoryClass;
    }

    public void setDataSourceFactoryClass(String dataSourceFactory) {
        this.dataSourceFactoryClass = dataSourceFactory;
    }

    public Resource getConfigurationResource() {
        return configurationResource;
    }

    public void setConfigurationResource(Resource descriptorResource) {
        this.configurationResource = descriptorResource;
    }

    public String getSchemaUpdateStrategyClass() {
        return schemaUpdateStrategyClass;
    }

    public void setSchemaUpdateStrategyClass(String schemaUpdateStrategyClass) {
        this.schemaUpdateStrategyClass = schemaUpdateStrategyClass;
    }
}
