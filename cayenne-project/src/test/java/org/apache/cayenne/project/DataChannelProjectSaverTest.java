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
package org.apache.cayenne.project;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.DefaultHandlerFactory;
import org.apache.cayenne.configuration.xml.HandlerFactory;
import org.apache.cayenne.configuration.xml.NoopDataChannelMetaData;
import org.apache.cayenne.configuration.xml.XMLDataChannelDescriptorLoader;
import org.apache.cayenne.configuration.xml.XMLDataMapLoader;
import org.apache.cayenne.configuration.xml.XMLReaderProvider;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.project.extension.ProjectExtension;
import org.apache.cayenne.project.unit.Project2Case;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.junit.Test;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collections;

import static org.junit.Assert.*;

public class DataChannelProjectSaverTest extends Project2Case {

    @Test
    public void testSaveAs() throws Exception {

        FileProjectSaver saver = new FileProjectSaver(Collections.<ProjectExtension>emptyList());

        Module testModule = binder -> {
            binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);

            binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
            binder.bind(DataChannelDescriptorLoader.class).to(XMLDataChannelDescriptorLoader.class);
            binder.bind(ProjectLoader.class).to(DataChannelProjectLoader.class);
            binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            binder.bind(HandlerFactory.class).to(DefaultHandlerFactory.class);
            binder.bind(DataChannelMetaData.class).to(NoopDataChannelMetaData.class);
            binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();
        };

        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(saver);

        String testConfigName = "PROJECT2";
        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(
                baseUrl + "/cayenne-" + testConfigName + ".xml");

        Resource source = new URLResource(url);
        Project project = injector.getInstance(ProjectLoader.class).loadProject(source);

        File outFile = setupTestDirectory("testSave");

        saver.saveAs(project, new URLResource(outFile.toURI().toURL()));

        File rootFile = new File(outFile, "cayenne-PROJECT2.xml");
        assertTrue(rootFile.exists());
        assertTrue(rootFile.length() > 0);

        File map1File = new File(outFile, "testProjectMap2_1.map.xml");
        assertTrue(map1File.exists());
        assertTrue(map1File.length() > 0);

        File map2File = new File(outFile, "testProjectMap2_2.map.xml");
        assertTrue(map2File.exists());
        assertTrue(map2File.length() > 0);
    }

    @Test
    public void testSaveAs_RecoverFromSaveError() throws Exception {

        FileProjectSaver saver = new FileProjectSaver(Collections.<ProjectExtension>emptyList()) {

            @Override
            void saveToTempFile(SaveUnit unit, PrintWriter printWriter) {
                throw new CayenneRuntimeException("Test Exception");
            }
        };

        Module testModule = binder -> {
            binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
            binder.bind(DataChannelDescriptorLoader.class).to(XMLDataChannelDescriptorLoader.class);
            binder.bind(ProjectLoader.class).to(DataChannelProjectLoader.class);
            binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            binder.bind(HandlerFactory.class).to(DefaultHandlerFactory.class);
            binder.bind(DataChannelMetaData.class).to(NoopDataChannelMetaData.class);
            binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();
        };

        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(saver);

        String testConfigName = "PROJECT2";
        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(
                baseUrl + "/cayenne-" + testConfigName + ".xml");

        Resource source = new URLResource(url);
        Project project = injector.getInstance(ProjectLoader.class).loadProject(source);

        File outFile = setupTestDirectory("testSaveAs_RecoverFromSaveError");
        assertEquals(0, outFile.list().length);

        try {
            saver.saveAs(project, new URLResource(outFile.toURI().toURL()));
            fail("No exception was thrown..");
        }
        catch (CayenneRuntimeException e) {
            // expected

            assertEquals(0, outFile.list().length);
        }

    }

}
