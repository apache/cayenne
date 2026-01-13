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

package org.apache.cayenne.modeler.graph.extension;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.xml.ProjectVersion;
import org.apache.cayenne.configuration.xml.Schema;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.graph.GraphRegistry;
import org.apache.cayenne.project.extension.BaseNamingDelegate;
import org.apache.cayenne.project.extension.LoaderDelegate;
import org.apache.cayenne.project.extension.ProjectExtension;
import org.apache.cayenne.project.extension.SaverDelegate;

/**
 * @since 4.1
 */
public class GraphExtension implements ProjectExtension {

    static final String NAMESPACE = Schema.buildNamespace(ProjectVersion.getCurrent(), "graph");

    static final String GRAPH_SUFFIX = ".graph.xml";

    @Inject
    protected Provider<Application> applicationProvider;

    @Override
    public LoaderDelegate createLoaderDelegate() {
        return new GraphLoaderDelegate(applicationProvider.get());
    }

    @Override
    public SaverDelegate createSaverDelegate() {
        return new GraphSaverDelegate(applicationProvider.get().getMetaData());
    }

    @Override
    public ConfigurationNodeVisitor<String> createNamingDelegate() {
        return new BaseNamingDelegate() {
            @Override
            public String visitDataChannelDescriptor(DataChannelDescriptor channelDescriptor) {
                // if there is no registry, than there is no need to save anything
                GraphRegistry registry = applicationProvider.get().getMetaData()
                        .get(channelDescriptor, GraphRegistry.class);
                if (registry == null) {
                    return null;
                }
                return channelDescriptor.getName() + GRAPH_SUFFIX;
            }
        };
    }
}
