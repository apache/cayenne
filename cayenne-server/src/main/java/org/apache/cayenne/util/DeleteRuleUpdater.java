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
package org.apache.cayenne.util;

import java.util.Collection;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * DeleteRuleUpdater is responsible for auto-setting delete rules for object relationships
 */
public class DeleteRuleUpdater implements EntityMergeListener {
    /**
     * Singleton object to for defining EntityMergeListener instance
     */
    private static DeleteRuleUpdater instance;
    
    /**
     * Updates delete rules for all obj entities in a datamap
     */
    public static void updateDataMap(DataMap map) {
        Collection<ObjEntity> entities = map.getObjEntities();
        for (ObjEntity ent : entities) {
            updateObjEntity(ent);
        }
    }
    
    /**
     * Updates delete rules for all relationships in a objentity
     */
    public static void updateObjEntity(ObjEntity e) {
        Collection<ObjRelationship> rels = e.getRelationships();
        for (ObjRelationship rel : rels) {
            updateObjRelationship(rel);
        }
    }
    
    /**
     * Updates delete rules for specified relationship
     */
    public static void updateObjRelationship(ObjRelationship rel) {
        rel.setDeleteRule(rel.isToMany() ? DeleteRule.DEFAULT_DELETE_RULE_TO_MANY :
            DeleteRule.DEFAULT_DELETE_RULE_TO_ONE);
    }

    public void objAttributeAdded(ObjAttribute attr) {
    }

    public void objRelationshipAdded(ObjRelationship rel) {
        updateObjRelationship(rel);
    }
    
    /**
     * Returns EntityMergeListener instance, which can set delete rule at relationship change
     */
    public static EntityMergeListener getEntityMergeListener() {
        if (instance == null) {
            instance = new DeleteRuleUpdater();
        }
        
        return instance;
    }
}
