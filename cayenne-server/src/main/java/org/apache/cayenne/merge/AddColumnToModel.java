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

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.EntityMergeSupport;

/**
 * A {@link MergerToken} to add a {@link DbAttribute} to a {@link DbEntity}. The
 * {@link EntityMergeSupport} will be used to update the mapped {@link ObjEntity}
 * 
 */
public class AddColumnToModel extends AbstractToModelToken.EntityAndColumn {

    public AddColumnToModel(DbEntity entity, DbAttribute column) {
        super(entity, column);
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createDropColumnToDb(getEntity(), getColumn());
    }

    public void execute(MergerContext mergerContext) {
        getEntity().addAttribute(getColumn());
        synchronizeWithObjEntity(mergerContext, getEntity());
        mergerContext.getModelMergeDelegate().dbAttributeAdded(getColumn());
    }

    public String getTokenName() {
        return "Add Column";
    }

}
