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

package org.apache.cayenne.project.compatibility;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.xml.DefaultHandlerFactory;
import org.apache.cayenne.configuration.xml.HandlerFactory;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.upgrade.UpgradeService;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V10;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V11;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V12;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V7;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V8;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V9;

import static org.mockito.Mockito.mock;

/**
 * @since 4.1
 */
public class CompatibilityTestModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(UpgradeService.class).to(CompatibilityUpgradeService.class);
        binder.bind(DocumentProvider.class).to(DefaultDocumentProvider.class);

        binder.bind(HandlerFactory.class).to(DefaultHandlerFactory.class);
        binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
        binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);

        binder.bindList(UpgradeHandler.class)
                .add(UpgradeHandler_V7.class)
                .add(UpgradeHandler_V8.class)
                .add(UpgradeHandler_V9.class)
                .add(UpgradeHandler_V10.class)
                .add(UpgradeHandler_V11.class)
                .add(UpgradeHandler_V12.class);

        binder.bind(ProjectSaver.class).toInstance(mock(ProjectSaver.class));
        binder.bind(DataChannelDescriptorLoader.class).toInstance(mock(DataChannelDescriptorLoader.class));
    }
}
