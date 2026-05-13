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

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.no_pk.NoPkTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataContextNoPkIT  {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.NO_PK_PROJECT);

        protected ObjectContext context;


    
    @BeforeEach
    public void setUp() throws Exception {
        context = env.context();
        TableHelper noPkTestTable = env.table("NO_PK_TEST", "ATTRIBUTE1");
        noPkTestTable.deleteAll();

        noPkTestTable.insert(1);
        noPkTestTable.insert(2);
    }

    @Test
    public void noPkFetchObjects() {
        assertThrows(CayenneRuntimeException.class, () -> ObjectSelect.query(NoPkTestEntity.class).select(context));
    }

    @Test
    public void noPkFetchDataRows() {

        List<DataRow> rows = ObjectSelect.dataRowQuery(NoPkTestEntity.class).select(context);
        assertNotNull(rows);
        assertEquals(2, rows.size());

        DataRow row1 = rows.get(0);
        DataRow row2 = rows.get(1);

        // assert that rows have different values
        // (there was a bug earlier that fetched distinct rows for
        // entities with no primary key.
        assertTrue(!row1.get("ATTRIBUTE1").equals(row2.get("ATTRIBUTE1")));
    }
}
