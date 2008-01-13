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

import java.util.Collections;

import org.apache.cayenne.access.MockOperationObserver;
import org.apache.cayenne.unit.CayenneCase;

public abstract class SelectQueryBase extends CayenneCase {

    protected SelectQuery query;
    protected MockOperationObserver opObserver;

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
        populateTables();
        query = new SelectQuery();
        opObserver = new MockOperationObserver();
    }

    protected void performQuery() throws Exception {
        // run query
        getDomain().performQueries(Collections.singletonList(getQuery()), opObserver);
    }

    protected Query getQuery() {
        return query;
    }

    protected abstract void populateTables() throws java.lang.Exception;
}
