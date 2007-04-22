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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DerivedDbEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines a set of algorithms useful for a generic AccessStack.
 * 
 * @author Andrus Adamchik
 */
public abstract class AbstractAccessStack {

    private static Log logObj = LogFactory.getLog(AbstractAccessStack.class);

    // hardcoded dependent entities that should be excluded
    // if LOBs are not supported
    private static final String[] EXTRA_EXCLUDED_FOR_NO_LOB = new String[] {
        "CLOB_DETAIL"
    };

    protected CayenneResources resources;

    public AccessStackAdapter getAdapter(DataNode node) {
        return resources.getAccessStackAdapter(node.getAdapter().getClass());
    }

    protected abstract DataDomain getDomain();

    /**
     * Helper method that orders DbEntities to satisfy referential constraints and returns
     * an ordered list.
     */
    protected List dbEntitiesInInsertOrder(DataNode node, DataMap map) {
        List entities = new ArrayList(map.getDbEntities());

        // filter varios unsupported tests...

        // LOBs
        boolean excludeLOB = !getAdapter(node).supportsLobs();
        boolean excludeBinPK = !getAdapter(node).supportsBinaryPK();
        if (excludeLOB || excludeBinPK) {
            Iterator it = entities.iterator();
            List filtered = new ArrayList();
            while (it.hasNext()) {
                DbEntity ent = (DbEntity) it.next();

                if (ent instanceof DerivedDbEntity) {
                    continue;
                }

                // check for LOB attributes
                if (excludeLOB) {
                    if (Arrays.binarySearch(EXTRA_EXCLUDED_FOR_NO_LOB, ent.getName()) >= 0) {
                        continue;
                    }

                    boolean hasLob = false;
                    Iterator attrs = ent.getAttributes().iterator();
                    while (attrs.hasNext()) {
                        DbAttribute attr = (DbAttribute) attrs.next();
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
                    Iterator attrs = ent.getAttributes().iterator();
                    while (attrs.hasNext()) {
                        // check for BIN PK or FK to BIN Pk
                        DbAttribute attr = (DbAttribute) attrs.next();
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

        node.getEntitySorter().sortDbEntities(entities, false);
        return entities;
    }

    protected void deleteTestData(DataNode node, DataMap map) throws Exception {

        Connection conn = node.getDataSource().getConnection();
        List list = this.dbEntitiesInInsertOrder(node, map);
        try {
            if (conn.getAutoCommit()) {
                conn.setAutoCommit(false);
            }

            Statement stmt = conn.createStatement();

            ListIterator it = list.listIterator(list.size());
            while (it.hasPrevious()) {
                DbEntity ent = (DbEntity) it.previous();
                if (ent instanceof DerivedDbEntity) {
                    continue;
                }

                String deleteSql = "DELETE FROM " + ent.getName();

                try {
                    stmt.executeUpdate(deleteSql);
                }
                catch (SQLException e) {
                    throw new CayenneRuntimeException(
                            "Error deleting test data for entity '"
                                    + ent.getName()
                                    + "': "
                                    + e.getLocalizedMessage());
                }
            }
            conn.commit();
            stmt.close();
        }
        finally {
            conn.close();
        }
    }

    protected void dropSchema(DataNode node, DataMap map) throws Exception {
        Connection conn = node.getDataSource().getConnection();
        List list = dbEntitiesInInsertOrder(node, map);

        try {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet tables = md.getTables(null, null, "%", null);
            List allTables = new ArrayList();

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

            ListIterator it = list.listIterator(list.size());
            while (it.hasPrevious()) {
                DbEntity ent = (DbEntity) it.previous();
                if (!allTables.contains(ent.getName())) {
                    continue;
                }

                try {
                    String dropSql = node.getAdapter().dropTable(ent);
                    logObj.info(dropSql);
                    stmt.execute(dropSql);
                }
                catch (SQLException sqe) {
                    logObj.warn(
                            "Can't drop table " + ent.getName() + ", ignoring...",
                            sqe);
                }
            }

            getAdapter(node).droppedTables(conn, map);
        }
        finally {
            conn.close();
        }

    }

    protected void dropPKSupport(DataNode node, DataMap map) throws Exception {
        List filteredEntities = dbEntitiesInInsertOrder(node, map);
        node.getAdapter().getPkGenerator().dropAutoPk(node, filteredEntities);
    }

    protected void createPKSupport(DataNode node, DataMap map) throws Exception {
        List filteredEntities = dbEntitiesInInsertOrder(node, map);

        // remove derived...
        Iterator it = filteredEntities.iterator();
        while (it.hasNext()) {
            DbEntity e = (DbEntity) it.next();
            if (e instanceof DerivedDbEntity) {
                it.remove();
            }
        }

        node.getAdapter().getPkGenerator().createAutoPk(node, filteredEntities);
    }

    protected void createSchema(DataNode node, DataMap map) throws Exception {
        Connection conn = node.getDataSource().getConnection();

        try {
            getAdapter(node).willCreateTables(conn, map);
            Statement stmt = conn.createStatement();
            Iterator it = tableCreateQueries(node, map);
            while (it.hasNext()) {
                String query = (String) it.next();
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
    protected Iterator tableCreateQueries(DataNode node, DataMap map) throws Exception {
        DbAdapter adapter = node.getAdapter();
        DbGenerator gen = new DbGenerator(adapter, map, null, getDomain());

        List orderedEnts = dbEntitiesInInsertOrder(node, map);
        List queries = new ArrayList();

        // table definitions
        Iterator it = orderedEnts.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            if (ent instanceof DerivedDbEntity) {
                continue;
            }

            queries.add(adapter.createTable(ent));
        }

        // FK constraints

        it = orderedEnts.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            if (ent instanceof DerivedDbEntity
                    || !getAdapter(node).supportsFKConstraints(ent)) {
                continue;
            }

            List qs = gen.createConstraintsQueries(ent);
            queries.addAll(qs);
        }

        return queries.iterator();
    }
}
