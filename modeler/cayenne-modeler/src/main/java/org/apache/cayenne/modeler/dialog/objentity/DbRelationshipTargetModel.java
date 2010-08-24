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
package org.apache.cayenne.modeler.dialog.objentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.modeler.util.Comparators;
import org.scopemvc.core.ModelChangeTypes;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;

/**
 * Model of a dialog to select source, target and cardinality for DbRelationship  
 */
public class DbRelationshipTargetModel extends BasicModel {
    public static final Selector TARGET_SELECTOR = Selector.fromString("target");
    
    public static final Selector TARGETS_SELECTOR = Selector.fromString("targets");
    
    public static final Selector TO_MANY_SELECTOR = Selector.fromString("toMany");
    
    protected DbEntity relTarget;
    protected List<DbEntity> relTargets;
    
    protected DbEntity source;
    
    protected boolean source1Selected;
    
    protected boolean toMany;
    
    @SuppressWarnings("unchecked")
    public DbRelationshipTargetModel(DbEntity source1, DbEntity source2) {
        this.relTargets = new ArrayList<DbEntity>(source1.getDataMap().getDbEntities());
        Collections.sort(relTargets, Comparators.getNamedObjectComparator());
        
        // this sets the right enabled state
        fireModelChange(ModelChangeTypes.VALUE_CHANGED, TARGETS_SELECTOR);
    }
    
    public List<DbEntity> getTargets() {
        return relTargets;
    }
    
    public DbEntity getTarget() {
        return relTarget;
    }
    
    public void setTarget(DbEntity newRelTarget) {
        this.relTarget = newRelTarget;
    }
    
    public boolean isSource1Selected() {
        return source1Selected;
    }
    
    public DbEntity getSource() {
        return source;
    }
    
    public void setSource(DbEntity source, boolean source1) {
        this.source = source;
        this.source1Selected = source1;
    }
    
    public boolean isToMany() {
        return toMany;
    }
    
    public void setToMany(boolean b) {
        this.toMany = b;
    }
}
