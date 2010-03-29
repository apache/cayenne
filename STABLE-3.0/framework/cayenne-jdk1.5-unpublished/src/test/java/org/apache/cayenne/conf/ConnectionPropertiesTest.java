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

package org.apache.cayenne.conf;

import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.cayenne.conn.DataSourceInfo;

/**
 */
public class ConnectionPropertiesTest extends TestCase {

    public void testBuildDataSourceInfo() throws Exception {
        ConnectionProperties ps = new ConnectionProperties(new ExtendedProperties());

        ExtendedProperties props = new ExtendedProperties();
        props.setProperty(ConnectionProperties.ADAPTER_KEY, "1");
        props.setProperty(ConnectionProperties.DRIVER_KEY, "2");
        props.setProperty(ConnectionProperties.PASSWORD_KEY, "3");
        props.setProperty(ConnectionProperties.URL_KEY, "4");
        props.setProperty(ConnectionProperties.USER_NAME_KEY, "5");

        DataSourceInfo dsi = ps.buildDataSourceInfo(props);

        assertEquals("1", dsi.getAdapterClassName());
        assertEquals("2", dsi.getJdbcDriver());
        assertEquals("3", dsi.getPassword());
        assertEquals("4", dsi.getDataSourceUrl());
        assertEquals("5", dsi.getUserName());
    }

    public void testExtractNames() throws Exception {
        ConnectionProperties ps = new ConnectionProperties(new ExtendedProperties());

        ExtendedProperties props = new ExtendedProperties();
        props.setProperty("a.1", "a");
        props.setProperty("a.2", "a");
        props.setProperty("b.3", "a");
        props.setProperty("c.4", "a");

        List names = ps.extractNames(props);
        assertNotNull(names);
        assertEquals(3, names.size());
        assertTrue(names.contains("a"));
        assertTrue(names.contains("b"));
        assertTrue(names.contains("c"));
    }
}
