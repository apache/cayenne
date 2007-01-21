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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;

/**
 * @author Andrus Adamchik
 */
public class MySQLStackAdapter extends AccessStackAdapter {

    static final Collection NO_CONSTRAINTS_TABLES = Arrays.asList(new Object[] {
            "REFLEXIVE_AND_TO_ONE", "ARTGROUP"
    });

    public MySQLStackAdapter(DbAdapter adapter) {
        super(adapter);
    }

    public boolean supportsLobs() {
        return true;
    }

    public boolean supportsCaseSensitiveLike() {
        return false;
    }

    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop)
            throws Exception {
        // special DROP CONSTRAINT syntax for MySQL
        if (adapter.supportsFkConstraints()) {
            Map constraintsMap = getConstraints(conn, map, tablesToDrop);

            Iterator it = constraintsMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();

                Collection constraints = (Collection) entry.getValue();
                if (constraints == null || constraints.isEmpty()) {
                    continue;
                }

                Object tableName = entry.getKey();
                Iterator cit = constraints.iterator();
                while (cit.hasNext()) {
                    Object constraint = cit.next();
                    StringBuffer drop = new StringBuffer();
                    drop.append("ALTER TABLE ").append(tableName).append(
                            " DROP FOREIGN KEY ").append(constraint);
                    executeDDL(conn, drop.toString());
                }
            }
        }
    }

    public boolean supportsFKConstraints(DbEntity entity) {
        // MySQL supports that, but there are problems deleting objects from such
        // tables...
        return adapter.supportsFkConstraints()
                && !NO_CONSTRAINTS_TABLES.contains(entity.getName());
    }

}
