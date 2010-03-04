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
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.Util;
import org.scopemvc.core.ModelChangeTypes;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;

/**
 * A Scope model for mapping an ObjRelationship to one or more DbRelationships.
 * 
 * @since 1.1
 */
public class ObjRelationshipInfoModel extends BasicModel {

    static final String COLLECTION_TYPE_MAP = "java.util.Map";
    static final String COLLECTION_TYPE_SET = "java.util.Set";
    static final String COLLECTION_TYPE_COLLECTION = "java.util.Collection";

    static final String DEFAULT_MAP_KEY = "ID (default)";

    public static final Selector DB_RELATIONSHIPS_SELECTOR = Selector
            .fromString("dbRelationships");
    public static final Selector SOURCE_ENTITY_NAME_SELECTOR = Selector
            .fromString("relationship.sourceEntity.name");
    public static final Selector OBJECT_TARGET_SELECTOR = Selector
            .fromString("objectTarget");
    public static final Selector OBJECT_TARGETS_SELECTOR = Selector
            .fromString("objectTargets");

    public static final Selector RELATIONSHIP_NAME_SELECTOR = Selector
            .fromString("relationshipName");
    public static final Selector TARGET_COLLECTIONS_SELECTOR = Selector
            .fromString("targetCollections");
    public static final Selector TARGET_COLLECTION_SELECTOR = Selector
            .fromString("targetCollection");
    public static final Selector MAP_KEYS_SELECTOR = Selector.fromString("mapKeys");
    public static final Selector MAP_KEY_SELECTOR = Selector.fromString("mapKey");

    public static final Selector CURRENT_PATH_SELECTOR = Selector
            .fromString("currentPath");

    protected ObjRelationship relationship;

    /**
     * List of DB Relationships current ObjRelationship is mapped to
     */
    protected List<DbRelationship> dbRelationships;

    /**
     * List of current saved DB Relationships
     */
    protected List<DbRelationship> savedDbRelationships;

    protected ObjEntity objectTarget;
    protected List<ObjEntity> objectTargets;
    protected List<String> targetCollections;
    protected List<String> mapKeys;
    protected String relationshipName;
    protected String targetCollection;
    protected String mapKey;

    protected String currentPath;

    @SuppressWarnings("unchecked")
    public ObjRelationshipInfoModel(ObjRelationship relationship) {

        this.relationship = relationship;
        this.relationshipName = relationship.getName();

        this.mapKey = relationship.getMapKey();
        this.targetCollection = relationship.getCollectionType();
        if (targetCollection == null) {
            targetCollection = ObjRelationship.DEFAULT_COLLECTION_TYPE;
        }

        this.objectTarget = (ObjEntity) relationship.getTargetEntity();
        if (objectTarget != null) {
            updateTargetCombo(objectTarget.getDbEntity());
        }

        // validate -
        // current limitation is that an ObjRelationship must have source
        // and target entities present, with DbEntities chosen.
        validateCanMap();

        this.targetCollections = new ArrayList<String>(4);
        targetCollections.add(COLLECTION_TYPE_COLLECTION);
        targetCollections.add(ObjRelationship.DEFAULT_COLLECTION_TYPE);
        targetCollections.add(COLLECTION_TYPE_MAP);
        targetCollections.add(COLLECTION_TYPE_SET);

        this.mapKeys = new ArrayList<String>();
        initMapKeys();

        // setup path
        dbRelationships = new ArrayList<DbRelationship>(relationship.getDbRelationships());
        selectPath();

        // this sets the right enabled state of collection type selectors
        fireModelChange(ModelChangeTypes.VALUE_CHANGED, DB_RELATIONSHIPS_SELECTOR);

        // add dummy last relationship if we are not connected
        connectEnds();
    }

    /**
     * Places in objectTargets list all ObjEntities for specified DbEntity
     */
    @SuppressWarnings("unchecked")
    protected void updateTargetCombo(DbEntity dbTarget) {
        // copy those that have DbEntities mapped to dbTarget, and then sort

        this.objectTargets = new ArrayList<ObjEntity>();

        if (dbTarget != null) {
            objectTargets.addAll(dbTarget.getDataMap().getMappedEntities(dbTarget));
            Collections.sort(objectTargets, Comparators.getNamedObjectComparator());
        }

        fireModelChange(ModelChangeTypes.VALUE_CHANGED, OBJECT_TARGETS_SELECTOR);
    }

