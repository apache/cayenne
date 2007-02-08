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

package org.apache.cayenne.regression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.collections.map.LinkedMap;
import org.objectstyle.ashwood.graph.ArcIterator;
import org.objectstyle.ashwood.graph.Digraph;
import org.objectstyle.ashwood.graph.GraphUtils;
import org.objectstyle.ashwood.graph.MapDigraph;
import org.objectstyle.ashwood.random.Roulette;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * DataModificationRobot performs randomized inserts/deletes/updates of data objects in a
 * data context. Usually it works in conjuction with RandomDomainBuilder but can be used
 * independently for suitable schemas This class works for any kind of DataObjects and
 * tests not only basic create/delete object but also referential integrity dependencies
 * (multi-reflexive too). It always tries to generate correct data, therefore possible
 * commit failures mostly point out problems with the Cayenne commit algorithms.
 * 
 * @author Andriy Shapochka
 */

public class DataModificationRobot {

    private DataContext context;
    private Roulette insertionRandomizer;
    private Roulette deletionRandomizer;
    private List objEntities;
    private Map objectsByEntity = new LinkedMap();
    private Random randomizer;

    public DataModificationRobot(DataContext context, Random randomizer,
            int newObjectPerEntityCount, int deleteObjectPerEntityCount) {
        this.context = context;
        this.randomizer = randomizer;
        objEntities = new ArrayList(32);
        for (Iterator i = context.getEntityResolver().getDataMaps().iterator(); i
                .hasNext();) {
            DataMap dataMap = (DataMap) i.next();
            objEntities.addAll(dataMap.getObjEntities());
        }
        insertionRandomizer = new Roulette(
                objEntities.size(),
                newObjectPerEntityCount,
                randomizer);
        deletionRandomizer = new Roulette(
                objEntities.size(),
                deleteObjectPerEntityCount,
                randomizer);
    }

    public void generate() {
        insertRandomData();
        deleteRandomData();
    }

    private void insertRandomData() {
        Set usedFlattenedRels = new HashSet();
        while (insertionRandomizer.hasNext()) {
            Integer entityIndex = (Integer) insertionRandomizer.next();
            ObjEntity entity = (ObjEntity) objEntities.get(entityIndex.intValue());
            DataObject o = context.createAndRegisterNewObject(entity.getName());
            List objects = (List) objectsByEntity.get(entity);
            if (objects == null) {
                objects = new ArrayList();
                objectsByEntity.put(entity, objects);
            }
            objects.add(o);
        }
        for (Iterator i = objectsByEntity.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            ObjEntity entity = (ObjEntity) entry.getKey();
            List objects = (List) entry.getValue();
            Collection relationships = entity.getRelationships();
            ObjRelationship dependentPkRelation = null;
            for (Iterator k = relationships.iterator(); k.hasNext();) {
                ObjRelationship r = (ObjRelationship) k.next();
                if (entity.equals(r.getTargetEntity()))
                    continue;
                if (!r.isToMany() && r.getReverseRelationship().isToDependentEntity()) {
                    dependentPkRelation = r;
                    List targetObjects = (List) objectsByEntity.get(dependentPkRelation
                            .getTargetEntity());
                    Roulette masterPkEntitySelector = new Roulette(
                            targetObjects.size(),
                            1,
                            randomizer);
                    for (Iterator j = objects.iterator(); j.hasNext();) {
                        DataObject o = (DataObject) j.next();
                        int targetIndex = ((Integer) masterPkEntitySelector.next())
                                .intValue();
                        o.setToOneTarget(
                                dependentPkRelation.getName(),
                                (DataObject) targetObjects.get(targetIndex),
                                true);
                    }
                    break;
                }
            }

            for (Iterator j = objects.iterator(); j.hasNext();) {
                DataObject o = (DataObject) j.next();
                for (Iterator k = relationships.iterator(); k.hasNext();) {
                    ObjRelationship r = (ObjRelationship) k.next();
                    if (entity.equals(r.getTargetEntity()))
                        continue;
                    if (r == dependentPkRelation || r.isToMany())
                        continue;
                    List targetObjects = (List) objectsByEntity.get(r.getTargetEntity());
                    int targetIndex = randomizer.nextInt(targetObjects.size());
                    o.setToOneTarget(r.getName(), (DataObject) targetObjects
                            .get(targetIndex), true);
                }
            }

            generateRelationshipsForReflexive(objects, entity);

            for (Iterator k = relationships.iterator(); k.hasNext();) {
                ObjRelationship r = (ObjRelationship) k.next();
                if (!r.isFlattened() || usedFlattenedRels.contains(r))
                    continue;
                usedFlattenedRels.add(r);
                usedFlattenedRels.add(r.getReverseRelationship());
                List targetObjects = (List) objectsByEntity.get(r.getTargetEntity());
                for (Iterator j = objects.iterator(); j.hasNext();) {
                    DataObject o = (DataObject) j.next();
                    Roulette targetSelector = new Roulette(
                            targetObjects.size(),
                            1,
                            randomizer);
                    for (int l = 0; l < targetObjects.size() / 5; l++) {
                        int targetIndex = ((Integer) targetSelector.next()).intValue();
                        o.addToManyTarget(r.getName(), (DataObject) targetObjects
                                .get(targetIndex), true);
                    }
                }
            }
        }
    }

