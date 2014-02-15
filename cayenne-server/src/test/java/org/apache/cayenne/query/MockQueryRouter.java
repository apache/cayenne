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

package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.access.MockQueryEngine;
import org.apache.cayenne.access.QueryEngine;
import org.apache.cayenne.map.DataMap;

public class MockQueryRouter implements QueryRouter {

    protected List queries = new ArrayList();

    public void reset() {
        this.queries = new ArrayList();
    }

    public List getQueries() {
        return Collections.unmodifiableList(queries);
    }

    public int getQueryCount() {
        return queries.size();
    }

    public void route(QueryEngine engine, Query query, Query substitutedQuery) {
        queries.add(query);
    }

    public QueryEngine engineForDataMap(DataMap map) {
        return new MockQueryEngine();
    }
    
    @Override
    public QueryEngine engineForName(String name) {
        return new MockQueryEngine();
    }
}
