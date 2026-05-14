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
package org.apache.cayenne.map;

import org.apache.cayenne.GenericPersistentObject;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityResolverGenericStackIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.GENERIC_PROJECT);

    private EntityResolver resolver;

    @BeforeEach
    public void setUp() {
        resolver = env.entityResolver();
    }

    @Test
    public void objEntityLookupDuplicates() {

        DataMap generic = resolver.getDataMap("generic");
        EntityResolver resolver = new EntityResolver(Collections.singleton(generic));

        ObjEntity g1 = resolver.getObjEntity("Generic1");
        assertNotNull(g1);

        ObjEntity g2 = resolver.getObjEntity("Generic2");
        assertNotNull(g2);

        assertNotSame(g1, g2);
        assertNull(resolver.getObjEntity(Object.class));

        assertThrows(CayenneRuntimeException.class, () -> resolver.getObjEntity(GenericPersistentObject.class));
    }
}
