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

package org.apache.cayenne.unit.jira;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships_clob.ClobMaster;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 */
public class CAY_115IT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.RELATIONSHIPS_CLOB_PROJECT);

    protected DataContext context;
    protected UnitDbAdapter accessStackAdapter;

    protected TableHelper tClobMaster;
    protected TableHelper tClobDetail;

    @BeforeEach
    public void setUp() throws Exception {
        context = env.dataContext();
        accessStackAdapter = env.getInstance(UnitDbAdapter.class);
        tClobMaster = env.table("CLOB_MASTER", "CLOB_MASTER_ID", "CLOB_COLUMN", "NAME");
        
        tClobDetail = env.table("CLOB_DETAIL", "CLOB_DETAIL_ID", "CLOB_MASTER_ID", "NAME");
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

    @Test
    public void distinctClobFetch() throws Exception {
        if (!accessStackAdapter.supportsLobInsertsAsStrings()) {
            return;
        }

        createDistinctClobFetchDataSet();

        ObjectSelect<ClobMaster> noDistinct = ObjectSelect.query(ClobMaster.class);
        noDistinct.orderBy(ClobMaster.NAME.asc());

        ObjectSelect<ClobMaster> distinct = ObjectSelect.query(ClobMaster.class);
        distinct.distinct();
        distinct.orderBy(ClobMaster.NAME.asc());

        List<?> noDistinctResult = context.performQuery(noDistinct);
        List<?> distinctResult = context.performQuery(distinct);

        assertEquals(3, noDistinctResult.size());
        assertEquals(noDistinctResult, distinctResult);
    }

    @Test
    public void distinctClobFetchWithToManyJoin() throws Exception {
        if (!accessStackAdapter.supportsLobInsertsAsStrings()) {
            return;
        }

        createDistinctClobFetchWithToManyJoin();

        Expression qual = ExpressionFactory.exp("details.name like 'cd%'");
        List<?> result = ObjectSelect.query(ClobMaster.class, qual).select(context);

        assertEquals(3, result.size());
    }
}
