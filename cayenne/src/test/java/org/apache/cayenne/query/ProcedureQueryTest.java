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

package org.apache.cayenne.query;

import org.apache.cayenne.map.Procedure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcedureQueryTest {

    @Test
    public void setRootProcedureName() {
        ProcedureQuery query = new ProcedureQuery("SomeProcedure");
        assertSame("SomeProcedure", query.getRoot());
    }

    @Test
    public void setRootProcedure() {
        Procedure procedure = new Procedure("ABC");
        ProcedureQuery query = new ProcedureQuery(procedure);
        assertSame(procedure, query.getRoot());
    }

    @Test
    public void setInvalidRoot() {
        ProcedureQuery query = new ProcedureQuery();
        assertNull(query.getRoot());
        assertThrows(IllegalArgumentException.class, () -> query.setRoot(1));
    }

    @Test
    public void columnNameCapitalization() {
        ProcedureQuery q1 = new ProcedureQuery();
        assertSame(CapsStrategy.DEFAULT, q1.getColumnNamesCapitalization());
        q1.setColumnNamesCapitalization(CapsStrategy.UPPER);
        assertEquals(CapsStrategy.UPPER, q1.getColumnNamesCapitalization());
    }

    @Test
    public void resultEntityName() {
        ProcedureQuery query = new ProcedureQuery();
        assertNull(query.getResultEntityName());

        query.setResultEntityName("abc.AAAA");
        assertSame("abc.AAAA", query.getResultEntityName());
    }

    @Test
    public void resultDescriptors() {
        ProcedureQuery query = new ProcedureQuery();

        assertNotNull(query.getResultDescriptors());
        assertTrue(query.getResultDescriptors().isEmpty());

        ProcedureColumn[] descriptor = new ProcedureColumn[5];
        query.addResultDescriptor(descriptor);
        assertEquals(1, query.getResultDescriptors().size());
        assertTrue(query.getResultDescriptors().contains(descriptor));

        query.removeResultDescriptor(descriptor);
        assertNotNull(query.getResultDescriptors());
        assertTrue(query.getResultDescriptors().isEmpty());
    }
}
