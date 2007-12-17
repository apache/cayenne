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
     * @since 1.2 changed signature to use Collection instead of List.
     */
    public boolean synchronizeWithDbEntities(Collection objEntities) {
        boolean changed = false;
        Iterator it = objEntities.iterator();
        while (it.hasNext()) {
            if (synchronizeWithDbEntity((ObjEntity) it.next())) {
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
        // against simulteneous modification of the map (like double-clicking on sync
        // button)
        synchronized (map) {

            if (removeMeaningfulFKs) {

                // get rid of attributes that are now src attributes for relationships
                Iterator rait = getMeaningfulFKs(entity).iterator();
                while (rait.hasNext()) {
                    DbAttribute da = (DbAttribute) rait.next();
                    ObjAttribute oa = (ObjAttribute) entity.getAttributeForDbAttribute(da);
                    while (oa != null) {
                        String attrName = oa.getName();
                        entity.removeAttribute(attrName);
                        changed = true;
                        oa = (ObjAttribute) entity.getAttributeForDbAttribute(da);
                    }
                }
            }

            List addAttributes = getAttributesToAdd(entity);

            // add missing attributes
            Iterator ait = addAttributes.iterator();
            while (ait.hasNext()) {
                DbAttribute da = (DbAttribute) ait.next();
                String attrName = NameConverter.underscoredToJava(da.getName(), false);

                // avoid duplicate names
                attrName = NamedObjectFactory.createName(
                        ObjAttribute.class,
                        entity,
                        attrName);

                String type = TypesMapping.getJavaBySqlType(da.getType());

                ObjAttribute oa = new ObjAttribute(attrName, type, entity);
                oa.setDbAttribute(da);
                entity.addAttribute(oa);
                changed = true;
            }

            List addRelationships = getRelationshipsToAdd(entity);

            // add missing relationships
            Iterator rit = addRelationships.iterator();
            while (rit.hasNext()) {
                DbRelationship dr = (DbRelationship) rit.next();
                DbEntity dbEntity = (DbEntity) dr.getTargetEntity();

                Iterator targets = map.getMappedEntities(dbEntity).iterator();
                if (targets.hasNext()) {

                    Entity mappedTarget = (Entity) targets.next();

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
     * Returns a list of ObjAttributes that are mapped to foreign keys.
     * 
     * @since 1.2
     */
    public Collection getMeaningfulFKs(ObjEntity objEntity) {
        List fks = new ArrayList(2);
        Iterator it = objEntity.getAttributes().iterator();
        while (it.hasNext()) {
            ObjAttribute property = (ObjAttribute) it.next();
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
    protected List getAttributesToAdd(ObjEntity objEntity) {
        List missing = new ArrayList();
        Iterator it = objEntity.getDbEntity().getAttributes().iterator();
        Collection rels = objEntity.getDbEntity().getRelationships();
        Collection incomingRels = getIncomingRelationships(objEntity.getDbEntity());

        while (it.hasNext()) {
            DbAttribute dba = (DbAttribute) it.next();
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
            Iterator rit = rels.iterator();
            while (!isFK && rit.hasNext()) {
                DbRelationship rel = (DbRelationship) rit.next();
                Iterator jit = rel.getJoins().iterator();
                while (jit.hasNext()) {
                    DbJoin join = (DbJoin) jit.next();
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
                DbRelationship rel = (DbRelationship) rit.next();
                Iterator jit = rel.getJoins().iterator();
                while (jit.hasNext()) {
                    DbJoin join = (DbJoin) jit.next();
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
    
    private Collection getIncomingRelationships(DbEntity entity) {
        
        Collection incoming = new ArrayList();
        Iterator entities = entity.getDataMap().getDbEntities().iterator();
        while(entities.hasNext()) {
            DbEntity nextEntity = (DbEntity) entities.next();
            
            Iterator relationships = nextEntity.getRelationships().iterator();
            while(relationships.hasNext()) {
                DbRelationship relationship = (DbRelationship) relationships.next();
                if(entity == relationship.getTargetEntity()) {
                    incoming.add(relationship);
                }
            }
        }
        
        return incoming;
    }

    protected List getRelationshipsToAdd(ObjEntity objEntity) {
        List missing = new ArrayList();
        Iterator it = objEntity.getDbEntity().getRelationships().iterator();
        while (it.hasNext()) {
            DbRelationship dbrel = (DbRelationship) it.next();
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
