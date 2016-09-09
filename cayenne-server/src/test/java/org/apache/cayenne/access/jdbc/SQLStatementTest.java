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

package org.apache.cayenne.access.jdbc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 */
public class SQLStatementTest {

    @Test
    public void testConstructor() throws Exception {
        SQLParameterBinding[] bindings = new SQLParameterBinding[0];
        SQLStatement statement = new SQLStatement("abc", bindings);
        assertEquals("abc", statement.getSql());
        assertSame(bindings, statement.getBindings());
    }

    @Test
    public void testSQL() throws Exception {
        assertEquals("abc", new SQLStatement("abc", null).getSql());
        assertEquals("xyz", new SQLStatement("xyz", null).getSql());
        assertNull(new SQLStatement(null, null).getSql());
    }
}
