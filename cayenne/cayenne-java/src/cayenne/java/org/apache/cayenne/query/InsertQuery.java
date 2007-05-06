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

import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * Describes insert database operation. InsertQuery is initialized with object values
 * snapshot and ObjectId.
 * 
 * @deprecated since 1.2 Object InsertQuery is not needed anymore. It shouldn't be used
 *             directly anyway, but in cases where one might want to have access to it,
 *             InsertBatchQuery is a reasonable substitute.
 */
public class InsertQuery extends AbstractQuery {

    protected ObjectId objectId;
    protected Map objectSnapshot;

    /** Creates empty InsertQuery. */
    public InsertQuery() {
    }

    /** Creates InsertQuery with the <code>rootEntity</code> as the root object */
    public InsertQuery(ObjEntity rootEntity) {
        this.setRoot(rootEntity);
    }

    /** Creates InsertQuery with the <code>rootClass</code> as the root object */
    public InsertQuery(Class rootClass) {
        this.setRoot(rootClass);
    }

    /** Creates InsertQuery with the <code>rootClass</code> as the root object */
    public InsertQuery(Class rootClass, Map dataRow) {
        this.setRoot(rootClass);
        this.setObjectSnapshot(dataRow);
    }

    /** Creates InsertQuery with <code>objEntityName</code> parameter. */
    public InsertQuery(String objEntityName) {
        this.setRoot(objEntityName);
    }

    public QueryMetadata getMetaData(final EntityResolver resolver) {
        return new DefaultQueryMetadata() {

            public ObjEntity getObjEntity() {
                return resolver.lookupObjEntity(objectId.getEntityName());
            }

            public DbEntity getDbEntity() {
                return getObjEntity().getDbEntity();
            }
        };
    }

    /**
     * Calls "makeUpdate" on the visitor.
     * 
     * @since 1.2
     */
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.updateAction(this);
    }

    public Map getObjectSnapshot() {
        return objectSnapshot;
    }

    public void setObjectSnapshot(Map objectSnapshot) {
        this.objectSnapshot = objectSnapshot;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }
}
