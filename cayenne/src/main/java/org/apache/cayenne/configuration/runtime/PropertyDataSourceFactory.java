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
package org.apache.cayenne.configuration.runtime;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.datasource.CayenneDataSource;

import javax.sql.DataSource;

/**
 * A DataSourceFactory that creates a DataSource based on runtime properties. Properties can be set per domain/node
 * name or globally, applying to all nodes without explicit property set. The following properties are supported:
 * <ul>
 * <li>cayenne.jdbc.url[.domain_name.node_name]
 * <li>cayenne.jdbc.driver[.domain_name.node_name]
 * <li>cayenne.jdbc.username[.domain_name.node_name]
 * <li>cayenne.jdbc.password[.domain_name.node_name]
 * <li>cayenne.jdbc.min_connections[.domain_name.node_name]
 * <li>cayenne.jdbc.max_connections[.domain_name.node_name]
 * <li>cayenne.jdbc.max_wait[.domain_name.node_name]
 * <li>cayenne.jdbc.validation_query[.domain_name.node_name]
 * </ul>
 * The URL property is required. Pooling is enabled if at least one of the connections count properties is set.
 *
 * @since 3.1
 */
public class PropertyDataSourceFactory implements DataSourceFactory {

    @Inject
    protected RuntimeProperties properties;

    @Override
    public DataSource getDataSource(DataNodeDescriptor nodeDescriptor) {
        String nodeName = nodeDescriptor.getDataChannelDescriptor().getName() + "." + nodeDescriptor.getName();
        return CayenneDataSource.fromProperties(properties.toMap(), nodeName).build();
    }
}
