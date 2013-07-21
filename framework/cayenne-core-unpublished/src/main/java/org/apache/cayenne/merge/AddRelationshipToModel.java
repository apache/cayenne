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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

public class AddRelationshipToModel extends AbstractToModelToken.Entity {

    private DbRelationship rel;

    public AddRelationshipToModel(DbEntity entity, DbRelationship rel) {
        super(entity);
        this.rel = rel;
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createDropRelationshipToDb(getEntity(), rel);
    }

    public void execute(MergerContext mergerContext) {
        getEntity().addRelationship(rel);
        // TODO: add reverse relationship as well if it does not exist
        synchronizeWithObjEntity(mergerContext, getEntity());
        mergerContext.getModelMergeDelegate().dbRelationshipAdded(rel);
    }

    public String getTokenName() {
        return "Add Relationship";
    }

    @Override
    public String getTokenValue() {
        StringBuilder s = new StringBuilder();
        s.append(rel.getSourceEntity().getName());
        s.append("->");
        s.append(rel.getTargetEntityName());
        return s.toString();
    }
    
    public DbRelationship getRelationship() {
        return rel;
    }

}
