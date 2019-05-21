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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

public class MockDataNode extends DataNode {

    protected DataDomain domain;
    protected DataNode node;

    // mockup the actual results
    protected boolean replaceResults;
    protected Map results = new HashMap();
    protected int runCount;

    public static MockDataNode interceptNode(DataDomain domain, DataNode node) {
        MockDataNode mockNode = new MockDataNode(node);
        mockNode.domain = domain;
        domain.removeDataNode(node.getName());
        domain.addNode(mockNode);
        return mockNode;
    }

    public MockDataNode(DataNode node) {
        this.node = node;
    }

    public void stopInterceptNode() {
        if (domain == null) {
            throw new IllegalStateException("No domain set.");
        }

        domain.removeDataNode(getName());
        domain.addNode(node);
    }

    public void reset() {
        runCount = 0;
        results.clear();
    }

    public int getRunCount() {
        return runCount;
    }

    public void addExpectedResult(Query query, List result) {
        replaceResults = true;
        results.put(query, result);
    }

    @Override
    public void performQueries(Collection queries, OperationObserver resultConsumer) {
        runCount += queries.size();

        if (replaceResults) {
            initWithPresetResults(queries, resultConsumer);
        }
        else {
            node.performQueries(queries, resultConsumer);
        }
    }

    private void initWithPresetResults(
            Collection queries,
            OperationObserver resultConsumer) {

        // stick preset results to the consumer
        Iterator it = queries.iterator();
        while (it.hasNext()) {
            Query query = (Query) it.next();
            resultConsumer.nextRows(query, (List) results.get(query));
        }
    }

    @Override
    public void addDataMap(DataMap map) {
        node.addDataMap(map);
    }

    @Override
    public boolean equals(Object obj) {
        return node.equals(obj);
    }

    @Override
    public DbAdapter getAdapter() {
        return node.getAdapter();
    }

    @Override
    public Collection getDataMaps() {
        return node.getDataMaps();
    }

    @Override
    public DataSource getDataSource() {
        return node.getDataSource();
    }

    @Override
    public String getDataSourceFactory() {
        return node.getDataSourceFactory();
    }

    @Override
    public EntityResolver getEntityResolver() {
        return node.getEntityResolver();
    }

    @Override
    public String getName() {
        return node.getName();
    }

    @Override
    public void removeDataMap(String mapName) {
        node.removeDataMap(mapName);
    }

    @Override
    public void setAdapter(DbAdapter adapter) {
        node.setAdapter(adapter);
    }

    @Override
    public void setDataMaps(Collection dataMaps) {
        node.setDataMaps(dataMaps);
    }

    @Override
    public void setDataSource(DataSource dataSource) {
        node.setDataSource(dataSource);
    }

    @Override
    public void setDataSourceFactory(String dataSourceFactory) {
        node.setDataSourceFactory(dataSourceFactory);
    }

    @Override
    public void setEntityResolver(EntityResolver entityResolver) {
        node.setEntityResolver(entityResolver);
    }

    @Override
    public void setName(String name) {
        node.setName(name);
    }

    @Override
    public String toString() {
        return node.toString();
    }

}
