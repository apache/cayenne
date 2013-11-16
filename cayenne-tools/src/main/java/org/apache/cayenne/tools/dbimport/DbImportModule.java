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
package org.apache.cayenne.tools.dbimport;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.project.FileProjectSaver;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.tools.configuration.ToolsModule;

/**
 * A DI module that bootstraps {@link DbImportAction}. Should be used in
 * conjunction with {@link ToolsModule}.
 * 
 * @since 3.2
 */
public class DbImportModule implements Module {

    public void configure(Binder binder) {
        binder.bind(DbImportAction.class).to(DbImportAction.class);
        binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
        binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
    }
}
