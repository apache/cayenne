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

package org.apache.cayenne.remote.hessian;

import org.apache.cayenne.remote.hessian.HessianConnection;

import junit.framework.TestCase;

public class HessianConnectionTest extends TestCase {

    public void testConstructor1Arg() {
        HessianConnection c = new HessianConnection("a");
        assertEquals("a", c.getUrl());
        assertNull(c.getUserName());
        assertNull(c.getPassword());
    }
    
    public void testConstructor3Arg() {
        HessianConnection c = new HessianConnection("a", "b", "c", "d");
        assertEquals("a", c.getUrl());
        assertEquals("b", c.getUserName());
        assertEquals("c", c.getPassword());
        assertEquals("d", c.getSharedSessionName());
    }
}
