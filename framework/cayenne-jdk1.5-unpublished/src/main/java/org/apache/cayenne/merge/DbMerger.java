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
package org.apache.cayenne.merge;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.DbLoaderDelegate;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;

/**
 * Traverse a {@link DataNode} and a {@link DataMap} and create a group of
 * {@link MergerToken}s to alter the {@link DataNode} datastore to match the
 * {@link DataMap}.
 * 
 * @author halset
 */
public class DbMerger {

    private MergerFactory factory;

    /**
     * Create and return a {@link List} of {@link MergerToken}s to alter the given
     * {@link DataNode} to match the given {@link DataMap}
     */
    public List createMergeTokens(DataNode dataNode, DataMap dataMap) {
        return createMergeTokens(dataNode.getAdapter(), dataNode.getDataSource(), dataMap);
    }

    /**
     * Create and return a {@link List} of {@link MergerToken}s to alter the given
     * {@link DataNode} to match the given {@link DataMap}
     */
    public List createMergeTokens(DbAdapter adapter, DataSource dataSource, DataMap dataMap) {
        factory = adapter.mergerFactory();

        List tokens = new ArrayList();
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();

            DbLoader dbLoader = new DbLoader(conn, adapter, new LoaderDelegate());
            DataMap detectedDataMap = dbLoader.loadDataMapFromDB(
                    null,
                    null,
                    new DataMap());

            Map dbEntityToDropByName = new HashMap(detectedDataMap.getDbEntityMap());

            for (Iterator it = dataMap.getDbEntities().iterator(); it.hasNext();) {
                DbEntity dbEntity = (DbEntity) it.next();

                String tableName = dbEntity.getName();

                // look for table
                DbEntity detectedEntity = findDbEntity(detectedDataMap, tableName);
                if (detectedEntity == null) {
                    tokens.add(factory.createCreateTableToDb(dbEntity));
                    continue;
                }
                dbEntityToDropByName.remove(detectedEntity.getName());

                checkRows(tokens, dbEntity, detectedEntity);
            }

            // drop table
            // TODO: support drop table. currently, too many tables are marked for drop
            /*
             * for (Iterator it = dbEntityToDropByName.values().iterator(); it.hasNext();) {
             * DbEntity e = (DbEntity) it.next();
             * tokens.addToken(factory.createDropTable(e)); }
             */

        }
        catch (SQLException e) {
            throw new CayenneRuntimeException("", e);
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (SQLException e) {
                }
            }
        }

        return tokens;
    }

    private void checkRows(List tokens, DbEntity dbEntity, DbEntity detectedEntity) {

        // columns to drop
        for (Iterator it = detectedEntity.getAttributes().iterator(); it.hasNext();) {
            DbAttribute detected = (DbAttribute) it.next();
            if (findDbAttribute(dbEntity, detected.getName()) == null) {
                tokens.add(factory.createDropColumToDb(dbEntity, detected));
            }
        }

        // columns to add or modify
        for (Iterator it = dbEntity.getAttributes().iterator(); it.hasNext();) {
            DbAttribute attr = (DbAttribute) it.next();
            String columnName = attr.getName().toUpperCase();

            DbAttribute detected = findDbAttribute(detectedEntity, columnName);

            if (detected == null) {
                tokens.add(factory.createAddColumnToDb(dbEntity, attr));
                if (attr.isMandatory()) {
                    // TODO: default value
                    tokens.add(factory.createSetNotNullToDb(dbEntity, attr));
                }
                continue;
            }

            // check for not null
            if (attr.isMandatory() != detected.isMandatory()) {
                if (attr.isMandatory()) {
                    tokens.add(factory.createSetNotNullToDb(dbEntity, attr));
                }
                else {
                    tokens.add(factory.createSetAllowNullToDb(dbEntity, attr));
                }
            }

            // TODO: check more types than char/varchar
            // TODO: psql report VARCHAR for text column, not clob
            switch (detected.getType()) {
                case Types.VARCHAR:
                case Types.CHAR:
                    if (attr.getMaxLength() != detected.getMaxLength()) {
                        tokens.add(factory.createSetColumnTypeToDb(dbEntity, detected, attr));
                    }
                    break;
            }
        }
    }

    /**
     * case insensitive search for a {@link DbEntity} in a {@link DataMap} by name
     */
    private DbEntity findDbEntity(DataMap map, String caseInsensitiveName) {
        // TODO: create a Map with upper case keys?
        for (Iterator it = map.getDbEntities().iterator(); it.hasNext();) {
            DbEntity e = (DbEntity) it.next();
            if (e.getName().equalsIgnoreCase(caseInsensitiveName)) {
                return e;
            }
        }
        return null;
    }

    /**
     * case insensitive search for a {@link DbAttribute} in a {@link DbEntity} by name
     */
    private DbAttribute findDbAttribute(DbEntity entity, String caseInsensitiveName) {
        for (Iterator it = entity.getAttributes().iterator(); it.hasNext();) {
            DbAttribute a = (DbAttribute) it.next();
            if (a.getName().equalsIgnoreCase(caseInsensitiveName)) {
                return a;
            }
        }
        return null;
    }

    private static final class LoaderDelegate implements DbLoaderDelegate {

        public void dbEntityAdded(DbEntity ent) {
        }

        public void dbEntityRemoved(DbEntity ent) {
        }

        public void objEntityAdded(ObjEntity ent) {
        }

        public void objEntityRemoved(ObjEntity ent) {
        }

        public boolean overwriteDbEntity(DbEntity ent) throws CayenneException {
            return false;
        }

    }
}
