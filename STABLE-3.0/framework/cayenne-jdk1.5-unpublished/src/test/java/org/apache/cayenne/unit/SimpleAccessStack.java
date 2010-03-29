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

package org.apache.cayenne.unit;

import java.util.Map;

import org.apache.art.StringET1ExtendedType;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.UnitTestDomain;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.query.Query;

/**
 * Default implementation of the AccessStack that has a single DataNode per DataMap.
 * 
 */
public class SimpleAccessStack extends AbstractAccessStack implements AccessStack {

    protected UnitTestDomain domain;
    protected DataSetFactory dataSetFactory;

    public SimpleAccessStack(CayenneResources resources, DataSetFactory dataSetFactory,
            DataMap[] maps) throws Exception {

        this.dataSetFactory = dataSetFactory;
        this.resources = resources;
        this.domain = new UnitTestDomain("domain");
        domain.setEventManager(new EventManager(2));
        for (DataMap map : maps) {
            initNode(map);
        }
    }

    @Override
    protected DataDomain getDomain() {
        return domain;
    }

    protected void initNode(DataMap map) throws Exception {
        DataNode node = resources.newDataNode(map.getName());

        // setup test extended types
        node.getAdapter().getExtendedTypes().registerType(new StringET1ExtendedType());

        // tweak mapping with a delegate
        for (Procedure proc : map.getProcedures()) {
            getAdapter(node).tweakProcedure(proc);
        }

        node.addDataMap(map);

        // use shared data source in all cases but the multi-node...

        if (MultiNodeCase.NODE1.equals(node.getName())
                || MultiNodeCase.NODE2.equals(node.getName())) {
            node.setDataSource(resources.createDataSource());
        }
        else {
            node.setDataSource(resources.getDataSource());
        }

        node.setSchemaUpdateStrategy(new SkipSchemaUpdateStrategy());
        domain.addNode(node);
    }

    /**
     * Returns DataDomain for this AccessStack.
     */
    public UnitTestDomain getDataDomain() {
        return domain;
    }

    public void createTestData(Class<?> testCase, String testName, Map parameters)
            throws Exception {
        Query query = dataSetFactory.getDataSetQuery(testCase, testName, parameters);
        getDataDomain().onQuery(null, query);
    }

    /**
     * Deletes all data from the database tables mentioned in the DataMap.
     */
    public void deleteTestData() throws Exception {
        for (DataNode node : domain.getDataNodes()) {
            deleteTestData(node, node.getDataMaps().iterator().next());
        }
    }

    /** Drops all test tables. */
    public void dropSchema() throws Exception {
        for (DataNode node : domain.getDataNodes()) {
            dropSchema(node, node.getDataMaps().iterator().next());
        }
    }

    /**
     * Creates all test tables in the database.
     */
    public void createSchema() throws Exception {
        for (DataNode node : domain.getDataNodes()) {
            createSchema(node, node.getDataMaps().iterator().next());
        }
    }

    public void dropPKSupport() throws Exception {
        for (DataNode node : domain.getDataNodes()) {
            dropPKSupport(node, node.getDataMaps().iterator().next());
        }
    }

    /**
     * Creates primary key support for all node DbEntities. Will use its facilities
     * provided by DbAdapter to generate any necessary database objects and data for
     * primary key support.
     */
    public void createPKSupport() throws Exception {
        for (DataNode node : domain.getDataNodes()) {
            createPKSupport(node, node.getDataMaps().iterator().next());
        }
    }
}
