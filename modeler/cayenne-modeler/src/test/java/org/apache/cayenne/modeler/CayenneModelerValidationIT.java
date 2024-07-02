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
package org.apache.cayenne.modeler;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.modeler.init.CayenneModelerModule;
import org.apache.cayenne.modeler.validation.ConfigurableProjectValidator;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectLoader;
import org.apache.cayenne.project.ProjectModule;
import org.apache.cayenne.project.validation.Inspection;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.apache.cayenne.project.validation.ValidationConfig;
import org.apache.cayenne.resource.URLResource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CayenneModelerValidationIT {

    public static final String CAYENNE_CONFIGURED_VALIDATION_PROJECT = "cayenne-configured-validation.xml";

    private final static Logger logger = LoggerFactory.getLogger(CayenneModelerValidationIT.class);
    private static Injector injector;

    @BeforeClass
    public static void setUp() {
        injector = DIBootstrap.createInjector(List.of(
                new ToolsModule(logger),
                new ProjectModule(),
                new CayenneModelerModule()
        ));
    }

    @Test
    public void validatorProvided() {
        ProjectValidator projectValidator = injector.getInstance(ProjectValidator.class);
        assertTrue(projectValidator instanceof ConfigurableProjectValidator);
    }

    @Test
    public void configLoaded() {
        URLResource projectResource = new URLResource(getClass().getResource(CAYENNE_CONFIGURED_VALIDATION_PROJECT));
        Application application = injector.getInstance(Application.class);
        ProjectLoader projectLoader = injector.getInstance(ProjectLoader.class);
        Project project = projectLoader.loadProject(projectResource);

        DataChannelDescriptor dataChannel = (DataChannelDescriptor) project.getRootNode();
        ValidationConfig config = ValidationConfig.fromMetadata(application.getMetaData(), dataChannel);

        assertNotNull(config);
        assertEquals(EnumSet.complementOf(EnumSet.of(
                Inspection.DATA_CHANNEL_NO_NAME,
                Inspection.DATA_NODE_NO_NAME,
                Inspection.DATA_NODE_NAME_DUPLICATE,
                Inspection.DATA_NODE_CONNECTION_PARAMS
        )), config.getEnabledInspections());
    }
}
