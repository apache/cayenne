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

package org.apache.cayenne.query;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * BatchQuery and its descendants allow to group similar data for the batch
 * database modifications, including inserts, updates and deletes. Single
 * BatchQuery corresponds to a parameterized PreparedStatement and a matrix of
 * values.
 * 
 */
public abstract class BatchQuery implements Query {

    /**
     * @since 1.2
     */
    protected DbEntity dbEntity;

    /**
     * @since 4.0
     */
    protected List<BatchQueryRow> rows;

    protected List<DbAttribute> dbAttributes;

    /**
     * @since 4.0
     */
    public BatchQuery(DbEntity dbEntity, List<DbAttribute> dbAttributes, int batchCapacity) {
        this.dbEntity = dbEntity;
        this.rows = new ArrayList<>(batchCapacity);
        this.dbAttributes = dbAttributes;
    }

    /**
     * @since 4.0
     */
    public List<BatchQueryRow> getRows() {
        return rows;
    }

    /**
     * Returns default select parameters.
     * 
     * @since 1.2
     */
    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {
        return new DefaultQueryMetadata() {

            @Override
            public DbEntity getDbEntity() {
                return dbEntity;
            }
        };
    }

    /**
     * @since 1.2
     */
    @Override
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        router.route(router.engineForDataMap(dbEntity.getDataMap()), this, substitutedQuery);
    }

    /**
     * Calls "batchAction" on the visitor.
     * 
     * @since 1.2
     */
    @Override
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.batchAction(this);
    }

    /**
     * Returns true if the batch query uses optimistic locking.
     * 
     * @since 1.1
     */
    public boolean isUsingOptimisticLocking() {
        return false;
    }

    /**
     * Returns a DbEntity associated with this batch.
     */
    public DbEntity getDbEntity() {
        return dbEntity;
    }

    /**
     * Returns a list of DbAttributes describing batch parameters.
     */
    public List<DbAttribute> getDbAttributes() {
        return dbAttributes;
    }

}
