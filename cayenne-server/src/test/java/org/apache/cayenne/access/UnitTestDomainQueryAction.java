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

package org.apache.cayenne.access;

import java.util.Collection;
import java.util.Collections;

import junit.framework.AssertionFailedError;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.query.Query;

/**
 * A DataDomainQueryAction that can be configured to block queries that are not run from
 * cache.
 */
public class UnitTestDomainQueryAction extends DataDomainQueryAction {

    public UnitTestDomainQueryAction(ObjectContext context, UnitTestDomain domain,
            Query query) {
        super(context, domain, query);
    }

    /**
     * Exposing super as a public method.
     */
    @Override
    public QueryResponse execute() {
        return super.execute();
    }

    @Override
    void runQueryInTransaction() {
        checkQueryAllowed(Collections.singleton(query));
        super.runQueryInTransaction();
    }

    protected void checkQueryAllowed(Collection<? extends Query> queries)
            throws AssertionFailedError {
        ((UnitTestDomain) domain).checkQueryAllowed(queries);
    }
}
