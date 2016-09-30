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

package org.apache.cayenne.dbsync.merge;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.dbsync.naming.DuplicateNameResolver;
import org.apache.cayenne.dbsync.naming.NameCheckers;
import org.apache.cayenne.dbsync.reverse.naming.ObjectNameGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.EntityMergeListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implements methods for entity merging.
 */
public class EntityMergeSupport {

    private static final Log LOG = LogFactory.getLog(EntityMergeSupport.class);

    private static final Map<String, String> CLASS_TO_PRIMITIVE;

    static {
        CLASS_TO_PRIMITIVE = new HashMap<>();
        CLASS_TO_PRIMITIVE.put(Byte.class.getName(), "byte");
        CLASS_TO_PRIMITIVE.put(Long.class.getName(), "long");
        CLASS_TO_PRIMITIVE.put(Double.class.getName(), "double");
        CLASS_TO_PRIMITIVE.put(Boolean.class.getName(), "boolean");
        CLASS_TO_PRIMITIVE.put(Float.class.getName(), "float");
        CLASS_TO_PRIMITIVE.put(Short.class.getName(), "short");
        CLASS_TO_PRIMITIVE.put(Integer.class.getName(), "int");
    }

    private final DataMap map;
    /**
     * Strategy for choosing names for entities, attributes and relationships
     */
    private final ObjectNameGenerator nameGenerator;
    /**
     * Listeners of merge process.
     */
    private final List<EntityMergeListener> listeners = new ArrayList<EntityMergeListener>();
    protected boolean removeMeaningfulFKs;
    protected boolean removeMeaningfulPKs;
    protected boolean usePrimitives;
    
    /**
     * @since 3.0
     */
    public EntityMergeSupport(DataMap map, ObjectNameGenerator nameGenerator, boolean removeMeaningfulPKs) {
        this.map = map;
        this.nameGenerator = nameGenerator;
        this.removeMeaningfulFKs = true;
        this.removeMeaningfulPKs = removeMeaningfulPKs;

        /**
         * Adding a listener, so that all created ObjRelationships would have
         * default delete rule
         */
        addEntityMergeListener(DeleteRuleUpdater.getEntityMergeListener());
    }

    /**
     * Updates each one of the collection of ObjEntities, adding attributes and
     * relationships based on the current state of its DbEntity.
     *
     * @return true if any ObjEntity has changed as a result of synchronization.
     * @since 1.2 changed signature to use Collection instead of List.
     */
    public boolean synchronizeWithDbEntities(Iterable<ObjEntity> objEntities) {
        boolean changed = false;
        for (ObjEntity nextEntity : objEntities) {
            if (synchronizeWithDbEntity(nextEntity)) {
                changed = true;
            }
        }

        return changed;
    }

    /**
     * @since 4.0
     */
    protected boolean removePK(DbEntity dbEntity) {
        return removeMeaningfulPKs;
    }

    /**
     * @since 4.0
     */
    protected boolean removeFK(DbEntity dbEntity) {
        return removeMeaningfulFKs;
    }

    /**
     * Updates ObjEntity attributes and relationships based on the current state
     * of its DbEntity.
     *
     * @return true if the ObjEntity has changed as a result of synchronization.
     */
    public boolean synchronizeWithDbEntity(ObjEntity entity) {

        if (entity == null) {
            return false;
        }

        DbEntity dbEntity = entity.getDbEntity();
        if (dbEntity == null) {
            return false;
        }

        boolean changed = false;

        // synchronization on DataMap is some (weak) protection
        // against simultaneous modification of the map (like double-clicking on sync button)
        synchronized (map) {

            if (removeFK(dbEntity)) {
                changed = getRidOfAttributesThatAreNowSrcAttributesForRelationships(entity);
            }

            changed |= addMissingAttributes(entity);
            changed |= addMissingRelationships(entity);
        }

        return changed;
    }

    /**
     * @since 4.0
     */
    public boolean synchronizeOnDbAttributeAdded(ObjEntity entity, DbAttribute dbAttribute) {

        Collection<DbRelationship> incomingRels = getIncomingRelationships(dbAttribute.getEntity());
        if (isMissingFromObjEntity(entity, dbAttribute, incomingRels)) {
            addMissingAttribute(entity, dbAttribute);
            return true;
        }

        return false;
    }

    /**
     * @since 4.0
     */
    public boolean synchronizeOnDbRelationshipAdded(ObjEntity entity, DbRelationship dbRelationship) {

        if (isMissingFromObjEntity(entity, dbRelationship)) {
            addMissingRelationship(entity, dbRelationship);
        }

        return true;
    }

    private boolean addMissingRelationships(ObjEntity entity) {
        List<DbRelationship> relationshipsToAdd = getRelationshipsToAdd(entity);
        if (relationshipsToAdd.isEmpty()) {
            return false;
        }

        for (DbRelationship dr : relationshipsToAdd) {
            addMissingRelationship(entity, dr);
        }

        return true;
    }

