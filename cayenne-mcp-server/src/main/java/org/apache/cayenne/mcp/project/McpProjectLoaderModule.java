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

import org.apache.cayenne.project.ProjectLoader;
import org.apache.cayenne.project.DataMapLoader;
import org.apache.cayenne.project.upgrade.ProjectFileUpgrader;
import org.apache.cayenne.project.xml.ProjectMetaData;
import org.apache.cayenne.project.xml.DefaultProjectMetaData;
import org.apache.cayenne.project.xml.HandlerFactory;
import org.apache.cayenne.project.xml.XMLProjectLoader;
import org.apache.cayenne.project.xml.XMLDataMapLoader;
import org.apache.cayenne.project.xml.XMLReaderProvider;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.projecttools.extension.ExtensionAwareHandlerFactory;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;
import org.xml.sax.XMLReader;

/**
 * Wires the DI bindings needed to load a Cayenne project descriptor and read
 * its embedded extension metadata (e.g., cgen configuration).
 * Registered alongside {@link org.apache.cayenne.projecttools.ProjectToolsModule} and the
 * auto-loaded {@code CgenModule} when building the MCP tools injector.
 *
 * @since 5.0
 */
public class McpProjectLoaderModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(ProjectLoader.class).to(XMLProjectLoader.class);
        binder.bind(HandlerFactory.class).to(ExtensionAwareHandlerFactory.class);
        binder.bind(ProjectMetaData.class).to(DefaultProjectMetaData.class);
        binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
        binder.bind(ProjectFileUpgrader.class).to(ProjectFileUpgrader.class);
        binder.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class);
        binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();
    }
}