    public ObjRelationship getRelationship() {
        return relationship;
    }

    /**
     * @return list of DB Relationships current ObjRelationship is mapped to
     */
    public List<DbRelationship> getDbRelationships() {
        return dbRelationships;
    }

    /**
     * @return list of saved DB Relationships
     */
    public List<DbRelationship> getSavedDbRelationships() {
        return savedDbRelationships;
    }

    /**
     * @return last relationship in the path, or <code>null</code> if path is empty
     */
    public DbRelationship getLastRelationship() {
        return dbRelationships.size() == 0 ? null : dbRelationships.get(dbRelationships
                .size() - 1);
    }

    /**
     * Sets list of DB Relationships current ObjRelationship is mapped to
     */
    public void setDbRelationships(List<DbRelationship> rels) {
        this.dbRelationships = rels;

        updateTargetCombo(rels.size() > 0 ? (DbEntity) rels
                .get(rels.size() - 1)
                .getTargetEntity() : null);
    }

    /**
     * Sets list of saved DB Relationships
     */
    public void setSavedDbRelationships(List<DbRelationship> rels) {
        this.savedDbRelationships = rels;

        String currPath = "";
        for (DbRelationship rel : rels) {
            currPath += "->" + rel.getName();
        }

        if (rels.size() > 0) {
            currPath = currPath.substring(2);
        }

        currentPath = currPath;
        fireModelChange(ModelChangeTypes.VALUE_CHANGED, CURRENT_PATH_SELECTOR);
    }