    private boolean createObjRelationship(ObjEntity entity, DbRelationship dr, String targetEntityName) {
        String relationshipName = nameGenerator.createObjRelationshipName(dr);
        relationshipName = DuplicateNameResolver.resolve(NameCheckers.objRelationship, entity, relationshipName);

        ObjRelationship or = new ObjRelationship(relationshipName);
        or.addDbRelationship(dr);
        Map<String, ObjEntity> objEntities = entity.getDataMap().getSubclassesForObjEntity(entity);

        boolean hasFlattingAttributes = false;
        boolean needGeneratedEntity = true;

        if (objEntities.containsKey(targetEntityName)) {
            needGeneratedEntity = false;
        }

        for (ObjEntity subObjEntity : objEntities.values()) {
            for (ObjAttribute objAttribute : subObjEntity.getAttributes()) {
                String path = objAttribute.getDbAttributePath();
                if (path != null) {
                    if (path.startsWith(or.getDbRelationshipPath())) {
                        hasFlattingAttributes = true;
                        break;
                    }
                }
            }
        }

        if (!hasFlattingAttributes) {
            if (needGeneratedEntity) {
                or.setTargetEntityName(targetEntityName);
                or.setSourceEntity(entity);
            }

            entity.addRelationship(or);
            fireRelationshipAdded(or);
        }

        return needGeneratedEntity;
    }

    private boolean addMissingAttributes(ObjEntity entity) {
        boolean changed = false;

        for (DbAttribute da : getAttributesToAdd(entity)) {
            addMissingAttribute(entity, da);
            changed = true;
        }
        return changed;
    }

    private void addMissingRelationship(ObjEntity entity, DbRelationship dbRelationship) {
        DbEntity targetEntity = dbRelationship.getTargetEntity();

        Collection<ObjEntity> mappedObjEntities = map.getMappedEntities(targetEntity);
        if (!mappedObjEntities.isEmpty()) {
            for (Entity mappedTarget : mappedObjEntities) {
                createObjRelationship(entity, dbRelationship, mappedTarget.getName());
            }
        } else {

            if (targetEntity == null) {
                targetEntity = new DbEntity(dbRelationship.getTargetEntityName());
            }

            if (dbRelationship.getTargetEntityName() != null) {
                boolean needGeneratedEntity = createObjRelationship(entity, dbRelationship,
                        nameGenerator.createObjEntityName(targetEntity));
                if (needGeneratedEntity) {
                    LOG.warn("Can't find ObjEntity for " + dbRelationship.getTargetEntityName());
                    LOG.warn("Db Relationship (" + dbRelationship + ") will have GUESSED Obj Relationship reflection. ");
                }
            }
        }
    }

    private void addMissingAttribute(ObjEntity entity, DbAttribute da) {
        String attrName = DuplicateNameResolver.resolve(NameCheckers.objAttribute, entity,
                nameGenerator.createObjAttributeName(da));

        String type = TypesMapping.getJavaBySqlType(da.getType());
        if (usePrimitives) {
            String primitive = CLASS_TO_PRIMITIVE.get(type);
            if (primitive != null) {
                type = primitive;
            }
        }

        ObjAttribute oa = new ObjAttribute(attrName, type, entity);
        oa.setDbAttributePath(da.getName());
        entity.addAttribute(oa);
        fireAttributeAdded(oa);
    }

    private boolean getRidOfAttributesThatAreNowSrcAttributesForRelationships(ObjEntity entity) {
        boolean changed = false;
        for (DbAttribute da : getMeaningfulFKs(entity)) {
            ObjAttribute oa = entity.getAttributeForDbAttribute(da);
            while (oa != null) {
                String attrName = oa.getName();
                entity.removeAttribute(attrName);
                changed = true;
                oa = entity.getAttributeForDbAttribute(da);
            }
        }
        return changed;
    }

    /**
     * Returns a list of DbAttributes that are mapped to foreign keys.
     *
     * @since 1.2
     */
    public Collection<DbAttribute> getMeaningfulFKs(ObjEntity objEntity) {
        List<DbAttribute> fks = new ArrayList<DbAttribute>(2);

        for (ObjAttribute property : objEntity.getAttributes()) {
            DbAttribute column = property.getDbAttribute();

            // check if adding it makes sense at all
            if (column != null && column.isForeignKey()) {
                fks.add(column);
            }
        }

        return fks;
    }

    /**
     * Returns a list of attributes that exist in the DbEntity, but are missing
     * from the ObjEntity.
     */
    protected List<DbAttribute> getAttributesToAdd(ObjEntity objEntity) {
        DbEntity dbEntity = objEntity.getDbEntity();

        List<DbAttribute> missing = new ArrayList<DbAttribute>();
        Collection<DbRelationship> incomingRels = getIncomingRelationships(dbEntity);

        for (DbAttribute dba : dbEntity.getAttributes()) {

            if (isMissingFromObjEntity(objEntity, dba, incomingRels)) {
                missing.add(dba);
            }
        }

        return missing;
    }

