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
package org.apache.cayenne.map.compiled;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.GenericPersistentObject;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entities without a persistent class of their own, all mapped to {@link GenericPersistentObject}.
 */
public class CompiledEntity_GenericIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.GENERIC_PROJECT);

    @Test
    public void entityLookup() {
        CompiledMapping mapping = env.mapping();

        CompiledEntity g1 = mapping.entity("Generic1");
        assertNotNull(g1);
        CompiledEntity g2 = mapping.entity("Generic2");
        assertNotNull(g2);
        assertNotSame(g1, g2);

        assertTrue(g1.isGeneric());
        assertEquals(GenericPersistentObject.class, g1.objectClass());
        assertEquals(GenericPersistentObject.class, g2.objectClass());

        // a class lookup is impossible when the class is shared by several entities
        assertNull(mapping.entity(Object.class));
        assertThrows(CayenneRuntimeException.class, () -> mapping.entity(GenericPersistentObject.class));
    }

    @Test
    public void attributeAccess() {
        CompiledEntity g1 = env.mapping().entity("Generic1");
        GenericPersistentObject object = assertInstanceOf(GenericPersistentObject.class, g1.createObject());

        CompiledAttribute name = g1.attribute("name");
        assertNotNull(name);
        assertNull(name.readPropertyDirectly(object));

        name.writePropertyDirectly(object, null, "n1");
        assertEquals("n1", object.readPropertyDirectly("name"));
        assertEquals("n1", name.readPropertyDirectly(object));

        object.writePropertyDirectly("name", "n2");
        assertEquals("n2", name.readPropertyDirectly(object));
    }

    @Test
    public void relationships() {
        CompiledEntity g1 = env.mapping().entity("Generic1");
        CompiledEntity g2 = env.mapping().entity("Generic2");

        CompiledToManyRelationship generic2s = assertInstanceOf(CompiledToManyRelationship.class,
                g1.relationship("generic2s"));
        assertSame(g2, generic2s.target());
        assertSame(g1, generic2s.source());

        CompiledToOneRelationship generic1 = assertInstanceOf(CompiledToOneRelationship.class,
                g2.relationship("generic1"));
        assertSame(generic1, generic2s.reverse());
        assertSame(generic2s, generic1.reverse());
    }

    @Test
    public void newObjects() {
        GenericPersistentObject g1 = (GenericPersistentObject) env.context().newObject("Generic1");
        assertEquals("Generic1", g1.getObjectId().getEntityName());
        assertSame(env.mapping().entity("Generic1"), env.mapping().entity(g1));
    }
}
