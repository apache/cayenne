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
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A descriptor of a {@link DataNode}.
 * 
 * @since 3.1
 */
public class DataNodeDescriptor implements XMLSerializable {

    protected DataChannelDescriptor parent;
    protected String name;
    protected Collection<String> dataMapNames;

    protected String parameters;
    protected String adapterType;
    protected String dataSourceFactoryType;
    protected String schemaUpdateStrategyType;
    protected DataSourceInfo dataSourceDescriptor;

    public DataNodeDescriptor() {
        this(null);
    }

    public DataNodeDescriptor(DataChannelDescriptor parent) {
        this.parent = parent;
        this.dataMapNames = new ArrayList<String>();
    }

    public void encodeAsXML(XMLEncoder encoder) {
        throw new UnsupportedOperationException("TODO");
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

    /**
     * Returns extra DataNodeDescriptor parameters. This property is often used by custom
     * {@link DataSourceFactory} to configure a DataSource. E.g. JNDIDataSoirceFactory may
     * treat parameters String as a JNDI location of the DataSource, etc.
     */
    public String getParameters() {
        return parameters;
    }

    /**
     * Sets extra DataNodeDescriptor parameters. This property is often used by custom
     * {@link DataSourceFactory} to configure a DataSource. E.g. JNDIDataSoirceFactory may
     * treat parameters String as a JNDI location of the DataSource, etc.
     */
    public void setParameters(String location) {
        this.parameters = location;
    }

    public String getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(String adapter) {
        this.adapterType = adapter;
    }

    public String getDataSourceFactoryType() {
        return dataSourceFactoryType;
    }

    public void setDataSourceFactoryType(String dataSourceFactory) {
        this.dataSourceFactoryType = dataSourceFactory;
    }

    public String getSchemaUpdateStrategyType() {
        return schemaUpdateStrategyType;
    }

    public void setSchemaUpdateStrategyType(String schemaUpdateStrategyClass) {
        this.schemaUpdateStrategyType = schemaUpdateStrategyClass;
    }

    public DataSourceInfo getDataSourceDescriptor() {
        return dataSourceDescriptor;
    }

    public void setDataSourceDescriptor(DataSourceInfo dataSourceDescriptor) {
        this.dataSourceDescriptor = dataSourceDescriptor;
    }

    public DataChannelDescriptor getParent() {
        return parent;
    }

    public void setParent(DataChannelDescriptor parent) {
        this.parent = parent;
    }
}
