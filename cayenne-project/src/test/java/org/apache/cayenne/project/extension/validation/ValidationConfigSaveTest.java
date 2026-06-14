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
package org.apache.cayenne.project.extension.validation;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.DefaultDataChannelMetaData;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.project.FileProjectSaver;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.extension.ProjectExtension;
import org.apache.cayenne.project.validation.Inspection;
import org.apache.cayenne.project.validation.ValidationConfig;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidationConfigSaveTest {

    @TempDir
    public File tempDir;

    @Test
    @SuppressWarnings("removal") // intentionally references a deprecated inspection
    public void deprecatedInspectionStrippedOnSave() throws Exception {
        Module testModule = binder -> {
            binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            binder.bind(DataChannelMetaData.class).to(DefaultDataChannelMetaData.class);
        };
        Injector injector = DIBootstrap.createInjector(testModule);

        DataChannelMetaData metaData = injector.getInstance(DataChannelMetaData.class);
        ValidationExtension extension = new ValidationExtension();
        injector.injectMembers(extension);

        FileProjectSaver saver = new FileProjectSaver(List.<ProjectExtension>of(extension));
        injector.injectMembers(saver);

        DataChannelDescriptor descriptor = new DataChannelDescriptor();
        descriptor.setName("deprecation");

        // disable one deprecated inspection and one regular inspection
        EnumSet<Inspection> enabled = EnumSet.complementOf(EnumSet.of(
                Inspection.DB_ATTRIBUTE_NO_LENGTH,
                Inspection.DATA_NODE_NO_NAME));
        metaData.add(descriptor, new ValidationConfig(enabled));

        Project project = new Project(new ConfigurationTree<>(descriptor));
        saver.saveAs(project, new URLResource(tempDir.toURI().toURL()));

        String xml = Files.readString(new File(tempDir, "cayenne-deprecation.xml").toPath());

        // the deprecated inspection is dropped, the regular one is round-tripped
        assertFalse(xml.contains("DB_ATTRIBUTE_NO_LENGTH"), xml);
        assertTrue(xml.contains("DATA_NODE_NO_NAME"), xml);
    }
}
