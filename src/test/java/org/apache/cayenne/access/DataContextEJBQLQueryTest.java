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
package org.apache.cayenne.access;

import java.util.List;

import org.apache.art.Artist;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLQueryTest extends CayenneCase {

    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testSimpleSelect() throws Exception {
        createTestData("prepare");

        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List artists = createDataContext().performQuery(query);
        assertEquals(4, artists.size());
        assertTrue(artists.get(0) instanceof Artist);
        assertTrue(((Artist) artists.get(0)).getPersistenceState() == PersistenceState.COMMITTED);
    }
}
