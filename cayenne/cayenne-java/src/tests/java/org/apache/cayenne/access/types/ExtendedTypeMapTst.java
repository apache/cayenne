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

package org.apache.cayenne.access.types;

import junit.framework.TestCase;

public class ExtendedTypeMapTst extends TestCase {

    public void testRegisterType() throws Exception {
        ExtendedTypeMap map = new ExtendedTypeMap();
        ExtendedType tstType = new MockExtendedType();

        assertSame(map.getDefaultType(), map.getRegisteredType(tstType.getClassName()));

        map.registerType(tstType);
        assertSame(tstType, map.getRegisteredType(tstType.getClassName()));

        map.unregisterType(tstType.getClassName());
        assertSame(map.getDefaultType(), map.getRegisteredType(tstType.getClassName()));
    }

    public void testRegisterArrayType() throws Exception {
        ExtendedTypeMap map = new ExtendedTypeMap();
        ByteArrayType tstType = new ByteArrayType(false, true);

        map.registerType(tstType);
        assertSame(tstType, map.getRegisteredType(tstType.getClassName()));
        assertSame(tstType, map.getRegisteredType(byte[].class));

        map.unregisterType(tstType.getClassName());
        assertSame(map.getDefaultType(), map.getRegisteredType(tstType.getClassName()));
    }

    public void testRegisteredTypeName() throws Exception {
        ExtendedTypeMap map = new TstTypeMap();
        ExtendedType tstType = new MockExtendedType();

        assertNotNull(map.getRegisteredTypeNames());
        assertEquals(0, map.getRegisteredTypeNames().length);

        map.registerType(tstType);

        assertNotNull(map.getRegisteredTypeNames());
        assertEquals(1, map.getRegisteredTypeNames().length);
        assertEquals(tstType.getClassName(), map.getRegisteredTypeNames()[0]);
    }

    class TstTypeMap extends ExtendedTypeMap {

        protected void initDefaultTypes() {
            // noop to avoid any default types
        }
    }
}
