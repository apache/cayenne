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

import java.net.URL;

import junit.framework.TestCase;

import org.apache.cayenne.project2.upgrade.UpgradeHandler;
import org.apache.cayenne.project2.upgrade.UpgradeMetaData;
import org.apache.cayenne.project2.upgrade.UpgradeType;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;

public class ProjectUpgrader_V6Test extends TestCase {

    public void testMetadata_3_0_0_1() {

        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(
                baseUrl + "/3_0_0_1a/cayenne.xml");
        assertNotNull(url);

        ProjectUpgrader_V6 upgrader = new ProjectUpgrader_V6();

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

        ProjectUpgrader_V6 upgrader = new ProjectUpgrader_V6();

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

        ProjectUpgrader_V6 upgrader = new ProjectUpgrader_V6();

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
}
