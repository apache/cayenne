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
package org.apache.cayenne.configuration;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DefaultRuntimePropertiesTest {

    @Test
    public void testGet_FromMap() {
        Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("key1", "v1");

        DefaultRuntimeProperties properties = new DefaultRuntimeProperties(propertiesMap);
        assertEquals("v1", properties.get("key1"));
        assertNull(properties.get("key2"));
    }

    @Test
    public void testGet_FromSystem() {

        String userDir = System.getProperty("user.dir");
        assertNotNull(userDir);

        Map<String, String> propertiesMap = new HashMap<>();

        DefaultRuntimeProperties properties = new DefaultRuntimeProperties(propertiesMap);
        assertEquals(userDir, properties.get("user.dir"));
    }

    @Test
    public void testGet_FromSystemOverridesMap() {

        String userDir = System.getProperty("user.dir");
        assertNotNull(userDir);

        Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("user.dir", userDir + "_altered");

        DefaultRuntimeProperties properties = new DefaultRuntimeProperties(propertiesMap);
        assertEquals(userDir, properties.get("user.dir"));
    }
}
