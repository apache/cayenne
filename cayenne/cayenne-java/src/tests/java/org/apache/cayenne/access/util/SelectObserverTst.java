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

package org.apache.cayenne.access.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.cayenne.access.DataContextTestBase;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

/**
 * @author Andrei Adamchik
 * @deprecated since 1.2 as SelectObserver is also deprecated.
 */
public class SelectObserverTst extends DataContextTestBase {

    public void testResults() {
        SelectObserver observer = new SelectObserver();
        Expression qualifier = ExpressionFactory.matchExp("artistName", "artist2");
        SelectQuery query = new SelectQuery(Artist.class, qualifier);
        context.performQueries(Collections.singletonList(query), observer);

        List results = observer.getResults(query);
        assertNotNull(results);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Map);
    }

    /**
     * @deprecated Since 1.2 method being tested is deprecated.
     */
    public void testResultsAsObjectsOld() {
        SelectObserver observer = new SelectObserver();
        Expression qualifier = ExpressionFactory.matchExp("artistName", "artist2");
        SelectQuery query = new SelectQuery(Artist.class, qualifier);
        context.performQueries(Collections.singletonList(query), observer);

        List results = observer.getResultsAsObjects(context, query);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof Artist);
    }
}
