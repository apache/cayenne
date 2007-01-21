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

package org.apache.cayenne.modeler.util;

import junit.framework.TestCase;

public class VersionTest extends TestCase {

    public void testValidConstructor() {
        Version v1 = new Version("1");
        assertEquals("1", v1.getVersionString());

        Version v1_1 = new Version("1.1");
        assertEquals("1.1", v1_1.getVersionString());
    }

    public void testInValidConstructor() {
        try {
            new Version(null);
            fail("null version is invalid");
        }
        catch (IllegalArgumentException nfex) {

        }

        try {
            new Version(" ");
            fail("empty version is invalid");
        }
        catch (IllegalArgumentException nfex) {

        }

        try {
            new Version("1a");
            fail("non-numeric version is invalid");
        }
        catch (NumberFormatException nfex) {

        }
    }

    public void testCompare() {
        assertEquals(0, new Version("1.0").compareTo("1.0"));
        assertEquals(0, new Version("1.0.1").compareTo("1.0.1"));
        assertTrue(new Version("1.0.1").compareTo("1.0.2") < 0);
        assertTrue(new Version("1.0.2").compareTo("1.0.1") > 0);
        assertTrue(new Version("1.0.1.1").compareTo("1.0.1") > 0);
    }
}
