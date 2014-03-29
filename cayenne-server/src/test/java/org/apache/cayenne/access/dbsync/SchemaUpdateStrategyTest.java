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
package org.apache.cayenne.access.dbsync;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.MockOperationObserver;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.DEFAULT_PROJECT)
public class SchemaUpdateStrategyTest extends ServerCase {

    @Inject
    private DataNode node;

    @Inject
    private DbAdapter adapter;
    
    @Inject
    private ServerCaseDataSourceFactory dataSourceFactory;
    
    @Inject
    private JdbcEventLogger jdbcEventLogger;

    public void testDBGeneratorStrategy() throws Exception {

        String template = "SELECT #result('id' 'int') FROM SUS1";
        SQLTemplate query = new SQLTemplate(Object.class, template);

        DataMap map = node.getEntityResolver().getDataMap("sus-map");
        DataNode dataNode = createDataNode(map);
        int sizeDB = getNameTablesInDB(dataNode).size();
        MockOperationObserver observer = new MockOperationObserver();
        try {

            generateDBWithDBGeneratorStrategy(dataNode, query, observer);
            int sizeDB2 = getNameTablesInDB(dataNode).size();
            assertEquals(2, sizeDB2 - sizeDB);
            dataNode.performQueries(Collections.singletonList((Query) query), observer);
            int sizeDB3 = getNameTablesInDB(dataNode).size();
            assertEquals(sizeDB2, sizeDB3);
        }
        finally {
            DataNode dataNode2 = createDataNode(map);
            dataNode2.setSchemaUpdateStrategy((SchemaUpdateStrategy) Class.forName(
                    dataNode2.getSchemaUpdateStrategyName()).newInstance());
            dropTables(map, dataNode2, observer);
        }
        assertEquals(getNameTablesInDB(dataNode).size(), sizeDB);
    }

    public void testThrowOnPartialStrategyTableNoExist() throws Exception {

        String template = "SELECT #result('ARTIST_ID' 'int') FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Object.class, template);
        DataMap map = node.getEntityResolver().getDataMap("sus-map");
        MockOperationObserver observer = new MockOperationObserver();
        DataNode dataNode = createDataNode(map);

        setStrategy(ThrowOnPartialSchemaStrategy.class.getName(), dataNode);

        try {
            dataNode.performQueries(Collections.singletonList((Query) query), observer);
        }
        catch (CayenneRuntimeException e) {
            assertNotNull(e);
        }

        try {
            dataNode.performQueries(Collections.singletonList((Query) query), observer);
        }
        catch (CayenneRuntimeException e) {
            assertNotNull(e);
        }
    }

    public void testThrowOnPartialStrategyTableExist() throws Exception {
        tableExistfForThrowOnPartialAndMixStrategy(ThrowOnPartialSchemaStrategy.class
                .getName());
    }

    public void testThrowOnPartialStrategyWithOneTable() throws Exception {
        withOneTableForThrowOnPartialAndMixStrategy(ThrowOnPartialSchemaStrategy.class
                .getName());
    }

    public void testMixedStrategyTableNoExist() throws Exception {

        String template = "SELECT #result('id' 'int') FROM SUS1";
        SQLTemplate query = new SQLTemplate(Object.class, template);
        DataMap map = node.getEntityResolver().getDataMap("sus-map");
        DataNode dataNode = createDataNode(map);
        int sizeDB = getNameTablesInDB(dataNode).size();
        MockOperationObserver observer = new MockOperationObserver();

        setStrategy(ThrowOnPartialOrCreateSchemaStrategy.class.getName(), dataNode);

        try {
            dataNode.performQueries(Collections.singletonList((Query) query), observer);
            Map<String, Boolean> nameTables = getNameTablesInDB(dataNode);
            assertTrue(nameTables.get("sus1") != null || nameTables.get("SUS1") != null);
            int sizeDB2 = getNameTablesInDB(dataNode).size();
            assertEquals(2, sizeDB2 - sizeDB);
            dataNode.performQueries(Collections.singletonList((Query) query), observer);
            int sizeDB3 = getNameTablesInDB(dataNode).size();
            assertEquals(sizeDB2, sizeDB3);
        }
        finally {
            DataNode dataNode2 = createDataNode(map);
            dataNode2.setSchemaUpdateStrategy((SchemaUpdateStrategy) Class.forName(
                    dataNode2.getSchemaUpdateStrategyName()).newInstance());
            dropTables(map, dataNode2, observer);
        }
        assertEquals(getNameTablesInDB(dataNode).size(), sizeDB);

    }

    public void testMixedStrategyTableExist() throws Exception {
        tableExistfForThrowOnPartialAndMixStrategy(ThrowOnPartialOrCreateSchemaStrategy.class
                .getName());
    }

    public void testMixedStrategyWithOneTable() throws Exception {
        withOneTableForThrowOnPartialAndMixStrategy(ThrowOnPartialOrCreateSchemaStrategy.class
                .getName());
    };

    public void testNoStandartSchema() {
        String template = "SELECT #result('ARTIST_ID' 'int') FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Object.class, template);
        MockOperationObserver observer = new MockOperationObserver();
        DataMap map = node.getEntityResolver().getDataMap("sus-map");
        DataNode dataNode = createDataNode(map);

        setStrategy(TstSchemaUpdateStrategy.class.getName(), dataNode);

        dataNode.performQueries(Collections.singletonList((Query) query), observer);
        assertTrue(dataNode.getSchemaUpdateStrategy() instanceof TstSchemaUpdateStrategy);
    }

