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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.access.UnitTestDomain;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.ashwood.AshwoodEntitySorter;
import org.apache.cayenne.cache.MapQueryCache;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.testdo.testmap.StringET1ExtendedType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Default implementation of the AccessStack that has a single DataNode per DataMap.
 */
public class SimpleAccessStack implements AccessStack {

    private static Log logger = LogFactory.getLog(SimpleAccessStack.class);

    // hardcoded dependent entities that should be excluded
    // if LOBs are not supported
    private static final String[] EXTRA_EXCLUDED_FOR_NO_LOB = new String[] {
        "CLOB_DETAIL"
    };

    protected CayenneResources resources;
    protected UnitTestDomain domain;

    public SimpleAccessStack(CayenneResources resources, DataMap[] maps) throws Exception {

        this.resources = resources;
        this.domain = new UnitTestDomain("domain");
        domain.setEventManager(new DefaultEventManager(2));
        domain.setEntitySorter(new AshwoodEntitySorter());
        domain.setQueryCache(new MapQueryCache(50));

        for (DataMap map : maps) {
            initNode(map);
        }
    }

    public AccessStackAdapter getAdapter(DataNode node) {
        return resources.getAccessStackAdapter(node.getAdapter().getClass().getName());
    }

    private void initNode(DataMap map) throws Exception {
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

    /**
     * Helper method that orders DbEntities to satisfy referential constraints and returns
     * an ordered list.
     */
    private List<DbEntity> dbEntitiesInInsertOrder(DataNode node, DataMap map) {
        List<DbEntity> entities = new ArrayList<DbEntity>(map.getDbEntities());

        // filter various unsupported tests...

        // LOBs
        boolean excludeLOB = !getAdapter(node).supportsLobs();
        boolean excludeBinPK = !getAdapter(node).supportsBinaryPK();
        if (excludeLOB || excludeBinPK) {

            List<DbEntity> filtered = new ArrayList<DbEntity>();

            for (DbEntity ent : entities) {

                // check for LOB attributes
                if (excludeLOB) {
                    if (Arrays.binarySearch(EXTRA_EXCLUDED_FOR_NO_LOB, ent.getName()) >= 0) {
                        continue;
                    }

                    boolean hasLob = false;
                    for (final DbAttribute attr : ent.getAttributes()) {
                        if (attr.getType() == Types.BLOB || attr.getType() == Types.CLOB) {
                            hasLob = true;
                            break;
                        }
                    }

                    if (hasLob) {
                        continue;
                    }
                }

                // check for BIN PK
                if (excludeBinPK) {
                    boolean skip = false;
                    for (final DbAttribute attr : ent.getAttributes()) {
                        // check for BIN PK or FK to BIN Pk
                        if (attr.getType() == Types.BINARY
                                || attr.getType() == Types.VARBINARY
                                || attr.getType() == Types.LONGVARBINARY) {

                            if (attr.isPrimaryKey() || attr.isForeignKey()) {
                                skip = true;
                                break;
                            }
                        }
                    }

                    if (skip) {
                        continue;
                    }
                }

                filtered.add(ent);
            }

            entities = filtered;
        }

        domain.getEntitySorter().sortDbEntities(entities, false);
        return entities;
    }

    private void deleteTestData(DataNode node, DataMap map) throws Exception {

        Connection conn = node.getDataSource().getConnection();
        List<DbEntity> list = dbEntitiesInInsertOrder(node, map);
        try {
            if (conn.getAutoCommit()) {
                conn.setAutoCommit(false);
            }

            Statement stmt = conn.createStatement();

            ListIterator<DbEntity> it = list.listIterator(list.size());
            while (it.hasPrevious()) {
                DbEntity ent = it.previous();

                boolean status;
                if (ent.getDataMap() != null
                        && ent.getDataMap().isQuotingSQLIdentifiers()) {
                    status = true;
                }
                else {
                    status = false;
                }

                QuotingStrategy strategy = getAdapter(node).getQuotingStrategy(status);

                String deleteSql = "DELETE FROM " + strategy.quoteString(ent.getName());

                try {
                    stmt.executeUpdate(deleteSql);
                }
                catch (SQLException e) {
                    try {
                        Collection<String> deleteTableSql = node
                                .getAdapter()
                                .dropTableStatements(ent);
                        stmt.executeUpdate(deleteTableSql.iterator().next());
                        String createTableSql = node.getAdapter().createTable(ent);
                        stmt.executeUpdate(createTableSql);
                    }
                    catch (SQLException e1) {
                        throw new CayenneRuntimeException(
                                "Error deleting test data for entity '"
                                        + ent.getName()
                                        + "': "
                                        + e.getLocalizedMessage());
                    }
                }
            }
            conn.commit();
            stmt.close();
        }
        finally {
            conn.close();
        }
    }

    private void dropSchema(DataNode node, DataMap map) throws Exception {
        Connection conn = node.getDataSource().getConnection();
        List<DbEntity> list = dbEntitiesInInsertOrder(node, map);

        try {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet tables = md.getTables(null, null, "%", null);
            List<String> allTables = new ArrayList<String>();

            while (tables.next()) {
                // 'toUpperCase' is needed since most databases
                // are case insensitive, and some will convert names to lower case
                // (PostgreSQL)
                String name = tables.getString("TABLE_NAME");
                if (name != null)
                    allTables.add(name.toUpperCase());
            }
            tables.close();

            getAdapter(node).willDropTables(conn, map, allTables);

            // drop all tables in the map
            Statement stmt = conn.createStatement();

            ListIterator<DbEntity> it = list.listIterator(list.size());
            while (it.hasPrevious()) {
                DbEntity ent = it.previous();
                if (!allTables.contains(ent.getName().toUpperCase())) {
                    continue;
                }

                for (String dropSql : node.getAdapter().dropTableStatements(ent)) {
                    try {
                        logger.info(dropSql);
                        stmt.execute(dropSql);
                    }
                    catch (SQLException sqe) {
                        logger.warn(
                                "Can't drop table " + ent.getName() + ", ignoring...",
                                sqe);
                    }
                }
            }

            getAdapter(node).droppedTables(conn, map);
        }
        finally {
            conn.close();
        }

    }

    private void dropPKSupport(DataNode node, DataMap map) throws Exception {
        List<DbEntity> filteredEntities = dbEntitiesInInsertOrder(node, map);
        node.getAdapter().getPkGenerator().dropAutoPk(node, filteredEntities);
    }

    private void createPKSupport(DataNode node, DataMap map) throws Exception {
        List<DbEntity> filteredEntities = dbEntitiesInInsertOrder(node, map);
        node.getAdapter().getPkGenerator().createAutoPk(node, filteredEntities);
    }

    private void createSchema(DataNode node, DataMap map) throws Exception {
        Connection conn = node.getDataSource().getConnection();

        try {
            getAdapter(node).willCreateTables(conn, map);
            Statement stmt = conn.createStatement();

            for (String query : tableCreateQueries(node, map)) {
                QueryLogger.logQuery(query, Collections.EMPTY_LIST);
                stmt.execute(query);
            }
            getAdapter(node).createdTables(conn, map);
        }
        finally {
            conn.close();
        }
    }

    /**
     * Returns iterator of preprocessed table create queries.
     */
    private Collection<String> tableCreateQueries(DataNode node, DataMap map)
            throws Exception {
        DbAdapter adapter = node.getAdapter();
        DbGenerator gen = new DbGenerator(adapter, map, null, domain);

        List<DbEntity> orderedEnts = dbEntitiesInInsertOrder(node, map);
        List<String> queries = new ArrayList<String>();

        // table definitions
        for (DbEntity ent : orderedEnts) {
            queries.add(adapter.createTable(ent));
        }

        // FK constraints
        for (DbEntity ent : orderedEnts) {
            if (!getAdapter(node).supportsFKConstraints(ent)) {
                continue;
            }

            List<String> qs = gen.createConstraintsQueries(ent);
            queries.addAll(qs);
        }

        return queries;
    }
}
