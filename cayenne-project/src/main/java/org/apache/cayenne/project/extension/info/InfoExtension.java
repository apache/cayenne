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

package org.apache.cayenne.project.extension.info;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.ProjectVersion;
import org.apache.cayenne.configuration.xml.Schema;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.project.extension.BaseNamingDelegate;
import org.apache.cayenne.project.extension.LoaderDelegate;
import org.apache.cayenne.project.extension.ProjectExtension;
import org.apache.cayenne.project.extension.SaverDelegate;

/**
 * Extension that provides additional properties for project entities.
 * It stores data in {@link ObjectInfo} associated with objects via {@link DataChannelMetaData}.
 * Currently used by Modeler and cgen tools to provide user comments.
 *
 * @since 4.1
 */
public class InfoExtension implements ProjectExtension {

    static final String NAMESPACE = Schema.buildNamespace(ProjectVersion.getCurrent(), "info");

    @Inject
    private DataChannelMetaData metaData;

    @Override
    public LoaderDelegate createLoaderDelegate() {
        return new InfoLoaderDelegate(metaData);
    }

    @Override
    public SaverDelegate createSaverDelegate() {
        return new InfoSaverDelegate(metaData);
    }

    @Override
    public ConfigurationNodeVisitor<String> createNamingDelegate() {
        return new BaseNamingDelegate();
    }
}
