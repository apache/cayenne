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
package org.apache.cayenne.runtime;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.DIException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DataMap;

/**
 * A {@link DataChannel} provider that provides a single instance of DataDomain configured
 * per configuration supplied via injected {@link DataChannelDescriptorLoader}.
 * 
 * @since 3.1
 */
public class DataDomainProvider implements Provider<DataChannel> {

    @Inject
    protected DataChannelDescriptorLoader loader;

    @Inject
    protected RuntimeProperties configurationProperties;

    protected volatile DataChannel dataChannel;

    public DataChannel get() throws DIException {

        if (dataChannel == null) {
            synchronized (this) {
                if (dataChannel == null) {
                    createDataChannel();
                }
            }
        }

        return dataChannel;
    }

    protected void createDataChannel() {
        String runtimeName = configurationProperties
                .get(RuntimeProperties.CAYENNE_RUNTIME_NAME);
        DataChannelDescriptor descriptor = loader.load(runtimeName);

        DataDomain dataChannel = new DataDomain(descriptor.getName());

        dataChannel.initWithProperties(descriptor.getProperties());

        for (DataMap dataMap : descriptor.getDataMaps()) {
            dataChannel.addMap(dataMap);
        }

        for (DataNodeDescriptor dataNodeDescriptor : descriptor.getDataNodeDescriptors()) {
            // TODO: load data nodes
        }

        this.dataChannel = dataChannel;
    }
}
