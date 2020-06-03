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
package org.apache.cayenne.gen;

import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.gen.property.DatePropertyDescriptorCreator;
import org.apache.cayenne.gen.property.EmbeddablePropertyDescriptorCreator;
import org.apache.cayenne.gen.property.NumericPropertyDescriptorCreator;
import org.apache.cayenne.gen.property.PropertyDescriptorCreator;
import org.apache.cayenne.gen.property.StringPropertyDescriptorCreator;
import org.apache.cayenne.gen.xml.CgenExtension;
import org.apache.cayenne.project.ProjectModule;
import org.apache.cayenne.project.extension.info.InfoExtension;

/**
 * @since 4.1
 */
public class CgenModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
        binder.bind(ClassGenerationActionFactory.class).to(DefaultClassGenerationActionFactory.class);
        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
        binder.bind(ToolsUtilsFactory.class).to(DefaultToolsUtilsFactory.class);
        binder.bind(MetadataUtils.class).to(MetadataUtils.class);

        ProjectModule.contributeExtensions(binder)
                .add(CgenExtension.class)
                .add(InfoExtension.class); // info extension needed to get comments and other metadata

        contributeUserProperties(binder)
                .add(NumericPropertyDescriptorCreator.class)
                .add(DatePropertyDescriptorCreator.class)
                .add(StringPropertyDescriptorCreator.class)
                .add(EmbeddablePropertyDescriptorCreator.class);
    }

    public static ListBuilder<PropertyDescriptorCreator> contributeUserProperties(Binder binder) {
        return binder.bindList(PropertyDescriptorCreator.class);
    }
}
