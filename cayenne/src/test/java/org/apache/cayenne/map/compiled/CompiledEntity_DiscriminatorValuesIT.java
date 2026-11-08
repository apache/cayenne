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

import org.apache.cayenne.testdo.inheritance_with_enum.Root;
import org.apache.cayenne.testdo.inheritance_with_enum.Sub;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class CompiledEntity_DiscriminatorValuesIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.INHERITANCE_WITH_ENUM_PROJECT);

    @Test
    public void compiledFromQualifier() {
        // "type = 1" in the mapping, on a "short" attribute: the value is converted when the entity is compiled
        Sub sub = new Sub();
        env.mapping().entity("Sub").injectDiscriminatorValues(sub);
        assertEquals((short) 1, sub.getType());

        Root root = new Root();
        env.mapping().entity("Root").injectDiscriminatorValues(root);
        assertEquals((short) 0, root.getType());
    }

    @Test
    public void subEntityAppliesItsOwnQualifierOnly() {
        // a sub-entity applies the qualifier declared on it, like query translation does, so the super-entity
        // discriminator value is not applied over it
        Sub sub = new Sub();
        sub.setName("n");
        env.mapping().entity("Sub").injectDiscriminatorValues(sub);
        assertEquals((short) 1, sub.getType());
        assertEquals("n", sub.getName(), "unrelated attributes are left alone");
    }

    @Test
    public void discriminatorAttributes() {
        CompiledEntity root = env.mapping().entity("Root");
        CompiledEntity sub = env.mapping().entity("Sub");

        // the attribute the qualifiers of the hierarchy refer to, seen from either entity
        assertEquals(List.of("type"), root.discriminatorAttributes().stream().map(CompiledAttribute::name).toList());
        assertEquals(List.of("type"), sub.discriminatorAttributes().stream().map(CompiledAttribute::name).toList());
        assertSame(root.attribute("type"), root.discriminatorAttributes().getFirst());
        assertSame(sub.attribute("type"), sub.discriminatorAttributes().getFirst());
    }

    @Test
    public void appliedToNewObjects() {
        assertEquals((short) 1, env.context().newObject(Sub.class).getType());
        assertEquals((short) 0, env.context().newObject(Root.class).getType());
    }
}
