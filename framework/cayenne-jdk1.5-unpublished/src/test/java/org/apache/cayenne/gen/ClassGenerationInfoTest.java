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
package org.apache.cayenne.gen;

import junit.framework.TestCase;

public class ClassGenerationInfoTest extends TestCase {

    public void testFormatJavaType() {
        ClassGenerationInfo info = new ClassGenerationInfo();
        
        assertEquals("x.X", info.formatJavaType("x.X"));
        assertEquals("X", info.formatJavaType("java.lang.X"));
        assertEquals("java.lang.x.X", info.formatJavaType("java.lang.x.X"));
    }
}
