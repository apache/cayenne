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

package org.apache.cayenne.query;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.commons.collections.Factory;

/**
 * BatchQuery and its descendants allow to group similar data for the batch database
 * modifications, including inserts, updates and deletes. Single BatchQuery corresponds to
 * a parameterized PreparedStatement and a matrix of values.
 * 
 */
public abstract class BatchQuery implements Query {

    /**
     * @since 1.2
     */
    protected int batchIndex;

    /**
     * @since 1.2
     */
    protected DbEntity dbEntity;

    protected String name;
    
    /**
     * @since 3.1
     */
    protected DataMap dataMap;

    
    public BatchQuery(DbEntity dbEntity) {
        this.dbEntity = dbEntity;
        this.batchIndex = -1;
    }
    
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @since 3.1
     */
    public DataMap getDataMap() {
        return dataMap;
    }

    /**
     * @since 3.1
     */
    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * Returns default select parameters.
     * 
     * @since 1.2
     */
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
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        router.route(
                router.engineForDataMap(dbEntity.getDataMap()),
                this,
                substitutedQuery);
    }

    /**
     * Calls "batchAction" on the visitor.
     * 
     * @since 1.2
     */
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
     * Returns <code>true</code> if this batch query has no parameter rows.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns a list of DbAttributes describing batch parameters.
     */
    public abstract List<DbAttribute> getDbAttributes();

    /**
     * Rewinds batch to the first parameter row.
     */
    public void reset() {
        batchIndex = -1;
    }

    /**
     * Repositions batch to the next object, so that subsequent calls to getObject(int)
     * would return the values of the next batch object. Returns <code>true</code> if
     * batch has more objects to iterate over, <code>false</code> otherwise.
     */
    public boolean next() {
        batchIndex++;
        return size() > batchIndex;
    }

    /**
     * Returns a value at a given index for the current batch iteration.
     * 
     * @since 1.2
     */
    public abstract Object getValue(int valueIndex);

    /**
     * Returns the number of parameter rows in a batch.
     */
    public abstract int size();

    /**
     * A helper method used by subclasses to resolve deferred values on demand. This is
     * useful when a certain value comes from a generated key of another master object.
     * 
     * @since 1.2
     */
    protected Object getValue(Map<String, Object> valueMap, DbAttribute attribute) {

        Object value = valueMap.get(attribute.getName());

        // if a value is a Factory, resolve it here...
        // slight chance that a normal value will implement Factory interface???
        if (value instanceof Factory) {
            value = ((Factory) value).create();

            // update replacement id
            if (attribute.isPrimaryKey()) {
                // sanity check
                if (value == null) {
                    String name = attribute.getEntity() != null ? attribute
                            .getEntity()
                            .getName() : "<null>";
                    throw new CayenneRuntimeException("Failed to generate PK: "
                            + name
                            + "."
                            + attribute.getName());
                }

                ObjectId id = getObjectId();
                if (id != null) {
                    // always override with fresh value as this is what's in the DB
                    id.getReplacementIdMap().put(attribute.getName(), value);
                }
            }

            // update snapshot
            valueMap.put(attribute.getName(), value);
        }

        return value;
    }

    /**
     * Returns an ObjectId associated with the current batch iteration. Used internally by
     * Cayenne to match current iteration with a specific object and assign it generated
     * keys.
     * <p>
     * Default implementation simply returns null.
     * </p>
     * 
     * @since 1.2
     */
    public ObjectId getObjectId() {
        return null;
    }
}
