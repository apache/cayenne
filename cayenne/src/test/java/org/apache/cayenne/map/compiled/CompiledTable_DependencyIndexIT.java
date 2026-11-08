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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dependency order of the tables, formerly the job of the entity sorter.
 */
public class CompiledTable_DependencyIndexIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject("cayenne-ALL.xml").withoutAutoClean();

    private static void assertBefore(String first, String second) {
        CompiledTable t1 = env.mapping().schema().table(first);
        CompiledTable t2 = env.mapping().schema().table(second);
        assertTrue(t1.dependencyIndex() < t2.dependencyIndex(), first + " must come before " + second);
    }

    private static List<String> sorted(Comparator<CompiledTable> comparator, String... names) {
        List<CompiledTable> tables = new ArrayList<>();
        for (String name : names) {
            tables.add(env.mapping().schema().table(name));
        }
        Collections.shuffle(tables);
        tables.sort(comparator);
        return tables.stream().map(CompiledTable::name).toList();
    }

    @Test
    public void dependencyIndex() {
        assertBefore("ARTIST", "ARTIST_EXHIBIT");
        assertBefore("ARTIST", "PAINTING");
        assertBefore("GALLERY", "EXHIBIT");
        assertBefore("EXHIBIT", "ARTIST_EXHIBIT");
        assertBefore("PAINTING", "PAINTING_INFO");
    }

    @Test
    public void dependencyIndex_InsertAndDeleteOrder() {
        Comparator<CompiledTable> insertOrder = Comparator.comparingInt(CompiledTable::dependencyIndex);
        assertEquals(List.of("ARTIST", "PAINTING", "PAINTING_INFO"),
                sorted(insertOrder, "ARTIST", "PAINTING", "PAINTING_INFO"));
        assertEquals(List.of("PAINTING_INFO", "PAINTING", "ARTIST"),
                sorted(insertOrder.reversed(), "ARTIST", "PAINTING", "PAINTING_INFO"));
    }

    @Test
    public void dependencyIndex_Cycle() {
        // the "people" map: a single-table inheritance hierarchy over PERSON, with a DEPARTMENT <-> PERSON cycle
        assertBefore("PERSON", "PERSON_NOTES");
        assertBefore("CLIENT_COMPANY", "PERSON");
        assertBefore("PERSON", "ADDRESS");

        // PERSON depends on DEPARTMENT and DEPARTMENT depends on PERSON, so the two share the index
        CompiledMapping mapping = env.mapping();
        assertEquals(mapping.schema().table("PERSON").dependencyIndex(),
                mapping.schema().table("DEPARTMENT").dependencyIndex());
    }

    @Test
    public void reflexive() {
        CompiledMapping mapping = env.mapping();
        assertTrue(mapping.schema().table("ARTGROUP").isReflexive());
        // the FK side of the self-reference
        assertEquals(1, mapping.schema().table("ARTGROUP").reflexiveJoins().size());
        assertEquals("parentGroup", mapping.schema().table("ARTGROUP").reflexiveJoins().getFirst().name());
        assertFalse(mapping.schema().table("ARTIST").isReflexive());
        assertTrue(mapping.schema().table("ARTIST").reflexiveJoins().isEmpty());
    }
}
