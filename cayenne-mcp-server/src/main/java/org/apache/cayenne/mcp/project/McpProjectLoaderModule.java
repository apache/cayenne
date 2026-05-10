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
package org.apache.cayenne.mcp.project;

import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.DefaultDataChannelMetaData;
import org.apache.cayenne.configuration.xml.HandlerFactory;
import org.apache.cayenne.configuration.xml.XMLDataChannelDescriptorLoader;
import org.apache.cayenne.configuration.xml.XMLDataMapLoader;
import org.apache.cayenne.configuration.xml.XMLReaderProvider;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.project.extension.ExtensionAwareHandlerFactory;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;
import org.xml.sax.XMLReader;

/**
 * Wires the DI bindings needed to load a Cayenne project descriptor and read
 * its embedded extension metadata (e.g., cgen configuration).
 * Registered alongside {@link org.apache.cayenne.project.ProjectModule} and the
 * auto-loaded {@code CgenModule} when building the MCP tools injector.
 *
 * @since 5.0
 */
public class McpProjectLoaderModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(DataChannelDescriptorLoader.class).to(XMLDataChannelDescriptorLoader.class);
        binder.bind(HandlerFactory.class).to(ExtensionAwareHandlerFactory.class);
        binder.bind(DataChannelMetaData.class).to(DefaultDataChannelMetaData.class);
        binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
        binder.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class);
        binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();
    }
}
