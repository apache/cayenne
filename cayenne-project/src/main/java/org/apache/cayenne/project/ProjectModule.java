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
package org.apache.cayenne.project;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.project.extension.ProjectExtension;
import org.apache.cayenne.project.upgrade.DefaultUpgradeService;
import org.apache.cayenne.project.upgrade.UpgradeService;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V10;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V11;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V12;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V7;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V8;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V9;
import org.apache.cayenne.project.validation.DefaultProjectValidator;
import org.apache.cayenne.project.validation.ProjectValidator;

/**
 * A dependency injection (DI) module contributing configuration related
 * to Cayenne mapping project manipulation to a DI container.
 *
 * @since 4.0
 */
public class ProjectModule implements Module {

    /**
     * @since 5.0
     */
    public static ProjectModuleExtender extend(Binder binder) {
        return new ProjectModuleExtender(binder);
    }

    /**
     * @since 4.1
     * @deprecated in favor of {@link #extend(Binder)}
     */
    @Deprecated(since = "5.0")
    public static ListBuilder<ProjectExtension> contributeExtensions(Binder binder) {
        return binder.bindList(ProjectExtension.class);
    }

    /**
     * @since 4.1
     * @deprecated in favor of {@link #extend(Binder)}
     */
    @Deprecated(since = "5.0")
    public static ListBuilder<UpgradeHandler> contributeUpgradeHandler(Binder binder) {
        return binder.bindList(UpgradeHandler.class);
    }

    public void configure(Binder binder) {
        binder.bind(ProjectLoader.class).to(DataChannelProjectLoader.class);
        binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
        binder.bind(ProjectValidator.class).to(DefaultProjectValidator.class);
        binder.bind(ConfigurationNodeParentGetter.class).to(DefaultConfigurationNodeParentGetter.class);
        binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);

        binder.bind(UpgradeService.class).to(DefaultUpgradeService.class);

        extend(binder)
                .initAllExtensions()

                // Note: order is important
                .addUpgradeHandler(UpgradeHandler_V7.class)
                .addUpgradeHandler(UpgradeHandler_V8.class)
                .addUpgradeHandler(UpgradeHandler_V9.class)
                .addUpgradeHandler(UpgradeHandler_V10.class)
                .addUpgradeHandler(UpgradeHandler_V11.class)
                .addUpgradeHandler(UpgradeHandler_V12.class);
    }
}
