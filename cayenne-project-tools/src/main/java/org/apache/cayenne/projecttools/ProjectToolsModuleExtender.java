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

package org.apache.cayenne.projecttools;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.projecttools.extension.ProjectExtension;

/**
 * @since 5.0
 */
public class ProjectToolsModuleExtender {

    private final Binder binder;

    private ListBuilder<ProjectExtension> extensions;

    public ProjectToolsModuleExtender(Binder binder) {
        this.binder = binder;
    }

    protected ProjectToolsModuleExtender initAllExtensions() {
        contributeExtensions();
        return this;
    }

    public ProjectToolsModuleExtender addExtension(ProjectExtension extension) {
        contributeExtensions().add(extension);
        return this;
    }

    public ProjectToolsModuleExtender addExtension(Class<? extends ProjectExtension> extension) {
        contributeExtensions().add(extension);
        return this;
    }

    private ListBuilder<ProjectExtension> contributeExtensions() {
        if (extensions == null) {
            extensions = binder.bindList(ProjectExtension.class);
        }
        return extensions;
    }
}
