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

import org.apache.cayenne.SelectResult;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.BatchTranslator;
import org.apache.cayenne.access.translator.ProcedureTranslator;
import org.apache.cayenne.access.translator.SQLTemplateTranslator;
import org.apache.cayenne.access.translator.SelectTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.UpdateBatchQuery;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockDataNode extends DataNode {

    protected DataDomain domain;
    protected DataNode node;
    protected boolean wasDefaultNode;

    // mockup the actual results
    protected boolean replaceResults;
    protected Map results = new HashMap();
    protected int runCount;

    public static MockDataNode interceptNode(DataDomain domain, DataNode node) {
        MockDataNode mockNode = new MockDataNode(node);
        mockNode.domain = domain;
        mockNode.wasDefaultNode = domain.getDefaultNode() == node;

        domain.removeDataNode(node.getName());
        domain.addNode(mockNode);

        // queries for DataMaps not linked to a node are routed via the default node, so it must be intercepted as well
        if (mockNode.wasDefaultNode) {
            domain.setDefaultNode(mockNode);
        }

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

        if (wasDefaultNode) {
            domain.setDefaultNode(node);
        }
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
    public void execute(List<StatementContext> statementContexts) {
        runCount += statementContexts.size();

        if (replaceResults) {
            // stick preset results to the statements
            for (StatementContext statementContext : statementContexts) {
                statementContext.addResult(new SelectResult<>((List) results.get(statementContext.getQuery())));
            }
        } else {
            // execute here, with the delegate supplying all the parts
            super.execute(statementContexts);
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
    public PkGenerator getPkGenerator() {
        return node.getPkGenerator();
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
    public EntityResolver getEntityResolver() {
        return node.getEntityResolver();
    }

    @Override
    public EntitySorter getEntitySorter() {
        return node.getEntitySorter();
    }

    @Override
    public SQLLogger getSqlLogger() {
        return node.getSqlLogger();
    }

    @Override
    public SchemaUpdateStrategy getSchemaUpdateStrategy() {
        return node.getSchemaUpdateStrategy();
    }

    @Override
    public RowReaderFactory getRowReaderFactory() {
        return node.getRowReaderFactory();
    }

    @Override
    public SelectTranslator getSelectTranslator() {
        return node.getSelectTranslator();
    }

    @Override
    public ProcedureTranslator getProcedureTranslator() {
        return node.getProcedureTranslator();
    }

    @Override
    public SQLTemplateTranslator getSqlTemplateTranslator() {
        return node.getSqlTemplateTranslator();
    }

    @Override
    public BatchTranslator<InsertBatchQuery> getInsertBatchTranslator() {
        return node.getInsertBatchTranslator();
    }

    @Override
    public BatchTranslator<UpdateBatchQuery> getUpdateBatchTranslator() {
        return node.getUpdateBatchTranslator();
    }

    @Override
    public BatchTranslator<DeleteBatchQuery> getDeleteBatchTranslator() {
        return node.getDeleteBatchTranslator();
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
    public void setEntityResolver(EntityResolver entityResolver) {
        node.setEntityResolver(entityResolver);
    }

    @Override
    public void setEntitySorter(EntitySorter entitySorter) {
        node.setEntitySorter(entitySorter);
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
