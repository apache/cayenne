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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;

/**
 * A {@link MergerToken} to remove a {@link DbEntity} from a {@link DataMap}. Any
 * {@link ObjEntity} mapped to the {@link DbEntity} will also be removed.
 * 
 */
public class DropTableToModel extends AbstractToModelToken.Entity {

    public DropTableToModel(DbEntity entity) {
        super(entity);
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createCreateTableToDb(getEntity());
    }

    public void execute(MergerContext mergerContext) {
        for (ObjEntity objEntity : objEntitiesMappedToDbEntity(getEntity())) {
            objEntity.getDataMap().removeObjEntity(objEntity.getName(), true);
            mergerContext.getModelMergeDelegate().objEntityRemoved(objEntity);
        }
        getEntity().getDataMap().removeDbEntity(getEntity().getName(), true);
        mergerContext.getModelMergeDelegate().dbEntityRemoved(getEntity());
    }

    public String getTokenName() {
        return "Drop Table";
    }

}
