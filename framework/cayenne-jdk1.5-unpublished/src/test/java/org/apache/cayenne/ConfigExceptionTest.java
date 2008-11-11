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

package org.apache.cayenne;

import junit.framework.TestCase;

/**
 */
public class ConfigExceptionTest extends TestCase {

    public void testConstructor1() throws Exception {
        ConfigurationException ex = new ConfigurationException();
        assertNull(ex.getCause());
        assertTrue(ex.getMessage().startsWith(CayenneException.getExceptionLabel()));
    }

    public void testConstructor2() throws Exception {
        ConfigurationException ex = new ConfigurationException("abc");
        assertNull(ex.getCause());
        assertEquals(CayenneException.getExceptionLabel() + "abc", ex.getMessage());
    }

    public void testConstructor3() throws Exception {
        Throwable cause = new Throwable();
        ConfigurationException ex = new ConfigurationException(cause);
        assertSame(cause, ex.getCause());
        assertEquals(
            CayenneException.getExceptionLabel() + cause.toString(),
            ex.getMessage());
    }

    public void testConstructor4() throws Exception {
        Throwable cause = new Throwable();
        ConfigurationException ex = new ConfigurationException("abc", cause);
        assertSame(cause, ex.getCause());
        assertEquals(CayenneException.getExceptionLabel() + "abc", ex.getMessage());
    }
}
