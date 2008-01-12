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
import java.util.Iterator;
import java.util.List;

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
import org.apache.cayenne.project.NamedObjectFactory;

/**
 * Implements methods for entity merging.
 * 
 * @author Andrus Adamchik
 */
public class EntityMergeSupport {

    protected DataMap map;
    protected boolean removeMeaningfulFKs;

    public EntityMergeSupport(DataMap map) {
        this.map = map;
        this.removeMeaningfulFKs = true;
    }

    /**
     * Updates each one of the collection of ObjEntities, adding attributes and
     * relationships based on the current state of its DbEntity.
     * 
     * @return true if any ObjEntity has changed as a result of synchronization.
     * 
     * @since 1.2 changed signature to use Collection instead of List.
     */
    public boolean synchronizeWithDbEntities(Collection<ObjEntity> objEntities) {
        boolean changed = false;
        for (ObjEntity nextEntity : objEntities) {
            if (synchronizeWithDbEntity(nextEntity)) {
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Updates ObjEntity attributes and relationships based on the current state of its
     * DbEntity.
     * 
     * @return true if the ObjEntity has changed as a result of synchronization.
     */
    public boolean synchronizeWithDbEntity(ObjEntity entity) {

        if (entity == null || entity.getDbEntity() == null) {
            return false;
        }

        boolean changed = false;

        // synchronization on DataMap is some (weak) protection
        // against simultaneous modification of the map (like double-clicking on sync
        // button)
        synchronized (map) {

            if (removeMeaningfulFKs) {

                // get rid of attributes that are now src attributes for relationships
                for (DbAttribute da : getMeaningfulFKs(entity)) {
                    ObjAttribute oa = entity.getAttributeForDbAttribute(da);
                    while (oa != null) {
                        String attrName = oa.getName();
                        entity.removeAttribute(attrName);
                        changed = true;
                        oa = entity.getAttributeForDbAttribute(da);
                    }
                }
            }

            // add missing attributes
            for (DbAttribute da : getAttributesToAdd(entity)) {
                String attrName = NameConverter.underscoredToJava(da.getName(), false);

                // avoid duplicate names
                attrName = NamedObjectFactory.createName(
                        ObjAttribute.class,
                        entity,
                        attrName);

                String type = TypesMapping.getJavaBySqlType(da.getType());

                ObjAttribute oa = new ObjAttribute(attrName, type, entity);
                oa.setDbAttributePath(da.getName());
                entity.addAttribute(oa);
                changed = true;
            }

            // add missing relationships
            for (DbRelationship dr : getRelationshipsToAdd(entity)) {
                DbEntity dbEntity = (DbEntity) dr.getTargetEntity();

                for (Entity mappedTarget : map.getMappedEntities(dbEntity)) {

                    // avoid duplicate names
                    String relationshipName = NameConverter.underscoredToJava(dr
                            .getName(), false);
                    relationshipName = NamedObjectFactory.createName(
                            ObjRelationship.class,
                            entity,
                            relationshipName);

                    ObjRelationship or = new ObjRelationship(relationshipName);
                    or.addDbRelationship(dr);
                    or.setSourceEntity(entity);
                    or.setTargetEntity(mappedTarget);
                    entity.addRelationship(or);
                    changed = true;
                }
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
     * Returns a list of attributes that exist in the DbEntity, but are missing from the
     * ObjEntity.
     */
    protected List<DbAttribute> getAttributesToAdd(ObjEntity objEntity) {
        List<DbAttribute> missing = new ArrayList<DbAttribute>();
        Collection<DbRelationship> rels = objEntity.getDbEntity().getRelationships();
        Collection<DbRelationship> incomingRels = getIncomingRelationships(objEntity.getDbEntity());

        for (DbAttribute dba : objEntity.getDbEntity().getAttributes()) {
            // already there
            if (objEntity.getAttributeForDbAttribute(dba) != null) {
                continue;
            }

            // check if adding it makes sense at all
            if (dba.getName() == null || dba.isPrimaryKey()) {
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

            if (isFK) {
                continue;
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
            
            if (isFK) {
                continue;
            }

            missing.add(dba);
        }

        return missing;
    }
    
    private Collection<DbRelationship> getIncomingRelationships(DbEntity entity) {
        Collection<DbRelationship> incoming = new ArrayList<DbRelationship>();
        
        for (DbEntity nextEntity : entity.getDataMap().getDbEntities()) {
            for (DbRelationship relationship : nextEntity.getRelationships()) {
                if (entity == relationship.getTargetEntity()) {
                    incoming.add(relationship);
                }
            }
        }
        
        return incoming;
    }

    protected List<DbRelationship> getRelationshipsToAdd(ObjEntity objEntity) {
        List<DbRelationship> missing = new ArrayList<DbRelationship>();
        for (DbRelationship dbrel : objEntity.getDbEntity().getRelationships()) {
            // check if adding it makes sense at all
            if (dbrel.getName() == null) {
                continue;
            }

            if (objEntity.getRelationshipForDbRelationship(dbrel) == null) {
                missing.add(dbrel);
            }
        }

        return missing;
    }

    public DataMap getMap() {
        return map;
    }

    public void setMap(DataMap map) {
        this.map = map;
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
}
