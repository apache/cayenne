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

import org.apache.cayenne.access.types.MockEnum2;
import org.apache.cayenne.access.types.MockEnum3;
import org.apache.cayenne.access.types.MockEnum4;

public class EnumConverterTest extends TestCase {
    
    public void testConvert() {
        EnumConverter converter = new EnumConverter();

        assertSame(MockEnum2.x, converter.convert("x", MockEnum2.class));
        assertSame(MockEnum2.z, converter.convert("z", MockEnum2.class));
    }

    public void testConvertExtendedEnumeration() {
        EnumConverter converter = new EnumConverter();

        assertSame(MockEnum3.B, converter.convert(2, MockEnum3.class));
        assertSame(MockEnum3.C, converter.convert(3, MockEnum3.class));
    }
    
    public void testConvertExtendedEnumerationWithNull() {
        EnumConverter converter = new EnumConverter();

        assertSame(MockEnum4.B, converter.convert(null, MockEnum4.class));
        assertSame(MockEnum4.C, converter.convert("3", MockEnum4.class));
    }
}
