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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships.E1;
import org.apache.cayenne.testdo.relationships.E2;
import org.apache.cayenne.testdo.relationships.ReflexiveAndToOne;
import org.apache.cayenne.unit.OracleUnitDbAdapter;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.After;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.RELATIONSHIPS_PROJECT)
public class CircularDependencyIT extends RuntimeCase {

    @Inject
    private UnitDbAdapter unitDbAdapter;

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @After
    public void cleanUp() throws SQLException {
        // manually cleanup circular references
        TableHelper e1 = new TableHelper(dbHelper, "CYCLE_E1", "id", "e2_id", "text");
        e1.setColumnTypes(Types.INTEGER, Types.INTEGER, Types.VARCHAR);
        TableHelper e2 = new TableHelper(dbHelper, "CYCLE_E2", "id", "e1_id", "text");
        e2.setColumnTypes(Types.INTEGER, Types.INTEGER, Types.VARCHAR);
        TableHelper reflexive = new TableHelper(dbHelper, "REFLEXIVE_AND_TO_ONE", "REFLEXIVE_AND_TO_ONE_ID", "NAME", "PARENT_ID");

        e1.update().set("e2_id", null, Types.INTEGER).execute();
        e2.update().set("e1_id", null, Types.INTEGER).execute();
        e1.deleteAll();
        e2.deleteAll();

        reflexive.update().set("PARENT_ID", null, Types.INTEGER).execute();
        reflexive.deleteAll();
    }

    @Test()
    public void testCycle() {
        E1 e1 = context.newObject(E1.class);
        E2 e2 = context.newObject(E2.class);

        e1.setText("e1 #" + 1);
        e2.setText("e2 #" + 2);

        e1.setE2(e2);
        e2.setE1(e1);

        try {
            context.commitChanges();
            fail("Exception should be thrown here");
        } catch (CayenneRuntimeException ex) {
            // TODO: Oracle adapter still does not fully support key generation.
            if (unitDbAdapter instanceof OracleUnitDbAdapter) {
                assertTrue(ex.getCause().getMessage().contains("parent key not found"));
            } else {
                assertTrue(String.format("Unexpected exception message: %s%nCause: %s - %s",
                                ex.getMessage(), ex.getCause(),
                                ex.getCause() != null ? ex.getCause().getMessage() : null),
                        ex.getMessage().contains("PK is not generated"));
            }
        }

    }

    @Test
    public void testUpdate() {
        E1 e1 = context.newObject(E1.class);
        E2 e2 = context.newObject(E2.class);

        e1.setText("e1 #" + 1);
        e2.setText("e2 #" + 2);
        context.commitChanges();

        e1.setE2(e2);
        context.commitChanges();

        e2.setE1(e1);
        context.commitChanges();
    }

    @Test
    public void testUpdateSelfRelationship() {
        ReflexiveAndToOne e1 = context.newObject(ReflexiveAndToOne.class);
        ReflexiveAndToOne e2 = context.newObject(ReflexiveAndToOne.class);

        e1.setName("e1 #" + 1);
        e2.setName("e2 #" + 2);

        e1.setToParent(e2);
        context.commitChanges();

        e2.setToParent(e1);
        context.commitChanges();
    }
}
