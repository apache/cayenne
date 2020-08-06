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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.tx.BaseTransaction;

/**
 * A query engine used for unit testing. Returns canned results instead of doing the
 * actual query.
 * 
 */
public class MockQueryEngine implements QueryEngine {

    // mockup the actual results
    protected Map results = new HashMap();
    protected EntityResolver entityResolver;
    protected int runCount;

    public MockQueryEngine() {
    }

    public MockQueryEngine(QueryEngine engine) {
        this(engine.getEntityResolver());
    }

    public MockQueryEngine(EntityResolver resolver) {
        this.entityResolver = resolver;
    }

    public void reset() {
        runCount = 0;
        results.clear();
    }

    public int getRunCount() {
        return runCount;
    }

    public void addExpectedResult(Query query, List result) {
        results.put(query, result);
    }

    public void performQueries(
            Collection queries,
            OperationObserver resultConsumer,
            BaseTransaction transaction) {
        initWithPresetResults(queries, resultConsumer);
    }

    public void performQueries(Collection queries, OperationObserver resultConsumer) {
        initWithPresetResults(queries, resultConsumer);
    }

    private void initWithPresetResults(
            Collection queries,
            OperationObserver resultConsumer) {

        runCount++;

        // stick preset results to the consumer
        Iterator it = queries.iterator();
        while (it.hasNext()) {
            Query query = (Query) it.next();
            resultConsumer.nextRows(query, (List) results.get(query));
        }
    }

    public DataNode lookupDataNode(DataMap dataMap) {
        return null;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public Collection getDataMaps() {
        return (entityResolver != null)
                ? entityResolver.getDataMaps()
                : Collections.EMPTY_LIST;
    }
}
