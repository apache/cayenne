package org.apache.cayenne.map;

/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

import junit.framework.TestCase;

/**
 */
public class ProcedureTest extends TestCase {
    protected Procedure procedure;

    @Override
    public void setUp() throws Exception {
        procedure = new Procedure();
    }

    public void testFullyQualifiedName() throws Exception {
        String tstName = "tst_name";
        String schemaName = "tst_schema_name";
        procedure.setName(tstName);

        assertEquals(tstName, procedure.getName());
        assertEquals(tstName, procedure.getFullyQualifiedName());

        procedure.setSchema(schemaName);

        assertEquals(tstName, procedure.getName());
        assertEquals(schemaName + "." + tstName, procedure.getFullyQualifiedName());
    }

}
