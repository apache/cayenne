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

package org.apache.cayenne.unit.jira;

import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.relationship.ClobMaster;
import org.apache.cayenne.unit.RelationshipCase;

/**
 */
public class CAY_115Test extends RelationshipCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testDistinctClobFetch() throws Exception {
        if (!getAccessStackAdapter().supportsLobInsertsAsStrings()) {
            return;
        }

        createTestData("testDistinctClobFetch");

        DataContext context = createDataContext();

        SelectQuery noDistinct = new SelectQuery(ClobMaster.class);
        noDistinct.addOrdering(ClobMaster.NAME_PROPERTY, true);

        SelectQuery distinct = new SelectQuery(ClobMaster.class);
        distinct.setDistinct(true);
        distinct.addOrdering(ClobMaster.NAME_PROPERTY, true);

        List noDistinctResult = context.performQuery(noDistinct);
        List distinctResult = context.performQuery(distinct);

        assertEquals(3, noDistinctResult.size());
        assertEquals(noDistinctResult, distinctResult);
    }

    public void testDistinctClobFetchWithToManyJoin() throws Exception {
        if (!getAccessStackAdapter().supportsLobInsertsAsStrings()) {
            return;
        }

        createTestData("testDistinctClobFetchWithToManyJoin");

        DataContext context = createDataContext();

        Expression qual = Expression.fromString("details.name like 'cd%'");
        SelectQuery query = new SelectQuery(ClobMaster.class, qual);
        List result = context.performQuery(query);

        assertEquals(3, result.size());
    }
}
