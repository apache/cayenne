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

import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.xml.CompatibilityDataChannelDescriptorLoader;
import org.apache.cayenne.configuration.xml.CompatibilityDataMapLoader;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.project.ProjectModule;
import org.apache.cayenne.project.upgrade.UpgradeService;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V10;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V7;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V8;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V9;

/**
 * @since 4.1
 */
public class ProjectCompatibilityModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(DataChannelDescriptorLoader.class).to(CompatibilityDataChannelDescriptorLoader.class);
        binder.bind(DataMapLoader.class).to(CompatibilityDataMapLoader.class);

        binder.bind(UpgradeService.class).to(CompatibilityUpgradeService.class);
        binder.bind(DocumentProvider.class).to(DefaultDocumentProvider.class);

        ProjectModule.contributeUpgradeHandler(binder)
                .add(UpgradeHandler_V7.class)
                .add(UpgradeHandler_V8.class)
                .add(UpgradeHandler_V9.class)
                .add(UpgradeHandler_V10.class);
    }
}
