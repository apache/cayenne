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

package org.apache.cayenne.modeler.event;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.Relationship;

public class RelationshipDisplayEvent extends EntityDisplayEvent {
    protected Relationship[] relationships;
    protected boolean relationshipChanged = true;
    
    public RelationshipDisplayEvent(
            Object src,
            Relationship relationship,
            Entity entity,
            DataMap map,
            DataDomain domain) {

            super(src, entity, map, domain);
            this.relationships = new Relationship[] { relationship };
    }

    public RelationshipDisplayEvent(
        Object src,
        Relationship[] relationships,
        Entity entity,
        DataMap map,
        DataDomain domain) {

        super(src, entity, map, domain);
        this.relationships = relationships;
    }
    
    public Relationship[] getRelationships() {
        return relationships;
    }

    public boolean isRelationshipChanged() {
        return relationshipChanged;
    }

    public void setRelationshipChanged(boolean temp) {
        relationshipChanged = temp;
    }
}