    protected boolean isMissingFromObjEntity(ObjEntity entity, DbAttribute dbAttribute, Collection<DbRelationship> incomingRels) {

        if (dbAttribute.getName() == null || entity.getAttributeForDbAttribute(dbAttribute) != null) {
            return false;
        }

        boolean removeMeaningfulPKs = removePK(dbAttribute.getEntity());
        if (removeMeaningfulPKs && dbAttribute.isPrimaryKey()) {
            return false;
        }

        // check FK's
        boolean isFK = false;
        Iterator<DbRelationship> rit = dbAttribute.getEntity().getRelationships().iterator();
        while (!isFK && rit.hasNext()) {
            DbRelationship rel = rit.next();
            for (DbJoin join : rel.getJoins()) {
                if (join.getSource() == dbAttribute) {
                    isFK = true;
                    break;
                }
            }
        }

        if (!removeMeaningfulPKs) {
            if (!dbAttribute.isPrimaryKey() && isFK) {
                return false;
            }
        } else {
            if (isFK) {
                return false;
            }
        }

        // check incoming relationships
        rit = incomingRels.iterator();
        while (!isFK && rit.hasNext()) {
            DbRelationship rel = rit.next();
            for (DbJoin join : rel.getJoins()) {
                if (join.getTarget() == dbAttribute) {
                    isFK = true;
                    break;
                }
            }
        }

        if (!removeMeaningfulPKs) {
            if (!dbAttribute.isPrimaryKey() && isFK) {
                return false;
            }
        } else {
            if (isFK) {
                return false;
            }
        }

        return true;
    }

    protected boolean isMissingFromObjEntity(ObjEntity entity, DbRelationship dbRelationship) {
        return dbRelationship.getName() != null && entity.getRelationshipForDbRelationship(dbRelationship) == null;
    }

    private Collection<DbRelationship> getIncomingRelationships(DbEntity entity) {
        Collection<DbRelationship> incoming = new ArrayList<DbRelationship>();

        for (DbEntity nextEntity : entity.getDataMap().getDbEntities()) {
            for (DbRelationship relationship : nextEntity.getRelationships()) {

                // TODO: PERFORMANCE 'getTargetEntity' is generally slow, called
                // in this iterator it is showing (e.g. in YourKit profiles)..
                // perhaps use cheaper 'getTargetEntityName()' or even better -
                // pre-cache all relationships by target entity to avoid O(n)
                // search ?
                // (need to profile to prove the difference)
                if (entity == relationship.getTargetEntity()) {
                    incoming.add(relationship);
                }
            }
        }

        return incoming;
    }

    protected List<DbRelationship> getRelationshipsToAdd(ObjEntity objEntity) {
        List<DbRelationship> missing = new ArrayList<DbRelationship>();
        for (DbRelationship dbRel : objEntity.getDbEntity().getRelationships()) {
            if (isMissingFromObjEntity(objEntity, dbRel)) {
                missing.add(dbRel);
            }
        }

        return missing;
    }

    /**
     * @since 1.2
     */
    public boolean isRemoveMeaningfulFKs() {
        return removeMeaningfulFKs;
    }

    /**
     * @since 1.2
     */
    public void setRemoveMeaningfulFKs(boolean removeMeaningfulFKs) {
        this.removeMeaningfulFKs = removeMeaningfulFKs;
    }

    /**
     * Registers new EntityMergeListener
     */
    public void addEntityMergeListener(EntityMergeListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregisters an EntityMergeListener
     */
    public void removeEntityMergeListener(EntityMergeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns registered listeners
     */
    public EntityMergeListener[] getEntityMergeListeners() {
        return listeners.toArray(new EntityMergeListener[listeners.size()]);
    }

    /**
     * Notifies all listeners that an ObjAttribute was added
     */
    protected void fireAttributeAdded(ObjAttribute attr) {
        for (EntityMergeListener listener : listeners) {
            listener.objAttributeAdded(attr);
        }
    }

    /**
     * Notifies all listeners that an ObjRelationship was added
     */
    protected void fireRelationshipAdded(ObjRelationship rel) {
        for (EntityMergeListener listener : listeners) {
            listener.objRelationshipAdded(rel);
        }
    }

    /**
     * @return naming strategy for reverse engineering
     */
    public ObjectNameGenerator getNameGenerator() {
        return nameGenerator;
    }

    /**
     * @since 4.0
     */
    public boolean isUsePrimitives() {
        return usePrimitives;
    }

    /**
     * @param usePrimitives
     * @since 4.0
     */
    public void setUsePrimitives(boolean usePrimitives) {
        this.usePrimitives = usePrimitives;
    }
}