    private void withOneTableForThrowOnPartialAndMixStrategy(String strategy) {
        DbEntity entity = null;
        String template = "SELECT #result('ARTIST_ID' 'int') FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Object.class, template);
        DataMap map = node.getEntityResolver().getDataMap("sus-map");
        MockOperationObserver observer = new MockOperationObserver();
        DataNode dataNode = createDataNode(map);

        DataNode dataNode2 = createDataNode(map);

        try {

            int sizeDB = getNameTablesInDB(dataNode).size();
            entity = createOneTable(dataNode);
            int sizeDB2 = getNameTablesInDB(dataNode).size();
            assertEquals(1, sizeDB2 - sizeDB);
            setStrategy(strategy, dataNode2);
            dataNode2.performQueries(Collections.singletonList((Query) query), observer);

        }
        catch (CayenneRuntimeException e) {
            assertNotNull(e);
        }
        try {
            dataNode2.performQueries(Collections.singletonList((Query) query), observer);
        }
        catch (CayenneRuntimeException e) {
            assertNotNull(e);
        }
        finally {

            if (entity != null) {

                Collection<String> template2 = dataNode.getAdapter().dropTableStatements(
                        entity);
                Iterator<String> it = template2.iterator();
                List<Query> list = new ArrayList<Query>();
                while (it.hasNext()) {
                    SQLTemplate q = new SQLTemplate(Object.class, it.next());
                    list.add(q);
                }
                dataNode.performQueries(list, observer);
            }
        }
    }

    private void tableExistfForThrowOnPartialAndMixStrategy(String strategy)
            throws Exception {

        String template = "SELECT #result('ARTIST_ID' 'int') FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Object.class, template);
        DataMap map = node.getEntityResolver().getDataMap("sus-map");
        MockOperationObserver observer = new MockOperationObserver();
        DataNode dataNode = createDataNode(map);
        int sizeDB = getNameTablesInDB(dataNode).size();
        generateDBWithDBGeneratorStrategy(dataNode, query, observer);
        int sizeDB2 = getNameTablesInDB(dataNode).size();
        assertEquals(2, sizeDB2 - sizeDB);
        try {
            DataNode dataNode2 = createDataNode(map);
            setStrategy(strategy, dataNode2);
            dataNode2.performQueries(Collections.singletonList((Query) query), observer);
        }
        finally {
            dropTables(map, dataNode, observer);
        }

    }

    private DbEntity createOneTable(DataNode dataNode) {
        DataMap map = node.getEntityResolver().getDataMap("sus-map");
        Collection<DbEntity> ent = map.getDbEntities();
        DbEntity entity = ent.iterator().next();
        String template = dataNode.getAdapter().createTable(entity);

        SQLTemplate query = new SQLTemplate(Object.class, template);
        MockOperationObserver observer = new MockOperationObserver();

        setStrategy(null, dataNode);

        dataNode.performQueries(Collections.singletonList((Query) query), observer);
        return entity;
    }

    private DataNode createDataNode(DataMap map) {
        Collection<DataMap> colection = new ArrayList<DataMap>();
        colection.add(map);
        DataNode dataNode = new DataNode();
        dataNode.setJdbcEventLogger(jdbcEventLogger);
        dataNode.setDataMaps(colection);
        dataNode.setAdapter(adapter);
        dataNode.setDataSource(dataSourceFactory.getSharedDataSource());
        dataNode.setDataSourceFactory(node.getDataSourceFactory());
        dataNode.setSchemaUpdateStrategyName(node.getSchemaUpdateStrategyName());
        dataNode.setRowReaderFactory(node.getRowReaderFactory());
        dataNode.setBatchTranslatorFactory(node.getBatchTranslatorFactory());
        dataNode.setEntityResolver(new EntityResolver(colection));
        return dataNode;
    }

    private void generateDBWithDBGeneratorStrategy(
            DataNode dataNode,
            SQLTemplate query,
            MockOperationObserver observer) {

        setStrategy(CreateIfNoSchemaStrategy.class.getName(), dataNode);

        dataNode.performQueries(Collections.singletonList((Query) query), observer);
        Map<String, Boolean> nameTables = getNameTablesInDB(dataNode);
        assertTrue(nameTables.get("sus1") != null || nameTables.get("SUS1") != null);

    }

    private void setStrategy(String name, DataNode dataNode) {
        dataNode.setSchemaUpdateStrategyName(name);
        try {
            dataNode.setSchemaUpdateStrategy((SchemaUpdateStrategy) Class.forName(
                    dataNode.getSchemaUpdateStrategyName()).newInstance());
        }
        catch (Exception e) {
            throw new CayenneRuntimeException(e);
        }
    }

    private void dropTables(DataMap map, DataNode dataNode, MockOperationObserver observer) {
        Collection<DbEntity> ent = map.getDbEntities();
        Iterator<DbEntity> iterator = ent.iterator();
        while (iterator.hasNext()) {
            Collection<String> collectionDrop = dataNode
                    .getAdapter()
                    .dropTableStatements(iterator.next());
            for (String s : collectionDrop) {
                SQLTemplate queryDrop = new SQLTemplate(Object.class, s);
                dataNode.performQueries(
                        Collections.singletonList((Query) queryDrop),
                        observer);
            }
        }
    }

    private Map<String, Boolean> getNameTablesInDB(DataNode dataNode) {
        String tableLabel = dataNode.getAdapter().tableTypeForTable();
        Connection con = null;
        Map<String, Boolean> nameTables = new HashMap<String, Boolean>();
        try {
            con = dataNode.getDataSource().getConnection();
            ResultSet rs = con.getMetaData().getTables(null, null, "%", new String[] {
                tableLabel
            });
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                nameTables.put(name, false);
            }
            rs.close();
        }
        catch (SQLException e) {
            throw new CayenneRuntimeException(e);
        }
        finally {
            try {
                con.close();
            }
            catch (SQLException e) {
                throw new CayenneRuntimeException(e);
            }
        }
        return nameTables;
    }
}
