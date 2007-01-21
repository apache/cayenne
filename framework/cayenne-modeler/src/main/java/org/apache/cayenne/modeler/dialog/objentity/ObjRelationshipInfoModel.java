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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.util.Util;
import org.scopemvc.core.IntIndexSelector;
import org.scopemvc.core.ModelChangeEvent;
import org.scopemvc.core.ModelChangeTypes;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;
import org.scopemvc.model.collection.ListModel;

/**
 * A Scope model for mapping an ObjRelationship to one or more DbRelationships.
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public class ObjRelationshipInfoModel extends BasicModel {

    public static final Selector DB_RELATIONSHIP_PATH_SELECTOR = Selector
            .fromString("dbRelationshipPath");
    public static final Selector SOURCE_ENTITY_NAME_SELECTOR = Selector
            .fromString("relationship.sourceEntity.name");
    public static final Selector SELECTED_PATH_COMPONENT_SELECTOR = Selector
            .fromString("selectedPathComponent");
    public static final Selector OBJECT_TARGET_SELECTOR = Selector
            .fromString("objectTarget");
    public static final Selector OBJECT_TARGETS_SELECTOR = Selector
            .fromString("objectTargets");
    public static final Selector RELATIONSHIP_NAME_SELECTOR = Selector
            .fromString("relationshipName");

    protected ObjRelationship relationship;
    protected ListModel dbRelationshipPath;
    protected EntityRelationshipsModel selectedPathComponent;
    protected ObjEntity objectTarget;
    protected List objectTargets;
    protected String relationshipName;

    public ObjRelationshipInfoModel(ObjRelationship relationship, Collection objEntities) {

        this.relationship = relationship;
        this.relationshipName = relationship.getName();
        this.objectTarget = (ObjEntity) relationship.getTargetEntity();

        // prepare entities - copy those that have DbEntities mapped, and then sort

        this.objectTargets = new ArrayList(objEntities.size());
        Iterator entities = objEntities.iterator();
        while (entities.hasNext()) {
            ObjEntity entity = (ObjEntity) entities.next();
            if (entity.getDbEntity() != null) {
                objectTargets.add(entity);
            }
        }

        Collections.sort(objectTargets, Comparators.getNamedObjectComparator());

        // validate -
        // current limitation is that an ObjRelationship must have source
        // and target entities present, with DbEntities chosen.
        validateCanMap();

        // wrap path
        this.dbRelationshipPath = new ListModel();
        Iterator it = relationship.getDbRelationships().iterator();
        while (it.hasNext()) {
            DbRelationship dbRelationship = (DbRelationship) it.next();
            this.dbRelationshipPath.add(new EntityRelationshipsModel(dbRelationship));
        }

        // add dummy last relationship if we are not connected
        connectEnds();
        this.dbRelationshipPath.addModelChangeListener(this);
    }

    public ObjRelationship getRelationship() {
        return relationship;
    }

    public ListModel getDbRelationshipPath() {
        return dbRelationshipPath;
    }

    public EntityRelationshipsModel getSelectedPathComponent() {
        return selectedPathComponent;
    }

    public void setSelectedPathComponent(EntityRelationshipsModel selectedPathComponent) {
        if (this.selectedPathComponent != selectedPathComponent) {
            unlistenOldSubmodel(SELECTED_PATH_COMPONENT_SELECTOR);
            this.selectedPathComponent = selectedPathComponent;
            listenNewSubmodel(SELECTED_PATH_COMPONENT_SELECTOR);
            fireModelChange(
                    ModelChangeTypes.VALUE_CHANGED,
                    SELECTED_PATH_COMPONENT_SELECTOR);
        }
    }

    /**
     * Returns currently selected target of the ObjRelationship.
     */
    public ObjEntity getObjectTarget() {
        return objectTarget;
    }

    /**
     * Sets a new target
     */
    public void setObjectTarget(ObjEntity objectTarget) {
        if (this.objectTarget != objectTarget) {
            unlistenOldSubmodel(OBJECT_TARGET_SELECTOR);
            this.objectTarget = objectTarget;
            listenNewSubmodel(OBJECT_TARGET_SELECTOR);
            fireModelChange(ModelChangeTypes.VALUE_CHANGED, OBJECT_TARGET_SELECTOR);

            // change the list of relationships
            breakChain(-1);
            connectEnds();
            fireModelChange(ModelChangeTypes.VALUE_CHANGED, DB_RELATIONSHIP_PATH_SELECTOR);
        }
    }

    /**
     * Returns a list of ObjEntities available for target mapping.
     */
    public List getObjectTargets() {
        return objectTargets;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public void modelChanged(ModelChangeEvent event) {

        // if a different relationship was selected, we may need to rebuild the list
        Selector selector = event.getSelector();
        while (selector != null) {
            if (selector instanceof IntIndexSelector) {
                IntIndexSelector indexSel = (IntIndexSelector) selector;
                relationshipChanged(indexSel.getIndex());
                break;
            }

            selector = selector.getNext();
        }

        super.modelChanged(event);
    }

    /**
     * Processes relationship path when path component at index was changed.
     */
    public synchronized void relationshipChanged(int index) {
        // strip everything starting from the index
        breakChain(index);

        // connect the ends
        connectEnds();

        // must fire with null selector, or refresh won't happen
        dbRelationshipPath.fireModelChange(VALUE_CHANGED, null);
    }

    /**
     * Stores current state of the model in the internal ObjRelationship.
     */
    public synchronized boolean savePath() {
        // check for modifications
        if (relationship.getTargetEntity() == objectTarget) {
            if (Util.nullSafeEquals(relationship.getName(), relationshipName)) {
                List oldPath = relationship.getDbRelationships();
                if (oldPath.size() == dbRelationshipPath.size()) {
                    boolean hasChanges = false;
                    for (int i = 0; i < oldPath.size(); i++) {
                        EntityRelationshipsModel next = (EntityRelationshipsModel) dbRelationshipPath
                                .get(i);
                        if (oldPath.get(i) != next.getSelectedRelationship()) {
                            hasChanges = true;
                            break;
                        }
                    }

                    if (!hasChanges) {
                        return false;
                    }
                }
            }
        }

        // detected modifications, save...
        relationship.clearDbRelationships();

        // note on events notification - this needs to be propagated
        // via old modeler events, but we leave this to the controller
        // since model knows nothing about Modeler mediator.
        relationship.setTargetEntity(objectTarget);
        relationship.setName(relationshipName);

        Iterator it = dbRelationshipPath.iterator();
        while (it.hasNext()) {
            EntityRelationshipsModel next = (EntityRelationshipsModel) it.next();
            Relationship nextPathComponent = next.getSelectedRelationship();
            if (nextPathComponent == null) {
                break;
            }

            relationship.addDbRelationship((DbRelationship) nextPathComponent);
        }

        return true;
    }

    private void breakChain(int index) {
        // strip everything starting from the index
        dbRelationshipPath.makeActive(false);

        try {
            while (dbRelationshipPath.size() > (index + 1)) {
                // remove last
                dbRelationshipPath.remove(dbRelationshipPath.size() - 1);
            }
        }
        finally {
            dbRelationshipPath.makeActive(true);
        }
    }

    // Connects last selected DbRelationship in the path to the
    // last DbEntity, creating a dummy relationship if needed.
    private void connectEnds() {
        Relationship last = null;

        int size = dbRelationshipPath.size();
        if (size > 0) {
            EntityRelationshipsModel wrapper = (EntityRelationshipsModel) dbRelationshipPath
                    .get(size - 1);
            last = wrapper.getSelectedRelationship();

        }

        Entity target = getEndEntity();

        if (last == null || last.getTargetEntity() != target) {
            // try to connect automatically, if we can't use dummy connector

            Entity source = (last == null) ? getStartEntity() : last.getTargetEntity();
            Relationship anyConnector = source.getAnyRelationship(target);
            EntityRelationshipsModel connector = null;

            connector = (anyConnector == null) ? new EntityRelationshipsModel(
                    source,
                    getEndEntity()) : new EntityRelationshipsModel(anyConnector);

            dbRelationshipPath.makeActive(false);
            try {
                dbRelationshipPath.add(connector);
            }
            finally {
                dbRelationshipPath.makeActive(true);
            }
        }
    }

    private void validateCanMap() {
        if (relationship.getSourceEntity() == null) {
            throw new CayenneRuntimeException(
                    "Can't map relationship without source entity.");
        }

        if (relationship.getTargetEntity() == null) {
            throw new CayenneRuntimeException(
                    "Can't map relationship without target entity.");
        }

        if (getStartEntity() == null) {
            throw new CayenneRuntimeException(
                    "Can't map relationship without source DbEntity.");
        }

        if (getEndEntity() == null) {
            throw new CayenneRuntimeException(
                    "Can't map relationship without target DbEntity.");
        }
    }

    public DbEntity getStartEntity() {
        return ((ObjEntity) relationship.getSourceEntity()).getDbEntity();
    }

    public DbEntity getEndEntity() {
        return objectTarget.getDbEntity();
    }
}
