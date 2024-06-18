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

package org.apache.cayenne.dbsync.merge.context;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.value.Json;
import org.apache.cayenne.value.Wkt;
import org.apache.cayenne.dbsync.filter.NameFilter;
import org.apache.cayenne.dbsync.model.DetectedDbAttribute;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.EntityMergeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implements methods for entity merging.
 */
public class EntityMergeSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityMergeSupport.class);

    private static final Map<String, String> CLASS_TO_PRIMITIVE = new HashMap<>();

    /**
     * Type conversion to Java 8 types (now it's only java.time.* types)
     */
    private static final Map<Integer, String> SQL_TYPE_TO_JAVA8_TYPE = new HashMap<>();

    /**
     * Type conversion for the most spread DB data types that are out of the standard
     */
    private static final Map<String, String> SQL_ADDITIONAL_TYPES_TO_JAVA_TYPE = new HashMap<>();

    static {
        CLASS_TO_PRIMITIVE.put(Byte.class.getName(), "byte");
        CLASS_TO_PRIMITIVE.put(Long.class.getName(), "long");
        CLASS_TO_PRIMITIVE.put(Double.class.getName(), "double");
        CLASS_TO_PRIMITIVE.put(Boolean.class.getName(), "boolean");
        CLASS_TO_PRIMITIVE.put(Float.class.getName(), "float");
        CLASS_TO_PRIMITIVE.put(Short.class.getName(), "short");
        CLASS_TO_PRIMITIVE.put(Integer.class.getName(), "int");

        SQL_TYPE_TO_JAVA8_TYPE.put(Types.DATE,      "java.time.LocalDate");
        SQL_TYPE_TO_JAVA8_TYPE.put(Types.TIME,      "java.time.LocalTime");
        SQL_TYPE_TO_JAVA8_TYPE.put(Types.TIMESTAMP, "java.time.LocalDateTime");

        SQL_ADDITIONAL_TYPES_TO_JAVA_TYPE.put("json",       Json.class.getName());
        SQL_ADDITIONAL_TYPES_TO_JAVA_TYPE.put("geometry",   Wkt.class.getName());
    }

    private ObjectNameGenerator nameGenerator;
    private final List<EntityMergeListener> listeners;
    private final boolean removingMeaningfulFKs;
    private final NameFilter meaningfulPKsFilter;
    private final boolean usingJava7Types;

    public EntityMergeSupport(ObjectNameGenerator nameGenerator,
                              NameFilter meaningfulPKsFilter,
                              boolean removingMeaningfulFKs,
                              boolean usingJava7Types) {

        this.listeners = new ArrayList<>();
        this.nameGenerator = nameGenerator;
        this.removingMeaningfulFKs = removingMeaningfulFKs;
        this.meaningfulPKsFilter = meaningfulPKsFilter;
        this.usingJava7Types = usingJava7Types;

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
            if (removingMeaningfulFKs) {
                getRidOfAttributesThatAreNowSrcAttributesForRelationships(entity);
            }
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

        boolean needGeneratedEntity = !objEntities.containsKey(targetEntityName);
        boolean hasFlattingAttributes = objEntities.values()
                .stream()
                .flatMap(ent -> ent.getAttributes().stream())
                .map(ObjAttribute::getDbAttributePath)
                .filter(Objects::nonNull)
                .filter(path -> path.length() > 1)
                .anyMatch(path -> path.first().value().equals(dr.getName()));

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
                boolean needGeneratedEntity = createObjRelationship(entity, dbRelationship, nameGenerator.objEntityName(targetEntity));
                if (needGeneratedEntity) {
                    LOGGER.warn("Can't find ObjEntity for " + dbRelationship.getTargetEntityName());
                    LOGGER.warn("Db Relationship (" + dbRelationship + ") will have GUESSED Obj Relationship reflection. ");
                }
            }
        } else {
            for (ObjEntity mappedTarget : mappedObjEntities) {
                createObjRelationship(entity, dbRelationship, mappedTarget.getName());
            }
        }
    }

    private void addMissingAttribute(ObjEntity entity, DbAttribute da) {
        ObjAttribute oa = new ObjAttribute();
        oa.setName(NameBuilder.builder(oa, entity).baseName(nameGenerator.objAttributeName(da)).name());
        oa.setEntity(entity);
        oa.setType(getTypeForObjAttribute(da));
        oa.setDbAttributePath(da.getName());
        entity.addAttribute(oa);
        fireAttributeAdded(oa);
    }

    private String getTypeForObjAttribute(DbAttribute dbAttribute) {
        String java8Type;
        if(!usingJava7Types && (java8Type = SQL_TYPE_TO_JAVA8_TYPE.get(dbAttribute.getType())) != null) {
            return java8Type;
        }

        // Check additional common DB types, like 'json' or 'geometry'
        if(dbAttribute instanceof DetectedDbAttribute) {
            DetectedDbAttribute detectedDbAttribute = (DetectedDbAttribute)dbAttribute;
            String jdbcTypeName = detectedDbAttribute.getJdbcTypeName();
            if(jdbcTypeName != null) {
                String type = SQL_ADDITIONAL_TYPES_TO_JAVA_TYPE.get(jdbcTypeName.toLowerCase());
                if (type != null) {
                    return type;
                }
            }
        }

        String type = TypesMapping.getJavaBySqlType(dbAttribute.getType());
        String primitiveType = CLASS_TO_PRIMITIVE.get(type);
        // use primitive types for non nullable attributes
        if (primitiveType != null && dbAttribute.isMandatory()) {
            return primitiveType;
        }
        return type;
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
    private List<DbAttribute> getAttributesToAdd(ObjEntity objEntity) {
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

    private boolean shouldAddToObjEntity(ObjEntity entity, DbAttribute dbAttribute, Collection<DbRelationship> incomingRels) {
        if (dbAttribute.getName() == null || entity.getAttributeForDbAttribute(dbAttribute) != null) {
            return false;
        }

        boolean addMeaningfulPK = meaningfulPKsFilter.isIncluded(entity.getDbEntityName());
        if (dbAttribute.isPrimaryKey()) {
            return addMeaningfulPK;
        }

        // check FK's
        if(isFK(dbAttribute, dbAttribute.getEntity().getRelationships(), true)) {
            return false;
        }
        // check incoming relationships
        return !isFK(dbAttribute, incomingRels, false);
    }

    private boolean isFK(DbAttribute dbAttribute, Collection<DbRelationship> collection, boolean source) {
        for (DbRelationship rel : collection) {
            for (DbJoin join : rel.getJoins()) {
                DbAttribute joinAttribute = source ? join.getSource() : join.getTarget();
                if (joinAttribute == dbAttribute) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldAddToObjEntity(ObjEntity entity, DbRelationship dbRelationship) {
        if(dbRelationship.getName() == null) {
            return false;
        }

        for(ObjRelationship objRelationship : entity.getRelationships()) {
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
            // attribute can be null, see DbJoin.getSource() and DbJoin.getTarget()
            if(attr1 == null) {
                if(attr2 != null) {
                    return false;
                }
                continue;
            }
            if(attr2 == null) {
                return false;
            }
            // name is unlikely to be null, but we don't want NPE anyway
            if(attr1.getName() == null) {
                if(attr2.getName() != null) {
                    return false;
                }
                continue;
            }
            if(!attr1.getName().equals(attr2.getName())) {
                return false;
            }
        }

        return true;
    }

    private Collection<DbRelationship> getIncomingRelationships(DbEntity entity) {
        Collection<DbRelationship> incoming = new ArrayList<>();

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
        List<DbRelationship> missing = new ArrayList<>();
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
    private void fireAttributeAdded(ObjAttribute attr) {
        for (EntityMergeListener listener : listeners) {
            listener.objAttributeAdded(attr);
        }
    }

    /**
     * Notifies all listeners that an ObjRelationship was added
     */
    private void fireRelationshipAdded(ObjRelationship rel) {
        for (EntityMergeListener listener : listeners) {
            listener.objRelationshipAdded(rel);
        }
    }

    public void setNameGenerator(ObjectNameGenerator nameGenerator) {
        this.nameGenerator = nameGenerator;
    }
}
