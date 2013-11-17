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

import java.util.Collection;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.query.Query;

public class UnitTestDomain extends DataDomain {

    protected boolean blockingQueries;
    protected int queryCount;

    public UnitTestDomain(String name) {
        super(name);
    }

    public UnitTestDomain(String name, Map properties) {
        super(name, properties);
    }

    public void restartQueryCounter() {
        queryCount = 0;
    }

    public int getQueryCount() {
        return queryCount;
    }

    public boolean isBlockingQueries() {
        return blockingQueries;
    }

    public void setBlockingQueries(boolean blockingQueries) {
        this.blockingQueries = blockingQueries;
    }

    @Override
    QueryResponse onQueryNoFilters(ObjectContext originatingContext, Query query) {
        return new UnitTestDomainQueryAction(originatingContext, this, query).execute();
    }

    @Override
    public void performQueries(
            Collection<? extends Query> queries,
            OperationObserver callback) {
        checkQueryAllowed(queries);
        super.performQueries(queries, callback);
    }

    public void checkQueryAllowed(Collection<? extends Query> queries)
            throws AssertionFailedError {
        if (blockingQueries) {
            Assert.fail("Query is unexpected: " + queries);
        }

        queryCount++;
    }
}
