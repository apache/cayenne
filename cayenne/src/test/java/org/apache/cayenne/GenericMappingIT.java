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

package org.apache.cayenne;

import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenericMappingIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.GENERIC_PROJECT);

    @Test
    public void testInsertSingle() {
        Persistent g1 = env.context().newObject("Generic1");
        g1.writeProperty("name", "G1 Name");

        env.context().commitChanges();
    }

    @Test
    public void testInsertRelated() {
        Persistent g1 = env.context().newObject("Generic1");
        g1.writeProperty("name", "G1 Name");

        Persistent g2 = env.context().newObject("Generic2");
        g2.writeProperty("name", "G2 Name");
        g2.setToOneTarget("toGeneric1", g1, true);

        env.context().commitChanges();
    }

    @Test
    public void testSelect() {
        env.context().performNonSelectingQuery(new SQLTemplate(
                "Generic1",
                "INSERT INTO GENERIC1 (ID, NAME) VALUES (1, 'AAAA')"));
        env.context().performNonSelectingQuery(new SQLTemplate(
                "Generic1",
                "INSERT INTO GENERIC1 (ID, NAME) VALUES (2, 'BBBB')"));
        env.context().performNonSelectingQuery(new SQLTemplate(
                "Generic1",
                "INSERT INTO GENERIC2 (GENERIC1_ID, ID, NAME) VALUES (1, 1, 'CCCCC')"));

        Expression qual = ExpressionFactory.matchExp("name", "AAAA");
        List<?> result = ObjectSelect.query(Object.class, "Generic1").where(qual).select(env.context());
        assertEquals(1, result.size());
    }

    @Test
    public void testUpdateRelated() {
        Persistent g1 = env.context().newObject("Generic1");
        g1.writeProperty("name", "G1 Name");

        Persistent g2 = env.context().newObject("Generic2");
        g2.writeProperty("name", "G2 Name");
        g2.setToOneTarget("toGeneric1", g1, true);

        env.context().commitChanges();

        List<?> r1 = (List<?>) g1.readProperty("generic2s");
        assertTrue(r1.contains(g2));

        Persistent g11 = env.context().newObject("Generic1");
        g11.writeProperty("name", "G11 Name");
        g2.setToOneTarget("toGeneric1", g11, true);

        env.context().commitChanges();

        List<?> r11 = (List<?>) g11.readProperty("generic2s");
        assertTrue(r11.contains(g2));

        List<?> r1_1 = (List<?>) g1.readProperty("generic2s");
        assertFalse(r1_1.contains(g2));
    }
}
