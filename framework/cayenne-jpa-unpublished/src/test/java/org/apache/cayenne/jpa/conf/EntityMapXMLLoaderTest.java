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

import junit.framework.TestCase;

import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.xml.sax.SAXException;

public class EntityMapXMLLoaderTest extends TestCase {

    protected ClassLoader resourceLoader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.resourceLoader = Thread.currentThread().getContextClassLoader();
    }

    public void testSampleSchemaHeadersValidating() throws Exception {

        EntityMapXMLLoader loader = new EntityMapXMLLoader(
                getClass().getClassLoader(),
                true);

        loader.getEntityMap(resourceLoader
                .getResource("xml-samples/orm-schema-headers.xml"));
        // no validation exception is thrown... good

        try {
            loader.getEntityMap(resourceLoader
                    .getResource("xml-samples/orm-schema-headers-invalid1.xml"));
            fail("Failed to throw an exception on invalid tag");
        }
        catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof SAXException);
        }
    }

    public void testDetails() throws Exception {
        EntityMapXMLLoader loader = new EntityMapXMLLoader(
                getClass().getClassLoader(),
                true);

        // long t0 = System.currentTimeMillis();
        JpaEntityMap map = loader.getEntityMap(resourceLoader
                .getResource("xml-samples/orm-schema-headers-full.xml"));
        // long t1 = System.currentTimeMillis();
        // System.out.println("Load time: " + (t1 - t0));

        new XMLMappingAssertion().testEntityMap(map);
    }
}
