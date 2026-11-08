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

import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The attributes and relationships of a compiled entity, own or inherited, are iterated in name order, over every
 * test DataMap, with and without inheritance.
 */
public class CompiledEntity_PropertyOrderIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject("cayenne-ALL.xml").withoutAutoClean();

    private static void assertSortedAttributes(CompiledEntity entity, Collection<CompiledAttribute> attributes) {
        List<String> names = new ArrayList<>();
        for (CompiledAttribute attribute : attributes) {
            names.add(attribute.name());
        }
        assertSorted(entity, names);
    }

    private static void assertSortedRelationships(CompiledEntity entity,
                                                  Collection<CompiledRelationship> relationships) {
        List<String> names = new ArrayList<>();
        for (CompiledRelationship relationship : relationships) {
            names.add(relationship.name());
        }
        assertSorted(entity, names);
    }

    private static Set<String> names(Collection<?> properties) {
        Set<String> names = new HashSet<>();
        for (Object property : properties) {
            names.add(property instanceof CompiledAttribute a
                    ? a.name()
                    : ((CompiledRelationship) property).name());
        }
        return names;
    }

    private static void assertSorted(CompiledEntity entity, List<String> names) {
        for (int i = 1; i < names.size(); i++) {
            assertTrue(names.get(i - 1).compareTo(names.get(i)) < 0,
                    entity.name() + ": names are not ordered: " + names.get(i - 1) + " before " + names.get(i));
        }
    }

    @Test
    public void declaredProperties_IterationOrder() {
        assertFalse(env.mapping().entities().isEmpty());
        for (CompiledEntity entity : env.mapping().entities()) {
            assertSortedAttributes(entity, entity.declaredAttributes());
            assertSortedRelationships(entity, entity.declaredRelationships());
        }
    }

    @Test
    public void properties_IterationOrder() {
        for (CompiledEntity entity : env.mapping().entities()) {
            assertSortedAttributes(entity, entity.attributes());
            assertSortedRelationships(entity, entity.relationships());
        }
    }

    @Test
    public void allProperties_IncludeSubEntities() {
        for (CompiledEntity entity : env.mapping().entities()) {
            Set<CompiledAttribute> allAttributes = new HashSet<>(entity.allAttributes());
            assertTrue(allAttributes.containsAll(entity.attributes()), entity.name());
            Set<CompiledRelationship> allRelationships = new HashSet<>(entity.allRelationships());
            assertTrue(allRelationships.containsAll(entity.relationships()), entity.name());

            // a sub-entity has its own instances of the inherited properties, so only its declared ones are
            // expected as such; the inherited ones are present by name
            Set<String> allAttributeNames = names(allAttributes);
            Set<String> allRelationshipNames = names(allRelationships);
            for (CompiledEntity sub : entity.allSubEntities()) {
                assertTrue(allAttributes.containsAll(sub.declaredAttributes()), entity.name() + " / " + sub.name());
                assertTrue(allRelationships.containsAll(sub.declaredRelationships()),
                        entity.name() + " / " + sub.name());
                assertTrue(allAttributeNames.containsAll(names(sub.attributes())), entity.name() + " / " + sub.name());
                assertTrue(allRelationshipNames.containsAll(names(sub.relationships())),
                        entity.name() + " / " + sub.name());
            }

            if (!entity.hasSubEntities()) {
                assertEquals(new HashSet<>(entity.attributes()), allAttributes, entity.name());
                assertEquals(new HashSet<>(entity.relationships()), allRelationships, entity.name());
            }
        }
    }
}
