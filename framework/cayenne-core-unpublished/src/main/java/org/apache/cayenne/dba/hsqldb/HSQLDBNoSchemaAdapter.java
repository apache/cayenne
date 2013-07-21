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

package org.apache.cayenne.dba.hsqldb;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbEntity;

/**
 * A flavor of HSQLDBAdapter that implements workarounds for some old driver limitations.
 * 
 * @since 1.2
 */
public class HSQLDBNoSchemaAdapter extends HSQLDBAdapter {
    
    public HSQLDBNoSchemaAdapter(@Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories);
    }

    /**
     * Generate unqualified name without schema.
     * 
     * @since 1.2
     */
    @Override
    protected String getTableName(DbEntity entity) {
        return quotingStrategy.quotedIdentifier(entity, entity.getName());
    }

    /**
     * Returns NULL.
     * 
     * @since 1.2
     */
    @Override
    protected String getSchemaName(DbEntity entity) {
        return null;
    }
 
    @Override
    public Collection<String> dropTableStatements(DbEntity table) {
        // hsqldb doesn't support schema namespaces, so remove if found
        return Collections.singleton("DROP TABLE " + getTableName(table));
    }

    /**
     * Uses unqualified entity names.
     * 
     * @since 1.2
     */
    @Override
    public String createTable(DbEntity ent) {
        String sql = super.createTable(ent);

        // hsqldb doesn't support schema namespaces, so remove if found
        String fqnCreate = "CREATE CACHED TABLE " + super.getTableName(ent) + " (";
        if (sql != null && sql.toUpperCase().startsWith(fqnCreate)) {
            sql = "CREATE CACHED TABLE "
                    + getTableName(ent)
                    + " ("
                    + sql.substring(fqnCreate.length());
        }

        return sql;
    }
}
