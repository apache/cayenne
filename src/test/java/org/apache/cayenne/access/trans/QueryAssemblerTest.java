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

package org.apache.cayenne.access.trans;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class QueryAssemblerTest extends CayenneCase {

    protected TstQueryAssembler qa;

    @Override
    protected void setUp() throws Exception {
        qa = new TstQueryAssembler(getNode(), new SelectQuery());
    }

    public void testGetQuery() throws Exception {
        try {
            assertNotNull(qa.getQuery());
        }
        finally {
            qa.dispose();
        }
    }

    public void testAddToParamList() throws Exception {
        try {
            assertEquals(0, qa.getAttributes().size());
            assertEquals(0, qa.getValues().size());

            qa.addToParamList(new DbAttribute(), new Object());
            assertEquals(1, qa.getAttributes().size());
            assertEquals(1, qa.getValues().size());
        }
        finally {
            qa.dispose();
        }
    }

    public void testCreateStatement() throws Exception {
        try {
            assertNotNull(qa.createStatement());
        }
        finally {
            qa.dispose();
        }
    }
}
