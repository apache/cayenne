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

import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class ApplicationUpgradeHandlerTest extends CayenneCase {

    public void testCreateUpgradeHandler() throws Exception {
        ApplicationUpgradeHandler handler = ApplicationUpgradeHandler.sharedHandler();
        assertEquals(Project.CURRENT_PROJECT_VERSION, handler.supportedVersion());
    }

    public void testDecodeVersion() throws Exception {
        assertEquals(1.1, ApplicationUpgradeHandler.decodeVersion("1.1"), 0.000001);
        assertEquals(1.12, ApplicationUpgradeHandler.decodeVersion("1.1.2"), 0.000001);
        assertEquals(1.123, ApplicationUpgradeHandler.decodeVersion("1.1.2.3"), 0.000001);
    }

    public void testCompareVersion() throws Exception {
        ApplicationUpgradeHandler handler = ApplicationUpgradeHandler.sharedHandler();
        assertEquals(1.1, ApplicationUpgradeHandler.decodeVersion("1.1"), 0.000001);
        assertEquals(-1, handler.compareVersion("5.0"));
        assertEquals(0, handler.compareVersion(Project.CURRENT_PROJECT_VERSION));
        assertEquals(1, handler.compareVersion("1.2"));
        assertEquals(1, handler.compareVersion("1.0.1"));
        assertEquals(1, handler.compareVersion("1.0"));
    }
}
