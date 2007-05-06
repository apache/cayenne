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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.query.Query;

public class MockDataNode extends DataNode {

    protected DataDomain domain;
    protected DataNode node;

    // mockup the actual results
    protected boolean replaceResults;
    protected Map results = new HashMap();
    protected int runCount;

    public static MockDataNode interceptNode(DataDomain domain, String nodeName) {
        DataNode node = domain.getNode(nodeName);
        if (node == null) {
            throw new IllegalArgumentException("No node for name: " + nodeName);
        }
        return interceptNode(domain, node);
    }

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
            resultConsumer.nextDataRows(query, (List) results.get(query));
        }
    }

    public void addDataMap(DataMap map) {
        node.addDataMap(map);
    }

    public boolean equals(Object obj) {
        return node.equals(obj);
    }

    public DbAdapter getAdapter() {
        return node.getAdapter();
    }

    public Collection getDataMaps() {
        return node.getDataMaps();
    }

    public DataSource getDataSource() {
        return node.getDataSource();
    }

    public String getDataSourceFactory() {
        return node.getDataSourceFactory();
    }

    public String getDataSourceLocation() {
        return node.getDataSourceLocation();
    }

    public EntityResolver getEntityResolver() {
        return node.getEntityResolver();
    }

    public EntitySorter getEntitySorter() {
        return node.getEntitySorter();
    }

    public String getName() {
        return node.getName();
    }

    public void removeDataMap(String mapName) {
        node.removeDataMap(mapName);
    }

    public void setAdapter(DbAdapter adapter) {
        node.setAdapter(adapter);
    }

    public void setDataMaps(Collection dataMaps) {
        node.setDataMaps(dataMaps);
    }

    public void setDataSource(DataSource dataSource) {
        node.setDataSource(dataSource);
    }

    public void setDataSourceFactory(String dataSourceFactory) {
        node.setDataSourceFactory(dataSourceFactory);
    }

    public void setDataSourceLocation(String dataSourceLocation) {
        node.setDataSourceLocation(dataSourceLocation);
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        node.setEntityResolver(entityResolver);
    }

    public void setEntitySorter(EntitySorter entitySorter) {
        node.setEntitySorter(entitySorter);
    }

    public void setName(String name) {
        node.setName(name);
    }

    public void shutdown() {
        node.shutdown();
    }

    public String toString() {
        return node.toString();
    }

}
