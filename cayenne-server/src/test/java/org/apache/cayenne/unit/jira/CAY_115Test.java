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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationship.ClobMaster;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 */
@UseServerRuntime(ServerCase.RELATIONSHIPS_PROJECT)
public class CAY_115Test extends ServerCase {

    @Inject
    protected DataContext context;
    
    @Inject
    protected UnitDbAdapter accessStackAdapter;
    
    @Inject
    protected DBHelper dbHelper;
    
    protected TableHelper tClobMaster;
    protected TableHelper tClobDetail;
    
    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("CLOB_DETAIL");
        dbHelper.deleteAll("CLOB_MASTER");
        
        tClobMaster = new TableHelper(dbHelper, "CLOB_MASTER");
        tClobMaster.setColumns("CLOB_MASTER_ID", "CLOB_COLUMN", "NAME");
        
        tClobDetail = new TableHelper(dbHelper, "CLOB_DETAIL");
        tClobDetail.setColumns("CLOB_DETAIL_ID", "CLOB_MASTER_ID", "NAME");
    }
    
    protected void createDistinctClobFetchDataSet() throws Exception {
        tClobMaster.insert(1, "cm1 clob", "cm1");
        tClobMaster.insert(2, "cm2 clob", "cm2");
        tClobMaster.insert(3, "cm3 clob", "cm3");
    }
    
    protected void createDistinctClobFetchWithToManyJoin() throws Exception {
        tClobMaster.insert(1, "cm1 clob", "cm1");
        tClobMaster.insert(2, "cm2 clob", "cm2");
        tClobMaster.insert(3, "cm3 clob", "cm3");
        tClobDetail.insert(1, 1, "cd11");
        tClobDetail.insert(2, 2, "cd21");
        tClobDetail.insert(3, 2, "cd22");
        tClobDetail.insert(4, 3, "cd31");
    }

    public void testDistinctClobFetch() throws Exception {
        if (!accessStackAdapter.supportsLobInsertsAsStrings()) {
            return;
        }

        createDistinctClobFetchDataSet();

        SelectQuery noDistinct = new SelectQuery(ClobMaster.class);
        noDistinct.addOrdering(ClobMaster.NAME_PROPERTY, SortOrder.ASCENDING);

        SelectQuery distinct = new SelectQuery(ClobMaster.class);
        distinct.setDistinct(true);
        distinct.addOrdering(ClobMaster.NAME_PROPERTY, SortOrder.ASCENDING);

        List<?> noDistinctResult = context.performQuery(noDistinct);
        List<?> distinctResult = context.performQuery(distinct);

        assertEquals(3, noDistinctResult.size());
        assertEquals(noDistinctResult, distinctResult);
    }

    public void testDistinctClobFetchWithToManyJoin() throws Exception {
        if (!accessStackAdapter.supportsLobInsertsAsStrings()) {
            return;
        }

        createDistinctClobFetchWithToManyJoin();

        Expression qual = Expression.fromString("details.name like 'cd%'");
        SelectQuery query = new SelectQuery(ClobMaster.class, qual);
        List<?> result = context.performQuery(query);

        assertEquals(3, result.size());
    }
}
