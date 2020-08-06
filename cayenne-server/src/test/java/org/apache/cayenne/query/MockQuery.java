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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;

public class MockQuery implements Query {

    protected boolean selecting;
    protected boolean routeCalled;

    public MockQuery(boolean selecting) {
        this.selecting = selecting;
    }

    public MockQuery() {
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {
        return DefaultQueryMetadata.defaultMetadata;
    }

    public boolean isRouteCalled() {
        return routeCalled;
    }

    public boolean isSelecting() {
        return selecting;
    }

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return null;
    }

    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        routeCalled = true;
    }
}
