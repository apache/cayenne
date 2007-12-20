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

package org.apache.cayenne.jpa.conf;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.spi.PersistenceUnitTransactionType;

import junit.framework.TestCase;

import org.apache.cayenne.jpa.JpaUnit;
import org.apache.cayenne.jpa.Provider;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public class UnitDescriptorParserTest extends TestCase {

    public void testSample1() throws Exception {
        UnitDescriptorParser parser = new UnitDescriptorParser();

        URL root = new URL("file:///xyz");
        InputStream in = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("xml-samples/persistence1.xml");
        Collection<JpaUnit> units = parser.getPersistenceUnits(new InputSource(in), root);

        assertEquals(1, units.size());

        JpaUnit info = (JpaUnit) units.iterator().next();

        assertEquals("OrderManagement", info.getPersistenceUnitName());

        // JTA is the default when type is ommitted
        assertEquals(PersistenceUnitTransactionType.JTA, info.getTransactionType());
        assertEquals(root, info.getPersistenceUnitRootUrl());
        assertTrue(info.getDescription().startsWith("This unit manages orders"));
        assertEquals("jdbc/MyOrderDB", info.getProperties().getProperty(
                Provider.JTA_DATA_SOURCE_PROPERTY));

        assertEquals(1, info.getMappingFileNames().size());
        assertTrue(info.getMappingFileNames().contains("ormap.xml"));

        assertEquals(2, info.getManagedClassNames().size());
        assertTrue(info.getManagedClassNames().contains("com.widgets.Order"));
        assertTrue(info.getManagedClassNames().contains("com.widgets.Customer"));
    }

    public void testSampleSchemaHeadersValidating() throws Exception {
        UnitDescriptorParser parser = new UnitDescriptorParser(true);

        URL root = new URL("file:///xyz");
        InputStream in = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("xml-samples/persistence-schema-headers.xml");
        Collection<JpaUnit> units = parser.getPersistenceUnits(new InputSource(in), root);

        assertEquals(1, units.size());

        JpaUnit info = (JpaUnit) units.iterator().next();

        assertEquals("OrderManagement", info.getPersistenceUnitName());

        // JTA is the default when type is ommitted
        assertEquals(PersistenceUnitTransactionType.JTA, info.getTransactionType());
        assertEquals(root, info.getPersistenceUnitRootUrl());
        assertTrue(info.getDescription().startsWith("This unit manages orders"));
        assertEquals("jdbc/MyOrderDB", info.getProperties().getProperty(
                Provider.JTA_DATA_SOURCE_PROPERTY));

        assertEquals(1, info.getMappingFileNames().size());
        assertTrue(info.getMappingFileNames().contains("ormap.xml"));

        assertEquals(2, info.getManagedClassNames().size());
        assertTrue(info.getManagedClassNames().contains("com.widgets.Order"));
        assertTrue(info.getManagedClassNames().contains("com.widgets.Customer"));
    }

    public void testSample2() throws Exception {
        UnitDescriptorParser parser = new UnitDescriptorParser();

        URL root = new URL("file:///xyz");
        InputStream in = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("xml-samples/persistence2.xml");
        Collection<JpaUnit> units = parser.getPersistenceUnits(new InputSource(in), root);

        assertEquals(3, units.size());

        Iterator<JpaUnit> it = units.iterator();
        for (int i = 1; i <= 3; i++) {
            JpaUnit info = it.next();

            assertEquals("Unit" + i, info.getPersistenceUnitName());
            assertEquals(root, info.getPersistenceUnitRootUrl());
            assertNull(info.getDescription());
            assertTrue(info.getProperties().isEmpty());

            assertEquals(0, info.getMappingFileNames().size());
            assertEquals(0, info.getManagedClassNames().size());
        }
    }

    public void testSample3() throws Exception {
        UnitDescriptorParser parser = new UnitDescriptorParser();

        URL root = new URL("file:///xyz");
        InputStream in = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("xml-samples/persistence3.xml");
        Collection<JpaUnit> units = parser.getPersistenceUnits(new InputSource(in), root);

        assertEquals(1, units.size());

        JpaUnit info = units.iterator().next();

        assertEquals("OrderManagement4", info.getPersistenceUnitName());
        assertEquals(PersistenceUnitTransactionType.RESOURCE_LOCAL, info
                .getTransactionType());
        assertEquals(root, info.getPersistenceUnitRootUrl());
        assertEquals("jdbc/MyDB", info.getProperties().getProperty(
                Provider.NON_JTA_DATA_SOURCE_PROPERTY));

        assertEquals(1, info.getMappingFileNames().size());
        assertTrue(info.getMappingFileNames().contains("order-mappings.xml"));

        assertEquals(3, info.getManagedClassNames().size());
        assertTrue(info.getManagedClassNames().contains("com.acme.Order"));
        assertTrue(info.getManagedClassNames().contains("com.acme.Customer"));
        assertTrue(info.getManagedClassNames().contains("com.acme.Item"));

        assertTrue(info.excludeUnlistedClasses());
    }

    public void testSampleSchemaHeaders() throws Exception {
        UnitDescriptorParser parser = new UnitDescriptorParser();

        URL root = new URL("file:///xyz");
        InputStream in = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("xml-samples/persistence-schema-headers.xml");
        Collection<JpaUnit> units = parser.getPersistenceUnits(new InputSource(in), root);

        assertEquals(1, units.size());

        JpaUnit info = units.iterator().next();

        assertEquals("OrderManagement", info.getPersistenceUnitName());

        // JTA is the default when type is omitted
        assertEquals(PersistenceUnitTransactionType.JTA, info.getTransactionType());
        assertEquals(root, info.getPersistenceUnitRootUrl());
        assertTrue(info.getDescription().startsWith("This unit manages orders"));
        assertEquals("jdbc/MyOrderDB", info.getProperties().getProperty(
                Provider.JTA_DATA_SOURCE_PROPERTY));

        assertEquals(1, info.getMappingFileNames().size());
        assertTrue(info.getMappingFileNames().contains("ormap.xml"));

        assertEquals(2, info.getManagedClassNames().size());
        assertTrue(info.getManagedClassNames().contains("com.widgets.Order"));
        assertTrue(info.getManagedClassNames().contains("com.widgets.Customer"));
    }

    public void testInvalidSample1() throws Exception {
        UnitDescriptorParser parser = new UnitDescriptorParser(true);

        URL root = new URL("file:///xyz");
        InputStream in = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("xml-samples/persistence-invalid1.xml");

        try {
            parser.getPersistenceUnits(new InputSource(in), root);
            fail("Validation did not detect errors");
        }
        catch (SAXParseException e) {
            // expected
            assertTrue(e.getMessage().indexOf("invalidtag") > 0);
        }
    }

    public void testInvalidSample2() throws Exception {
        UnitDescriptorParser parser = new UnitDescriptorParser(true);

        URL root = new URL("file:///xyz");
        InputStream in = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("xml-samples/persistence-invalid2.xml");

        try {
            parser.getPersistenceUnits(new InputSource(in), root);
            fail("Validation did not detect errors");
        }
        catch (SAXParseException e) {
            // expected
            assertTrue(e.getMessage().indexOf("persistence-unit") > 0);
        }
    }

}
