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

package org.apache.cayenne.reflect;

import junit.framework.TestCase;

import org.apache.cayenne.access.types.MockEnum;
import org.apache.cayenne.access.types.MockEnumHolder;

public class PropertyUtilsTest extends TestCase {

    public void testSetConverted() {
        MockEnumHolder o1 = new MockEnumHolder();

        // String to Enum
        PropertyUtils.setProperty(o1, "mockEnum", "b");
        assertSame(MockEnum.b, o1.getMockEnum());
        
        // check that regular converters still work
        PropertyUtils.setProperty(o1, "number", "445");
        assertEquals(445, o1.getNumber());
    }
}
