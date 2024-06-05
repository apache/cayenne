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
package org.apache.cayenne.modeler.init;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.DefaultDataChannelMetaData;
import org.apache.cayenne.configuration.xml.HandlerFactory;
import org.apache.cayenne.configuration.xml.XMLReaderProvider;
import org.apache.cayenne.dbsync.xml.DbImportExtension;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.gen.xml.CgenExtension;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.DefaultActionManager;
import org.apache.cayenne.modeler.graph.extension.GraphExtension;
import org.apache.cayenne.modeler.init.platform.GenericPlatformInitializer;
import org.apache.cayenne.modeler.init.platform.PlatformInitializer;
import org.apache.cayenne.modeler.util.DefaultWidgetFactory;
import org.apache.cayenne.modeler.util.WidgetFactory;
import org.apache.cayenne.modeler.validation.ConfigurableProjectValidator;
import org.apache.cayenne.modeler.validation.extenstion.ValidationExtension;
import org.apache.cayenne.project.ProjectModule;
import org.apache.cayenne.project.extension.ExtensionAwareHandlerFactory;
import org.apache.cayenne.project.extension.info.InfoExtension;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.xml.sax.XMLReader;

/**
 * A DI module for bootstrapping CayenneModeler services.
 */
public class CayenneModelerModule implements Module {

    public void configure(Binder binder) {

        binder.bind(ActionManager.class).to(DefaultActionManager.class);
        binder.bind(Application.class).to(Application.class);
        binder.bind(PlatformInitializer.class).to(GenericPlatformInitializer.class);
        binder.bind(WidgetFactory.class).to(DefaultWidgetFactory.class);
        binder.bind(HandlerFactory.class).to(ExtensionAwareHandlerFactory.class);
        binder.bind(DataChannelMetaData.class).to(DefaultDataChannelMetaData.class);
        binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(true)).withoutScope();

        ProjectModule.extend(binder)
                .addExtension(InfoExtension.class)
                .addExtension(GraphExtension.class)
                .addExtension(DbImportExtension.class)
                .addExtension(CgenExtension.class)
                .addExtension(ValidationExtension.class);

        binder.bind(ProjectValidator.class).to(ConfigurableProjectValidator.class);
    }
}
