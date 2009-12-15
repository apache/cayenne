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
package org.apache.cayenne.project2.upgrade.v6;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.project2.FileProjectSaver;
import org.apache.cayenne.project2.ProjectSaver;
import org.apache.cayenne.project2.unit.Project2Case;
import org.apache.cayenne.project2.upgrade.UpgradeHandler;
import org.apache.cayenne.project2.upgrade.UpgradeMetaData;
import org.apache.cayenne.project2.upgrade.UpgradeType;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.util.Util;

public class ProjectUpgrader_V6Test extends Project2Case {

    public void testMetadata_3_0_0_1() {

        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(
                baseUrl + "/3_0_0_1a/cayenne.xml");
        assertNotNull(url);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
                binder.bind(ConfigurationNameMapper.class).to(
                        DefaultConfigurationNameMapper.class);
            }
        };

        ProjectUpgrader_V6 upgrader = new ProjectUpgrader_V6();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(upgrader);

        Resource source = new URLResource(url);
        UpgradeHandler handler = upgrader.getUpgradeHandler(source);

        assertNotNull(handler);
        assertSame(source, handler.getProjectSource());

        UpgradeMetaData md = handler.getUpgradeMetaData();
        assertNotNull(md);

        assertSame(UpgradeType.UPGRADE_NEEDED, md.getUpgradeType());
        assertNull(md.getIntermediateUpgradeVersion());
        assertEquals("3.0.0.1", md.getProjectVersion());
        assertEquals("6", md.getSupportedVersion());
    }

    public void testMetadata_Type2_0() {
        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(baseUrl + "/2_0a/cayenne.xml");
        assertNotNull(url);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
                binder.bind(ConfigurationNameMapper.class).to(
                        DefaultConfigurationNameMapper.class);
            }
        };

        ProjectUpgrader_V6 upgrader = new ProjectUpgrader_V6();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(upgrader);

        Resource source = new URLResource(url);
        UpgradeHandler handler = upgrader.getUpgradeHandler(source);

        assertNotNull(handler);
        assertSame(source, handler.getProjectSource());

        UpgradeMetaData md = handler.getUpgradeMetaData();
        assertNotNull(md);
        assertSame(UpgradeType.INTERMEDIATE_UPGRADE_NEEDED, md.getUpgradeType());
        assertEquals("3.0.0.1", md.getIntermediateUpgradeVersion());
        assertEquals("2.0", md.getProjectVersion());
        assertEquals("6", md.getSupportedVersion());
    }

    public void testMetadata_Type6() {
        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(
                baseUrl + "/6a/cayenne-PROJECT1.xml");
        assertNotNull(url);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
                binder.bind(ConfigurationNameMapper.class).to(
                        DefaultConfigurationNameMapper.class);
            }
        };

        ProjectUpgrader_V6 upgrader = new ProjectUpgrader_V6();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(upgrader);

        Resource source = new URLResource(url);
        UpgradeHandler handler = upgrader.getUpgradeHandler(source);

        assertNotNull(handler);
        assertSame(source, handler.getProjectSource());

        UpgradeMetaData md = handler.getUpgradeMetaData();
        assertNotNull(md);
        assertSame(UpgradeType.UPGRADE_NOT_NEEDED, md.getUpgradeType());
        assertNull(md.getIntermediateUpgradeVersion());
        assertEquals("6", md.getProjectVersion());
        assertEquals("6", md.getSupportedVersion());
    }

    public void testPerformUpgrade_3_0_0_1() throws Exception {

        File testFolder = setupTestDirectory("testPerformUpgrade_3_0_0_1");
        String sourceUrl = getClass().getPackage().getName().replace('.', '/')
                + "/3_0_0_1a/";

        List<String> sources = new ArrayList<String>();

        sources.add("cayenne.xml");
        sources.add("d1Map1.map.xml");
        sources.add("d1Map2.map.xml");
        sources.add("d1NodeDriver.driver.xml");

        // upgrades are done in-place, so copy it first
        List<File> targetsBefore = new ArrayList<File>();
        for (String source : sources) {

            URL url = getClass().getClassLoader().getResource(sourceUrl + source);
            File target = new File(testFolder, source);
            assertNotNull(source);
            Util.copy(url, target);
            targetsBefore.add(target);
        }

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
                binder.bind(ConfigurationNameMapper.class).to(
                        DefaultConfigurationNameMapper.class);
            }
        };

        ProjectUpgrader_V6 upgrader = new ProjectUpgrader_V6();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(upgrader);

        Resource source = new URLResource(targetsBefore.get(0).toURL());
        UpgradeHandler handler = upgrader.getUpgradeHandler(source);

        Resource upgraded = handler.performUpgrade();
        assertNotNull(upgraded);
        assertNotSame(source, upgrader);

        // check that all the new files are created...
        List<String> targets = new ArrayList<String>();

        targets.add("cayenne-d1.xml");
        targets.add("cayenne-d2.xml");
        targets.add("d1Map1.map.xml");
        targets.add("d1Map2.map.xml");
        for (String targetName : targets) {
            File target = new File(testFolder, targetName);
            assertTrue("File was not created: " + target.getAbsolutePath(), target
                    .exists());
        }

        // DataMap files should remain the same; all others need to be deleted
//        for (File file : targetsBefore) {
//            if (file.getName().endsWith(".map.xml")) {
//                assertTrue("DataMap file disappeared: " + file.getAbsolutePath(), file
//                        .exists());
//            }
//            else {
//                assertFalse(
//                        "File expected to be deleted: " + file.getAbsolutePath(),
//                        file.exists());
//            }
//        }
    }
}
