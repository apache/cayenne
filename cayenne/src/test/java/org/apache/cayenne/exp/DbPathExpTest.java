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
package org.apache.cayenne.exp;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class DbPathExpTest {

    @Test
    public void stringRepresentation() {
        assertEquals("db:x.y", ExpressionFactory.dbPathExp("x.y").toString());
    }

    @Test
    public void appendAsString() throws IOException {
        StringBuilder buffer = new StringBuilder();
        ExpressionFactory.dbPathExp("x.y").appendAsString(buffer);
        assertEquals("db:x.y", buffer.toString());
    }

    @Test
    public void equals() throws Exception {
        PathExp path1 = new DbPathExp("x.y.z");
        PathExp path2 = new DbPathExp("x.y.z");
        PathExp path3 = new DbPathExp("x.x.z");

        assertEquals(path1, path2);
        assertNotEquals(path1, path3);
    }

}
