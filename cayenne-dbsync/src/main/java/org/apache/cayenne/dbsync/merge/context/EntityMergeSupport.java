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

package org.apache.cayenne.dbsync.merge.context;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.dbsync.filter.NameFilter;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
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

    private static final Log LOGGER = LogFactory.getLog(EntityMergeSupport.class);

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

    private final ObjectNameGenerator nameGenerator;
    private final List<EntityMergeListener> listeners;
    private final boolean removingMeaningfulFKs;
    private final NameFilter meaningfulPKsFilter;
    private final boolean usingPrimitives;

    public EntityMergeSupport(ObjectNameGenerator nameGenerator,
                              NameFilter meaningfulPKsFilter,
                              boolean removingMeaningfulFKs,
                              boolean usingPrimitives) {

        this.listeners = new ArrayList<>();
        this.nameGenerator = nameGenerator;
        this.removingMeaningfulFKs = removingMeaningfulFKs;
        this.meaningfulPKsFilter = meaningfulPKsFilter;
        this.usingPrimitives = usingPrimitives;

        // will ensure that all created ObjRelationships would have
        // default delete rule
        addEntityMergeListener(DeleteRuleUpdater.getEntityMergeListener());
    }

    public boolean isRemovingMeaningfulFKs() {
        return removingMeaningfulFKs;
    }


    /**
     * Updates each one of the collection of ObjEntities, adding attributes and
     * relationships based on the current state of its DbEntity.
     *
     * @return true if any ObjEntity has changed as a result of synchronization.
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

        if (removingMeaningfulFKs) {
            changed = getRidOfAttributesThatAreNowSrcAttributesForRelationships(entity);
        }

        changed |= addMissingAttributes(entity);
        changed |= addMissingRelationships(entity);

        return changed;
    }

    /**
     * @since 4.0
     */
    public boolean synchronizeOnDbAttributeAdded(ObjEntity entity, DbAttribute dbAttribute) {

        Collection<DbRelationship> incomingRels = getIncomingRelationships(dbAttribute.getEntity());
        if (shouldAddToObjEntity(entity, dbAttribute, incomingRels)) {
            addMissingAttribute(entity, dbAttribute);
            return true;
        }

        return false;
    }

    /**
     * @since 4.0
     */
    public boolean synchronizeOnDbRelationshipAdded(ObjEntity entity, DbRelationship dbRelationship) {

        if (shouldAddToObjEntity(entity, dbRelationship)) {
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
        ObjRelationship or = new ObjRelationship();
        or.setName(NameBuilder.builder(or, entity)
                .baseName(nameGenerator.relationshipName(dr))
                .name());

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

        // getting DataMap from DbRelationship's source entity. This is the only object in our arguments that
        // is guaranteed to be a part of the map....
        DataMap dataMap = dbRelationship.getSourceEntity().getDataMap();

        DbEntity targetEntity = dbRelationship.getTargetEntity();
        Collection<ObjEntity> mappedObjEntities = dataMap.getMappedEntities(targetEntity);
        if (mappedObjEntities.isEmpty()) {
            if (targetEntity == null) {
                targetEntity = new DbEntity(dbRelationship.getTargetEntityName());
            }

            if (dbRelationship.getTargetEntityName() != null) {
                boolean needGeneratedEntity = createObjRelationship(entity, dbRelationship,
                        nameGenerator.objEntityName(targetEntity));
                if (needGeneratedEntity) {
                    LOGGER.warn("Can't find ObjEntity for " + dbRelationship.getTargetEntityName());
                    LOGGER.warn("Db Relationship (" + dbRelationship + ") will have GUESSED Obj Relationship reflection. ");
                }
            }
        } else {
            for (Entity mappedTarget : mappedObjEntities) {
                createObjRelationship(entity, dbRelationship, mappedTarget.getName());
            }
        }
    }

    private void addMissingAttribute(ObjEntity entity, DbAttribute da) {
        ObjAttribute oa = new ObjAttribute();
        oa.setName(NameBuilder.builder(oa, entity)
                .baseName(nameGenerator.objAttributeName(da))
                .name());
        oa.setEntity(entity);

        String type = TypesMapping.getJavaBySqlType(da.getType());
        if (usingPrimitives) {
            String primitive = CLASS_TO_PRIMITIVE.get(type);
            if (primitive != null) {
                type = primitive;
            }
        }
        oa.setType(type);
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
        List<DbAttribute> fks = new ArrayList<>(2);

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

        List<DbAttribute> missing = new ArrayList<>();
        Collection<DbRelationship> incomingRels = getIncomingRelationships(dbEntity);

        for (DbAttribute dba : dbEntity.getAttributes()) {

            if (shouldAddToObjEntity(objEntity, dba, incomingRels)) {
                missing.add(dba);
            }
        }

        return missing;
    }

    protected boolean shouldAddToObjEntity(ObjEntity entity, DbAttribute dbAttribute, Collection<DbRelationship> incomingRels) {

        if (dbAttribute.getName() == null || entity.getAttributeForDbAttribute(dbAttribute) != null) {
            return false;
        }

        boolean addMeaningfulPK = meaningfulPKsFilter.isIncluded(entity.getDbEntityName());

        if (dbAttribute.isPrimaryKey() && !addMeaningfulPK) {
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

        if (addMeaningfulPK) {
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

        if (addMeaningfulPK) {
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

    private boolean shouldAddToObjEntity(ObjEntity entity, DbRelationship dbRelationship) {
        if(dbRelationship.getName() == null) {
            return false;
        }

        for(Relationship relationship : entity.getRelationships()) {
            ObjRelationship objRelationship = (ObjRelationship)relationship;
            if(objRelationshipHasDbRelationship(objRelationship, dbRelationship)) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return true if objRelationship includes given dbRelationship
     */
    private boolean objRelationshipHasDbRelationship(ObjRelationship objRelationship, DbRelationship dbRelationship) {
        for(DbRelationship relationship : objRelationship.getDbRelationships()) {

            if(relationship.getSourceEntityName().equals(dbRelationship.getSourceEntityName())
                    && relationship.getTargetEntityName().equals(dbRelationship.getTargetEntityName())
                    && isSameAttributes(relationship.getSourceAttributes(), dbRelationship.getSourceAttributes())
                    && isSameAttributes(relationship.getTargetAttributes(), dbRelationship.getTargetAttributes())) {
                return true;
            }

        }
        return false;
    }


    /**
     * @param collection1 first collection to compare
     * @param collection2 second collection to compare
     * @return true if collections have same size and attributes in them have same names
     */
    private boolean isSameAttributes(Collection<DbAttribute> collection1, Collection<DbAttribute> collection2) {
        if(collection1.size() != collection2.size()) {
            return false;
        }

        if(collection1.isEmpty()) {
            return true;
        }

        Iterator<DbAttribute> iterator1 = collection1.iterator();
        Iterator<DbAttribute> iterator2 = collection2.iterator();
        for(int i=0; i<collection1.size(); i++) {
            DbAttribute attr1 = iterator1.next();
            DbAttribute attr2 = iterator2.next();
            if(!attr1.getName().equals(attr2.getName())) {
                return false;
            }
        }

        return true;
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
            if (shouldAddToObjEntity(objEntity, dbRel)) {
                missing.add(dbRel);
            }
        }

        return missing;
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
}
