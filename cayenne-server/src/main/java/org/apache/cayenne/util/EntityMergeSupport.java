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

package org.apache.cayenne.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.naming.LegacyNameGenerator;
import org.apache.cayenne.map.naming.DefaultUniqueNameGenerator;
import org.apache.cayenne.map.naming.NameCheckers;
import org.apache.cayenne.map.naming.ObjectNameGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    protected boolean removeMeaningfulFKs;
    protected boolean removeMeaningfulPKs;
    protected boolean usePrimitives;

    /**
     * Strategy for choosing names for entities, attributes and relationships
     */
    private final ObjectNameGenerator nameGenerator;

    /**
     * Listeners of merge process.
     */
    private final List<EntityMergeListener> listeners = new ArrayList<EntityMergeListener>();

    public EntityMergeSupport(DataMap map) {
        this(map, new LegacyNameGenerator(), true);
    }

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

    private boolean addMissingRelationships(ObjEntity entity) {
        List<DbRelationship> relationshipsToAdd = getRelationshipsToAdd(entity);
        if (relationshipsToAdd.isEmpty()) {
            return false;
        }

        for (DbRelationship dr : relationshipsToAdd) {
            DbEntity targetEntity = dr.getTargetEntity();

            Collection<ObjEntity> mappedObjEntities = map.getMappedEntities(targetEntity);
            if (!mappedObjEntities.isEmpty()) {
                for (Entity mappedTarget : mappedObjEntities) {
                    createObjRelationship(entity, dr, mappedTarget.getName());
                }
            } else {
                LOG.warn("Can't find ObjEntity for " + dr.getTargetEntityName());
                LOG.warn("Db Relationship (" + dr + ") will have GUESSED Obj Relationship reflection. ");

                if (targetEntity == null) {
                    targetEntity = new DbEntity(dr.getTargetEntityName());
                }
                createObjRelationship(entity, dr, nameGenerator.createObjEntityName(targetEntity));
            }
        }
        return true;
    }

    private void createObjRelationship(ObjEntity entity, DbRelationship dr, String targetEntityName) {
        String relationshipName = nameGenerator.createObjRelationshipName(dr);
        relationshipName = DefaultUniqueNameGenerator.generate(NameCheckers.objRelationship, entity, relationshipName);

        ObjRelationship or = new ObjRelationship(relationshipName);
        or.addDbRelationship(dr);
        or.setSourceEntity(entity);
        or.setTargetEntityName(targetEntityName);
        entity.addRelationship(or);

        fireRelationshipAdded(or);
    }

    private boolean addMissingAttributes(ObjEntity entity) {
        boolean changed = false;
        for (DbAttribute da : getAttributesToAdd(entity)) {

            String attrName = DefaultUniqueNameGenerator.generate(NameCheckers.objAttribute, entity,
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
            changed = true;
        }
        return changed;
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

        Collection<DbRelationship> rels = dbEntity.getRelationships();
        Collection<DbRelationship> incomingRels = getIncomingRelationships(dbEntity);

        for (DbAttribute dba : dbEntity.getAttributes()) {

            if (dba.getName() == null || objEntity.getAttributeForDbAttribute(dba) != null) {
                continue;
            }

            boolean removeMeaningfulPKs = removePK(dbEntity);
            if (removeMeaningfulPKs && dba.isPrimaryKey()) {
                continue;
            }

            // check FK's
            boolean isFK = false;
            Iterator<DbRelationship> rit = rels.iterator();
            while (!isFK && rit.hasNext()) {
                DbRelationship rel = rit.next();
                for (DbJoin join : rel.getJoins()) {
                    if (join.getSource() == dba) {
                        isFK = true;
                        break;
                    }
                }
            }

            if (!removeMeaningfulPKs) {
                if (!dba.isPrimaryKey() && isFK) {
                    continue;
                }
            } else {
                if (isFK) {
                    continue;
                }
            }

            // check incoming relationships
            rit = incomingRels.iterator();
            while (!isFK && rit.hasNext()) {
                DbRelationship rel = rit.next();
                for (DbJoin join : rel.getJoins()) {
                    if (join.getTarget() == dba) {
                        isFK = true;
                        break;
                    }
                }
            }

            if (!removeMeaningfulPKs) {
                if (!dba.isPrimaryKey() && isFK) {
                    continue;
                }
            } else {
                if (isFK) {
                    continue;
                }
            }

            missing.add(dba);
        }

        return missing;
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
            // check if adding it makes sense at all
            if (dbRel.getName() == null) {
                continue;
            }

            if (objEntity.getRelationshipForDbRelationship(dbRel) == null) {
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
     * @since 4.0
     * @param usePrimitives
     */
    public void setUsePrimitives(boolean usePrimitives) {
        this.usePrimitives = usePrimitives;
    }
}
