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

import org.apache.cayenne.GenericPersistentObject;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A vertical inheritance hierarchy of generic (class-less) entities: GenStudent with GenBoy and GenGirl sub-entities
 * that keep their own attributes in their own tables.
 */
public class CompiledEntity_VerticalInheritanceIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.INHERITANCE_VERTICAL_PROJECT);

    private static Set<String> names(Iterable<CompiledAttribute> attributes) {
        Set<String> names = new TreeSet<>();
        for (CompiledAttribute attribute : attributes) {
            names.add(attribute.name());
        }
        return names;
    }

    @Test
    public void hierarchy() {
        CompiledEntity genStudent = env.mapping().entity("GenStudent");
        assertNotNull(genStudent);
        assertTrue(genStudent.isAbstract());
        assertNull(genStudent.superEntity());

        CompiledEntity genBoy = genStudent.subEntity("GenBoy");
        assertNotNull(genBoy);
        CompiledEntity genGirl = genStudent.subEntity("GenGirl");
        assertNotNull(genGirl);

        assertNotSame(genStudent, genBoy);
        assertNotSame(genStudent, genGirl);
        assertNotSame(genBoy, genGirl);

        assertSame(genStudent, genBoy.superEntity());
        assertSame(genStudent, genGirl.superEntity());
        assertTrue(genBoy.isSubEntityOf(genStudent));
        assertFalse(genStudent.isSubEntityOf(genBoy));
        assertEquals(Set.of(genBoy, genGirl), Set.copyOf(genStudent.subEntities()));
        assertEquals(Set.of(genStudent, genBoy, genGirl), Set.copyOf(genStudent.allSubEntities()));
        assertSame(genStudent, genStudent.allSubEntities().getFirst());
        assertTrue(genBoy.subEntities().isEmpty());
        assertEquals(List.of(genBoy), genBoy.allSubEntities());

        assertSame(genBoy, env.mapping().entity("GenBoy"));
        assertSame(genGirl, env.mapping().entity("GenGirl"));
    }

    @Test
    public void genericObjectClass() {
        CompiledEntity genStudent = env.mapping().entity("GenStudent");
        assertEquals(GenericPersistentObject.class, genStudent.objectClass());
        assertEquals(GenericPersistentObject.class, genStudent.subEntity("GenBoy").objectClass());
        assertEquals(GenericPersistentObject.class, genStudent.subEntity("GenGirl").objectClass());
        assertTrue(genStudent.subEntity("GenBoy").isGeneric());
        assertInstanceOf(GenericPersistentObject.class, genStudent.subEntity("GenBoy").createObject());
    }

    @Test
    public void declaredAndInheritedAttributes() {
        CompiledEntity genStudent = env.mapping().entity("GenStudent");
        CompiledEntity genBoy = genStudent.subEntity("GenBoy");

        assertEquals(Set.of("name", "reference", "type"), names(genStudent.declaredAttributes()));
        assertEquals(Set.of("name", "reference", "type"), names(genStudent.attributes()));
        assertEquals(Set.of("name", "reference", "type", "toyDolls", "toyTrucks"), names(genStudent.allAttributes()));

        assertEquals(Set.of("toyTrucks"), names(genBoy.declaredAttributes()));
        assertEquals(Set.of("name", "reference", "type", "toyTrucks"), names(genBoy.attributes()));
        assertEquals(Set.of("name", "reference", "type", "toyTrucks"), names(genBoy.allAttributes()));

        // the sub-entity has its own instances of the inherited attributes
        CompiledAttribute boyName = genBoy.attribute("name");
        assertNotSame(genStudent.attribute("name"), boyName);
        assertTrue(boyName.isInherited());
        assertSame(genBoy, boyName.entity());
        assertFalse(genStudent.attribute("name").isInherited());

        CompiledAttribute toyTrucks = genBoy.attribute("toyTrucks");
        assertFalse(toyTrucks.isInherited());
        assertTrue(toyTrucks.isFlattened());
        assertEquals("boy.TOY_TRUCKS", toyTrucks.dbPath().value());
        assertEquals("TOY_TRUCKS", toyTrucks.column().name());
        assertEquals("GEN_BOY", toyTrucks.column().table().name());
    }

    @Test
    public void tables() {
        CompiledEntity genStudent = env.mapping().entity("GenStudent");
        CompiledEntity genBoy = genStudent.subEntity("GenBoy");

        // the table of the hierarchy root, and the sub-entity table reached through a flattened path
        assertEquals("GEN_STUDENT", genBoy.table().name());
        assertEquals(List.of(env.mapping().schema().table("GEN_STUDENT")), genBoy.rootTables());
        assertEquals(List.of(env.mapping().schema().table("GEN_STUDENT")), genStudent.rootTables());

        assertTrue(genStudent.flattenedTables().isEmpty());
        assertEquals(Set.of(CayennePath.of("boy")), genBoy.flattenedTables().keySet());
        CompiledFlattenedTable boy = genBoy.flattenedTables().get(CayennePath.of("boy"));
        assertSame(env.mapping().schema().table("GEN_BOY"), boy.table());
        assertEquals(CayennePath.of("boy"), boy.path());
    }

    @Test
    public void discriminatorValues() {
        CompiledEntity genStudent = env.mapping().entity("GenStudent");
        CompiledEntity genBoy = genStudent.subEntity("GenBoy");
        CompiledEntity genGirl = genStudent.subEntity("GenGirl");

        // an abstract root with no qualifier of its own filters nothing, so it has no discriminator columns; the
        // qualified sub-entities do
        assertTrue(genStudent.discriminatorAttributes().isEmpty());
        assertEquals(List.of("type"), genBoy.discriminatorAttributes().stream().map(CompiledAttribute::name).toList());
        assertSame(genBoy.attribute("type"), genBoy.discriminatorAttributes().getFirst());

        GenericPersistentObject boy = (GenericPersistentObject) genBoy.createObject();
        genBoy.injectDiscriminatorValues(boy);
        assertEquals("B", boy.readPropertyDirectly("type"));
        assertEquals("B", genBoy.attribute("type").readPropertyDirectly(boy));

        GenericPersistentObject girl = (GenericPersistentObject) genGirl.createObject();
        genGirl.injectDiscriminatorValues(girl);
        assertEquals("G", girl.readPropertyDirectly("type"));

        // the abstract root has no qualifier of its own
        GenericPersistentObject student = (GenericPersistentObject) genStudent.createObject();
        genStudent.injectDiscriminatorValues(student);
        assertNull(student.readPropertyDirectly("type"));
    }
}
