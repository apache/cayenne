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

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import javax.sql.DataSource;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.ConfigStatus;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.conf.RuntimeLoadDelegate;
import org.apache.cayenne.conf.RuntimeSaveDelegate;
import org.apache.cayenne.dba.db2.DB2Adapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.util.Util;

public class ApplicationProjectUpgradeTest extends CayenneCase {

    public void testUpgradeFrom1_1() throws Exception {

        // copy files first as upgrade is done in-place
        File upgradeScratchDir = new File(getTestDir(), "upgrade/1.1");
        upgradeScratchDir.mkdirs();

        URL upgradeSrcUrl = CayenneResources.getResourceURL("upgrade/1.1");
        File upgradeSrcDir = new File(new URI(upgradeSrcUrl.toExternalForm()));
        File[] files = upgradeSrcDir.listFiles();
        for (File file : files) {
            Util.copy(file, new File(upgradeScratchDir, file.getName()));
        }

        File cayenneXml = new File(upgradeScratchDir, "cayenne.xml");
        ApplicationProject p = new ApplicationProject(
                cayenneXml,
                buildProjectConfiguration(cayenneXml));

        ApplicationUpgradeHandler handler = ApplicationUpgradeHandler.sharedHandler();
        assertEquals(Project.UPGRADE_STATUS_OLD, handler.checkForUpgrades(p
                .getConfiguration(), new ArrayList()));
        handler.performUpgrade(p);

        ApplicationProject p1 = new ApplicationProject(
                cayenneXml,
                buildProjectConfiguration(cayenneXml));
        assertEquals(Project.UPGRADE_STATUS_CURRENT, handler.checkForUpgrades(p1
                .getConfiguration(), new ArrayList()));

        DataDomain dd = p1.getConfiguration().getDomain("default");
        assertNotNull(dd);
        DataNode dn = dd.getNode("defaultNode");
        assertNotNull(dn);
        assertNotNull(dn.getAdapter());
        assertEquals(MySQLAdapter.class.getName(), dn.getAdapter().getClass().getName());
        assertEquals(DriverDataSourceFactory.class.getName(), dn.getDataSourceFactory());

        DataMap testmap = dd.getMap("testmap");
        assertNotNull(testmap);
        SQLTemplate query = (SQLTemplate) testmap.getQuery("NonSelectingQuery");
        assertNotNull(query);
        assertNotNull(query.getCustomTemplate(DB2Adapter.class.getName()));
        assertNull(query.getCustomTemplate("invalid"));
    }

    public void testUpgradeFrom1_2() throws Exception {

        // copy files first as upgrade is done in-place
        File upgradeScratchDir = new File(getTestDir(), "upgrade/1.2");
        upgradeScratchDir.mkdirs();

        URL upgradeSrcUrl = CayenneResources.getResourceURL("upgrade/1.2");
        File upgradeSrcDir = new File(new URI(upgradeSrcUrl.toExternalForm()));
        File[] files = upgradeSrcDir.listFiles();
        for (File file : files) {
            Util.copy(file, new File(upgradeScratchDir, file.getName()));
        }

        File cayenneXml = new File(upgradeScratchDir, "cayenne.xml");
        ApplicationProject p = new ApplicationProject(
                cayenneXml,
                buildProjectConfiguration(cayenneXml));

        ApplicationUpgradeHandler handler = ApplicationUpgradeHandler.sharedHandler();
        assertEquals(Project.UPGRADE_STATUS_OLD, handler.checkForUpgrades(p
                .getConfiguration(), new ArrayList()));
        handler.performUpgrade(p);

        ApplicationProject p1 = new ApplicationProject(
                cayenneXml,
                buildProjectConfiguration(cayenneXml));
        assertEquals(Project.UPGRADE_STATUS_CURRENT, handler.checkForUpgrades(p1
                .getConfiguration(), new ArrayList()));

        DataDomain dd = p1.getConfiguration().getDomain("default");
        assertNotNull(dd);
        DataNode dn = dd.getNode("defaultNode");
        assertNotNull(dn);
        assertNotNull(dn.getAdapter());
        assertEquals(MySQLAdapter.class.getName(), dn.getAdapter().getClass().getName());
        assertEquals(DriverDataSourceFactory.class.getName(), dn.getDataSourceFactory());

        DataMap testmap = dd.getMap("testmap");
        assertNotNull(testmap);
        SQLTemplate query = (SQLTemplate) testmap.getQuery("NonSelectingQuery");
        assertNotNull(query);
        assertNotNull(query.getCustomTemplate(DB2Adapter.class.getName()));
        assertNull(query.getCustomTemplate("invalid"));
    }

    // coped from the Modeler ProjectAction.
    protected Configuration buildProjectConfiguration(File projectFile) {
        ProjectConfiguration config = new ProjectConfiguration(projectFile);
        config.setLoaderDelegate(new RuntimeLoadDelegate(config, new ConfigStatus()) {

            protected void updateDefaults(DataDomain domain) {
                // do nothing...
            }

            @Override
            protected DataNode createDataNode(String nodeName) {
                return new DataNode(nodeName) {

                    @Override
                    public DataSource getDataSource() {
                        return dataSource;
                    }
                };
            }

        });
        config.setSaverDelegate(new RuntimeSaveDelegate(config));
        return config;
    }
}