    /**
     * Confirms selection of Db Rels
     */
    public void selectPath() {
        setSavedDbRelationships(new ArrayList<DbRelationship>(dbRelationships));
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

            // init available map keys
            initMapKeys();
        }
    }

    private void initMapKeys() {
        this.mapKeys.clear();

        mapKeys.add(DEFAULT_MAP_KEY);

        /**
         * Object target can be null when selected target DbEntity has no ObjEntities
         */
        if (objectTarget == null) {
            return;
        }

        for (ObjAttribute attribute : this.objectTarget.getAttributes()) {
            mapKeys.add(attribute.getName());
        }

        fireModelChange(ModelChangeTypes.VALUE_CHANGED, MAP_KEYS_SELECTOR);

        if (mapKey != null && !mapKeys.contains(mapKey)) {
            mapKey = DEFAULT_MAP_KEY;
            fireModelChange(ModelChangeTypes.VALUE_CHANGED, MAP_KEY_SELECTOR);
        }
    }

    /**
     * Returns a list of ObjEntities available for target mapping.
     */
    public List<ObjEntity> getObjectTargets() {
        return objectTargets;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
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
        fireModelChange(VALUE_CHANGED, null);
    }

    public boolean isToMany() {
        // copied algorithm from ObjRelationship.calculateToMany(), only iterating through
        // the unsaved dbrels selection.

        for (DbRelationship relationship : dbRelationships) {
            if (relationship != null && relationship.isToMany()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Stores current state of the model in the internal ObjRelationship.
     */
    public synchronized boolean savePath() {
        boolean hasChanges = false;

        boolean oldToMany = relationship.isToMany();

        if (!Util.nullSafeEquals(relationship.getName(), relationshipName)) {
            hasChanges = true;
            relationship.setName(relationshipName);
        }

        if (savedDbRelationships.size() > 0) {
            DbEntity lastEntity = (DbEntity) savedDbRelationships.get(
                    savedDbRelationships.size() - 1).getTargetEntity();

            if (objectTarget == null || objectTarget.getDbEntity() != lastEntity) {
                /**
                 * Entities in combobox and path browser do not match. In this case, we
                 * rely on the browser and automatically select one of lastEntity's
                 * ObjEntities
                 */
                Collection<ObjEntity> objEntities = lastEntity
                        .getDataMap()
                        .getMappedEntities(lastEntity);
                objectTarget = objEntities.size() == 0 ? null : objEntities
                        .iterator()
                        .next();
            }
        }

        if (objectTarget == null
                || !Util.nullSafeEquals(objectTarget.getName(), relationship
                        .getTargetEntityName())) {
            hasChanges = true;

            // note on events notification - this needs to be propagated
            // via old modeler events, but we leave this to the controller
            // since model knows nothing about Modeler mediator.
            relationship.setTargetEntity(objectTarget);
        }

        // check for path modifications
        List<DbRelationship> oldPath = relationship.getDbRelationships();
        if (oldPath.size() != savedDbRelationships.size()) {
            hasChanges = true;
            updatePath();
        }
        else {
            for (int i = 0; i < oldPath.size(); i++) {
                DbRelationship next = savedDbRelationships.get(i);

                if (oldPath.get(i) != next) {
                    hasChanges = true;
                    updatePath();
                    break;
                }
            }
        }

        String collectionType = ObjRelationship.DEFAULT_COLLECTION_TYPE
                .equals(targetCollection)
                || !relationship.isToMany() ? null : targetCollection;
        if (!Util.nullSafeEquals(collectionType, relationship.getCollectionType())) {
            hasChanges = true;
            relationship.setCollectionType(collectionType);
        }

        // map key only makes sense for Map relationships
        String mapKey = COLLECTION_TYPE_MAP.equals(collectionType)
                && !DEFAULT_MAP_KEY.equals(this.mapKey) ? this.mapKey : null;
        if (!Util.nullSafeEquals(mapKey, relationship.getMapKey())) {
            hasChanges = true;
            relationship.setMapKey(mapKey);
        }

        /**
         * As of CAY-436 here we check if to-many property has changed during the editing,
         * and if so, delete rule must be reset to default value
         */
        if (hasChanges && relationship.isToMany() != oldToMany) {
            DeleteRuleUpdater.updateObjRelationship(relationship);
        }

        return hasChanges;
    }

    private void updatePath() {
        relationship.clearDbRelationships();

        for (DbRelationship nextPathComponent : dbRelationships) {
            if (nextPathComponent == null) {
                break;
            }

            relationship.addDbRelationship(nextPathComponent);
        }
    }

    private void breakChain(int index) {
        // strip everything starting from the index

        while (dbRelationships.size() > (index + 1)) {
            // remove last
            dbRelationships.remove(dbRelationships.size() - 1);
        }
    }

    // Connects last selected DbRelationship in the path to the
    // last DbEntity, creating a dummy relationship if needed.
    private void connectEnds() {
        Relationship last = null;

        int size = dbRelationships.size();
        if (size > 0) {
            last = dbRelationships.get(size - 1);
        }

        Entity target = getEndEntity();

        if (target != null && (last == null || last.getTargetEntity() != target)) {
            // try to connect automatically, if we can't use dummy connector

            Entity source = (last == null) ? getStartEntity() : last.getTargetEntity();
            if (source != null) {

                Relationship anyConnector = source != null ? source
                        .getAnyRelationship(target) : null;

                if (anyConnector != null) {
                    dbRelationships.add((DbRelationship) anyConnector);
                }
            }
        }
    }

    /**
     * Checks if the entity can be edited with this inspector. NOTE: As of CAY-1077,
     * relationship inspector can be opened even if no target entity was set.
     */
    private void validateCanMap() {
        if (relationship.getSourceEntity() == null) {
            throw new CayenneRuntimeException(
                    "Can't map relationship without source entity.");
        }

        if (getStartEntity() == null) {
            throw new CayenneRuntimeException(
                    "Can't map relationship without source DbEntity.");
        }
    }

    public DbEntity getStartEntity() {
        return ((ObjEntity) relationship.getSourceEntity()).getDbEntity();
    }

    public DbEntity getEndEntity() {
        /**
         * Object target can be null when selected target DbEntity has no ObjEntities
         */
        if (objectTarget == null) {
            return null;
        }

        return objectTarget.getDbEntity();
    }

    public String getMapKey() {
        return mapKey;
    }

    public void setMapKey(String mapKey) {
        this.mapKey = mapKey;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public String getTargetCollection() {
        return targetCollection;
    }

    public void setTargetCollection(String targetCollection) {
        this.targetCollection = targetCollection;
    }

    public List getMapKeys() {
        return mapKeys;
    }

    public List<String> getTargetCollections() {
        return targetCollections;
    }
}
