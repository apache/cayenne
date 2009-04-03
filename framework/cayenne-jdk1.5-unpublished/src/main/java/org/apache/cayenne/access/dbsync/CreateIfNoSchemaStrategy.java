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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @since 3.0
 */
public class CreateIfNoSchemaStrategy implements SchemaUpdateStrategy {

    final Log logObj = LogFactory.getLog(CreateIfNoSchemaStrategy.class);

    private SchemaUpdateStrategy currentSchema;

    private SchemaUpdateStrategy getSchema() {
        return currentSchema;
    }

    public CreateIfNoSchemaStrategy() {
        currentSchema = this;
    }

    public void updateSchema(DataNode dataNode) {
        getSchema().generateUpdateSchema(dataNode);
    }

    public void generateUpdateSchema(DataNode dataNode) {

        Map<String, Boolean> nameTables = getNameTablesInDB(dataNode);
        Collection<DbEntity> entities = dataNode.getEntityResolver().getDbEntities();
        boolean generate = true;
        Iterator<DbEntity> it = entities.iterator();
        while (it.hasNext()) {
            if (nameTables.get(it.next().getName()) != null) {
                generate = false;
                break;
            }
        }
        if (generate) {
            generate(dataNode);
        }
        else {
            logObj
                    .info("DbGenerator no create, because one of the tables, modeled in Cayenne, already exist in DB");
        }
        currentSchema = new SkipSchemaUpdateStrategy();
    }

    private synchronized void generate(DataNode dataNode) {
        Collection<DataMap> map = dataNode.getDataMaps();
        Iterator<DataMap> iterator = map.iterator();
        while (iterator.hasNext()) {
            DbGenerator gen = new DbGenerator(dataNode.getAdapter(), iterator.next());
            gen.setShouldCreateTables(true);
            gen.setShouldDropTables(false);
            gen.setShouldCreateFKConstraints(false);
            gen.setShouldCreatePKSupport(false);
            gen.setShouldDropPKSupport(false);
            try {
                gen.runGenerator(dataNode.getDataSource());
            }
            catch (Exception e) {
                throw new CayenneRuntimeException(e);
            }
        }
    }

    /**
     * Returns all the table names in database.
     */
    protected Map<String, Boolean> getNameTablesInDB(DataNode dataNode) {
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
                logObj.info("error: " + e);
            }
        }
        return nameTables;
    }
}
