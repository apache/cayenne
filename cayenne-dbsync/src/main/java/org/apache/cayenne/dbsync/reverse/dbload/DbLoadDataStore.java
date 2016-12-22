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

package org.apache.cayenne.dbsync.reverse.dbload;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DetectedDbEntity;
import org.apache.cayenne.map.Procedure;

/**
 * Temporary storage for loaded from DB DbEntities and Procedures.
 * DataMap is used but it's functionality is excessive and
 * there can be unwanted side effects.
 * But we can't get rid of it right now as parallel data structure
 * for dbEntity, attributes, procedures etc.. must be created
 * or some other work around should be implemented because
 * some functionality relies on side effects (e.g. entity resolution
 * in relationship)
 */
public class DbLoadDataStore extends DataMap {

    private Map<String, Set<ExportedKey>> exportedKeys = new HashMap<>();

    private Map<String, DbEntity> upperCaseNames = new HashMap<>();

    DbLoadDataStore() {
        super("__generated_by_dbloader__");
    }

    @Override
    public DbEntity getDbEntity(String dbEntityName) {
        return upperCaseNames.get(dbEntityName.toUpperCase());
    }

    @Override
    public void addDbEntity(DbEntity entity) {
        if(!(entity instanceof DetectedDbEntity)) {
            throw new IllegalArgumentException("Only DetectedDbEntity can be inserted in this map");
        }
        super.addDbEntity(entity);
        upperCaseNames.put(entity.getName().toUpperCase(), entity);
    }

    DbEntity addDbEntitySafe(DbEntity entity) {
        if(!(entity instanceof DetectedDbEntity)) {
            throw new IllegalArgumentException("Only DetectedDbEntity can be inserted in this map");
        }
        DbEntity old = getDbEntity(entity.getName());
        if(old != null) {
            removeDbEntity(old.getName());
        }
        addDbEntity(entity);
        return old;
    }

    void addProcedureSafe(Procedure procedure) {
        Procedure old = getProcedure(procedure.getName());
        if(old != null) {
            removeProcedure(old.getName());
        }
        addProcedure(procedure);
    }

    void addExportedKey(ExportedKey key) {
        Set<ExportedKey> exportedKeys = this.exportedKeys.get(key.getStrKey());
        if (exportedKeys == null) {
            exportedKeys = new TreeSet<>();
            this.exportedKeys.put(key.getStrKey(), exportedKeys);
        }
        exportedKeys.add(key);
    }

    Set<Map.Entry<String, Set<ExportedKey>>> getExportedKeysEntrySet() {
        return exportedKeys.entrySet();
    }
}
