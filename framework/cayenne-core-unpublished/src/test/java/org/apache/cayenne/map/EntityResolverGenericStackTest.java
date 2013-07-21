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
package org.apache.cayenne.map;

import java.util.Collections;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.DEFAULT_PROJECT)
public class EntityResolverGenericStackTest extends ServerCase {

    @Inject
    private EntityResolver resolver;

    public void testObjEntityLookupDuplicates() {

        DataMap generic = resolver.getDataMap("generic");
        EntityResolver resolver = new EntityResolver(Collections.singleton(generic));

        ObjEntity g1 = resolver.getObjEntity("Generic1");
        assertNotNull(g1);

        ObjEntity g2 = resolver.getObjEntity("Generic2");
        assertNotNull(g2);

        assertNotSame(g1, g2);
        assertNull(resolver.getObjEntity(Object.class));

        try {
            resolver.getObjEntity(CayenneDataObject.class);
            fail("two entities mapped to the same class... resolver must have thrown.");
        }
        catch (CayenneRuntimeException e) {
            // expected
        }
    }
}
