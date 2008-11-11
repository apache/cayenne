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

import java.sql.Connection;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.util.ResourceLocator;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 */
public class DBCPDataSourcePropertiesTest extends TestCase {

    public void testLoadProperties() throws Exception {
        ResourceLocator locator = new ResourceLocator();
        locator.setSkipClasspath(false);

        Properties props1 = DBCPDataSourceProperties.loadProperties(locator, "dbcp");
        assertNotNull(props1);
        assertEquals("a", props1.get("b"));
        
        Properties props2 = DBCPDataSourceProperties.loadProperties(locator, "dbcp.properties");
        assertNotNull(props2);
        assertEquals("a", props2.get("b"));
        
        Properties props3 = DBCPDataSourceProperties.loadProperties(locator, "dbcp.driver");
        assertNotNull(props3);
        assertEquals("d", props3.get("c"));
        
        Properties props4 = DBCPDataSourceProperties.loadProperties(locator, "dbcp.driver.properties");
        assertNotNull(props4);
        assertEquals("d", props4.get("c"));
    }

    public void testStringProperty() {
        Properties props = new Properties();
        props.put("a", "X");
        props.put("cayenne.dbcp.c", "Y");
        DBCPDataSourceProperties factory = new DBCPDataSourceProperties(props);

        assertNull(factory.getString("a"));
        assertNull(factory.getString("b"));
        assertEquals("Y", factory.getString("c"));
    }

    public void testIntProperty() {

        Properties props = new Properties();
        props.put("a", "10");
        props.put("cayenne.dbcp.b", "11");
        props.put("cayenne.dbcp.d", "**");
        DBCPDataSourceProperties factory = new DBCPDataSourceProperties(props);

        assertEquals(11, factory.getInt("b", -1));
        assertEquals(-1, factory.getInt("a", -1));
        assertEquals(-1, factory.getInt("c", -1));
        assertEquals(-2, factory.getInt("d", -2));
    }

    public void testWhenExhaustedAction() throws Exception {
        Properties props = new Properties();
        props.put("cayenne.dbcp.a", "1");
        props.put("cayenne.dbcp.b", "WHEN_EXHAUSTED_BLOCK");
        props.put("cayenne.dbcp.c", "WHEN_EXHAUSTED_GROW");
        props.put("cayenne.dbcp.d", "WHEN_EXHAUSTED_FAIL");
        props.put("cayenne.dbcp.e", "garbage");
        DBCPDataSourceProperties factory = new DBCPDataSourceProperties(props);

        assertEquals(1, factory.getWhenExhaustedAction("a", (byte) 100));
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_BLOCK, factory
                .getWhenExhaustedAction("b", (byte) 100));
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_GROW, factory
                .getWhenExhaustedAction("c", (byte) 100));
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_FAIL, factory
                .getWhenExhaustedAction("d", (byte) 100));

        try {
            factory.getWhenExhaustedAction("e", (byte) 100);
            fail("must throw on invalid key");
        }
        catch (ConfigurationException ex) {
            // expected
        }

        assertEquals(100, factory.getWhenExhaustedAction("f", (byte) 100));
    }

    public void testTransactionIsolation() throws Exception {
        Properties props = new Properties();
        props.put("cayenne.dbcp.a", "1");
        props.put("cayenne.dbcp.b", "TRANSACTION_NONE");
        props.put("cayenne.dbcp.c", "TRANSACTION_READ_UNCOMMITTED");
        props.put("cayenne.dbcp.d", "TRANSACTION_SERIALIZABLE");
        props.put("cayenne.dbcp.e", "garbage");
        DBCPDataSourceProperties factory = new DBCPDataSourceProperties(props);
        assertEquals(1, factory.getTransactionIsolation("a", (byte) 100));
        assertEquals(Connection.TRANSACTION_NONE, factory.getTransactionIsolation(
                "b",
                (byte) 100));

        assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, factory
                .getTransactionIsolation("c", (byte) 100));
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, factory
                .getTransactionIsolation("d", (byte) 100));

        try {
            factory.getTransactionIsolation("e", (byte) 100);
            fail("must throw on invalid key");
        }
        catch (ConfigurationException ex) {
            // expected
        }

        assertEquals(100, factory.getTransactionIsolation("f", (byte) 100));
    }
}
