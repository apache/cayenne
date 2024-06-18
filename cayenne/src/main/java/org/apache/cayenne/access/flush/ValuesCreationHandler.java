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

package org.apache.cayenne.access.flush;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.flush.operation.DbRowOpType;
import org.apache.cayenne.access.flush.operation.DbRowOpWithValues;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

/**
 * Extension of {@link ArcValuesCreationHandler} that also tracks property changes.
 *
 * @since 4.2
 */
class ValuesCreationHandler extends ArcValuesCreationHandler {

    ValuesCreationHandler(DbRowOpFactory factory, DbRowOpType defaultType) {
        super(factory, defaultType);
    }

    @Override
    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
        ObjectId id = (ObjectId)nodeId;
        ObjEntity entity = factory.getDescriptor().getEntity();
        if(entity.isReadOnly()) {
            throw new CayenneRuntimeException("Attempt to modify object(s) mapped to a read-only entity: '%s'. " +
                    "Can't commit changes.", entity.getName());
        }
        ObjAttribute attribute = entity.getAttribute(property);
        DbEntity dbEntity = entity.getDbEntity();

        if(attribute.isFlattened()) {
            // get target row ID
            FlattenedPathProcessingResult result
                    = processFlattenedPath(id, null, dbEntity, attribute.getDbAttributePath(), newValue != null);
            if(result.isProcessed()) {
                id = result.getId();
            }
        }

        if(id == null) {
            // some extra safety, shouldn't happen
            throw new CayenneRuntimeException("Unable to resolve DB row PK for object's %s update of property '%s'"
                    , nodeId, property);
        }

        DbAttribute dbAttribute = attribute.getDbAttribute();
        if(dbAttribute.isPrimaryKey()) {
            if(!(newValue instanceof Number) || ((Number) newValue).longValue() != 0) {
                id.getReplacementIdMap().put(dbAttribute.getName(), newValue);
            }
        }

        DbRowOpWithValues dbRow = factory.get(id);
        if(dbRow != null) {
            dbRow.getValues().addValue(dbAttribute, newValue, false);
        }
    }

}