    private void generateRelationshipsForReflexive(List objects, ObjEntity entity) {
        int count = objects.size();
        if (count < 2)
            return;
        List reflexiveRels = new ArrayList(3);
        Collection relationships = entity.getRelationships();
        for (Iterator k = relationships.iterator(); k.hasNext();) {
            ObjRelationship r = (ObjRelationship) k.next();
            if (!r.isToMany() && entity.equals(r.getTargetEntity()))
                reflexiveRels.add(r.getName());
        }
        if (reflexiveRels.isEmpty())
            return;
        Digraph graph = new MapDigraph(MapDigraph.HASHMAP_FACTORY);
        GraphUtils.randomizeAcyclic(
                graph,
                count - 1,
                reflexiveRels.size(),
                count - 1,
                randomizer);
        DataObject referencedObjectForUnusedRels = (DataObject) objects.get(0);
        for (Iterator i = reflexiveRels.iterator(); i.hasNext();) {
            String relName = (String) i.next();
            referencedObjectForUnusedRels.setToOneTarget(
                    relName,
                    referencedObjectForUnusedRels,
                    true);
        }
        for (Iterator i = graph.vertexIterator(); i.hasNext();) {
            Object vertex = i.next();
            int objectIndex = ((Number) vertex).intValue();
            DataObject referencingObject = (DataObject) objects.get(objectIndex);
            Iterator relIt = reflexiveRels.iterator();
            for (ArcIterator j = graph.incomingIterator(vertex); j.hasNext();) {
                j.next();
                String relName = (String) relIt.next();

                // Andrus: this line performs an assignment to the variable that
                // is never used. Commented out for this reason
                // Object origin = j.getOrigin();
                int referencedObjectIndex = ((Number) vertex).intValue();
                DataObject referencedObject = (DataObject) objects
                        .get(referencedObjectIndex);
                referencingObject.setToOneTarget(relName, referencedObject, true);
            }
            while (relIt.hasNext()) {
                String relName = (String) relIt.next();
                referencingObject.setToOneTarget(
                        relName,
                        referencedObjectForUnusedRels,
                        true);
            }
        }
    }

    private void deleteRandomData() {
        Map objectsByObjEntity = new HashMap();
        Iterator it = context.getObjectStore().getObjectIterator();
        while (it.hasNext()) {
            DataObject o = (DataObject) it.next();
            ObjEntity entity = context.getEntityResolver().lookupObjEntity(
                    o.getObjectId().getEntityName());
            List objectsForObjEntity = (List) objectsByObjEntity.get(entity);
            if (objectsForObjEntity == null) {
                objectsForObjEntity = new LinkedList();
                objectsByObjEntity.put(entity, objectsForObjEntity);
            }
            objectsForObjEntity.add(o);
        }

        while (deletionRandomizer.hasNext()) {
            Integer entityIndex = (Integer) deletionRandomizer.next();
            ObjEntity entity = (ObjEntity) objEntities.get(entityIndex.intValue());
            List objects = (List) objectsByObjEntity.get(entity);
            if (objects.size() <= 1)
                continue;
            int objectIndex = randomizer.nextInt(objects.size());
            DataObject objectToDelete = (DataObject) objects.remove(objectIndex);
            DataObject dependentsTakeOver = (DataObject) objects.get(0);
            Collection relationships = entity.getRelationships();
            for (Iterator i = relationships.iterator(); i.hasNext();) {
                ObjRelationship relation = (ObjRelationship) i.next();
                if (!relation.isToMany())
                    continue;
                List dependentObjects = (List) objectToDelete
                        .readPropertyDirectly(relation.getName());
                if (dependentObjects == null || dependentObjects.isEmpty())
                    continue;
                dependentObjects = new ArrayList(dependentObjects);
                if (relation.isToDependentEntity() && !relation.isFlattened()) {
                    for (Iterator j = dependentObjects.iterator(); j.hasNext();) {
                        DataObject dependent = (DataObject) j.next();
                        context.deleteObject(dependent);
                    }
                }
                else if (entity.equals(relation.getTargetEntity())) {
                    ObjRelationship reverse = relation.getReverseRelationship();
                    // removeFromReflexiveToOne(objectToDelete, objects,
                    // reverse.getName());
                    DataObject master = (DataObject) objectToDelete
                            .readPropertyDirectly(reverse.getName());
                    for (Iterator j = dependentObjects.iterator(); j.hasNext();) {
                        DataObject dependent = (DataObject) j.next();
                        if (objectToDelete.equals(dependent))
                            continue;
                        objectToDelete.removeToManyTarget(
                                relation.getName(),
                                dependent,
                                true);
                        if (master == null
                                || master.getPersistenceState() == PersistenceState.DELETED
                                || objectToDelete.equals(master))
                            dependent
                                    .addToManyTarget(relation.getName(), dependent, true);
                        else
                            master.addToManyTarget(relation.getName(), dependent, true);
                        dependent.setToOneTarget(reverse.getName(), dependent, true);
                    }
                }
                else {
                    for (Iterator j = dependentObjects.iterator(); j.hasNext();) {
                        DataObject dependent = (DataObject) j.next();
                        objectToDelete.removeToManyTarget(
                                relation.getName(),
                                dependent,
                                true);
                        if (!relation.isFlattened())
                            dependentsTakeOver.addToManyTarget(
                                    relation.getName(),
                                    dependent,
                                    true);
                    }
                }
            }
            context.deleteObject(objectToDelete);
        }
    }

    /*
     * private void removeFromReflexiveToOne(Object master, List objects, String
     * objRelName) { for (Iterator i = objects.iterator(); i.hasNext(); ) { DataObject
     * object = (DataObject)i.next(); if
     * (master.equals(object.readPropertyDirectly(objRelName))) {
     * object.setToOneTarget(objRelName, object, true); } } }
     */

}
