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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.Inject;

/**
 * A {@link DataSourceFactoryLoader} that loads factories explicitly configured in the
 * {@link DataNodeDescriptor}. If the factory class is not explicitly configured, and the
 * descriptor has a configuration resource attached to it,
 * {@link XMLPoolingDataSourceFactory} is returned.
 * 
 * @since 3.1
 */
public class DefaultDataSourceFactoryLoader implements DataSourceFactoryLoader {

    @Inject
    protected AdhocObjectFactory objectFactory;

    public DataSourceFactory getDataSourceFactory(DataNodeDescriptor nodeDescriptor) {
        String typeName = nodeDescriptor.getDataSourceFactoryType();

        if (typeName == null) {
            if (nodeDescriptor.getDataSourceDescriptor() != null) {
                return objectFactory.newInstance(
                        DataSourceFactory.class,
                        XMLPoolingDataSourceFactory.class.getName());
            }
            else {
                throw new CayenneRuntimeException(
                        "DataNodeDescriptor '%s' has null 'dataSourceFactoryType' and 'dataSourceDescriptor' properties",
                        nodeDescriptor.getName());
            }
        }

        return objectFactory.newInstance(DataSourceFactory.class, typeName);
    }
}
