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

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.AbstractQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;

public class MockNoRoutingQuery extends AbstractQuery {

    protected boolean routed;

    @Override
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        this.routed = true;
    }

    @Override
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new UnsupportedOperationException();
    }

    public boolean isRouted() {
        return routed;
    }
}
