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

package org.apache.cayenne.ashwood;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.ashwood.graph.Digraph;
import org.apache.cayenne.ashwood.graph.IndegreeTopologicalSort;
import org.apache.cayenne.ashwood.graph.MapDigraph;
import org.apache.cayenne.ashwood.graph.StrongConnection;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.commons.collections.comparators.ReverseComparator;

/**
 * Implements dependency sorting algorithms for ObjEntities, DbEntities and
 * DataObjects. Presently it works for acyclic database schemas with possible
 * multi-reflexive tables.
 * 
 * @since 3.1
 */
public class AshwoodEntitySorter implements EntitySorter {

    protected EntityResolver entityResolver;
    protected Map<DbEntity, ComponentRecord> components;
    protected Map<DbEntity, List<DbRelationship>> reflexiveDbEntities;

    protected Comparator<DbEntity> dbEntityComparator;
    protected Comparator<ObjEntity> objEntityComparator;

    private volatile boolean dirty;

    public AshwoodEntitySorter() {
        dbEntityComparator = new DbEntityComparator();
        objEntityComparator = new ObjEntityComparator();
        dirty = true;
    }

    /**
     * Reindexes internal sorter in a thread-safe manner.
     */
    protected void indexSorter() {

        // correct double check locking per Joshua Bloch
        // http://java.sun.com/developer/technicalArticles/Interviews/bloch_effective_08_qa.html
        // (maybe we should use something like CountDownLatch or a Cyclic
        // barrier
        // instead?)

        boolean localDirty = dirty;
        if (localDirty) {
            synchronized (this) {
                localDirty = dirty;
                if (localDirty) {
                    doIndexSorter();
                    dirty = false;
                }
            }
        }
    }

    /**
     * Reindexes internal sorter without synchronization.
     */
    protected void doIndexSorter() {

        Map<DbEntity, List<DbRelationship>> reflexiveDbEntities = new HashMap<DbEntity, List<DbRelationship>>(32);

        Digraph<DbEntity, List<DbAttribute>> referentialDigraph = new MapDigraph<DbEntity, List<DbAttribute>>();

        Map<String, DbEntity> tableMap = new HashMap<String, DbEntity>();

        if (entityResolver != null) {
            for (DbEntity entity : entityResolver.getDbEntities()) {
                tableMap.put(entity.getFullyQualifiedName(), entity);
                referentialDigraph.addVertex(entity);
            }
        }

        for (DbEntity destination : tableMap.values()) {
            for (DbRelationship candidate : destination.getRelationships()) {
                if ((!candidate.isToMany() && !candidate.isToDependentPK()) || candidate.isToMasterPK()) {
                    DbEntity origin = (DbEntity) candidate.getTargetEntity();
                    boolean newReflexive = destination.equals(origin);

                    for (DbJoin join : candidate.getJoins()) {
                        DbAttribute targetAttribute = join.getTarget();
                        if (targetAttribute.isPrimaryKey()) {

                            if (newReflexive) {
                                List<DbRelationship> reflexiveRels = reflexiveDbEntities.get(destination);
                                if (reflexiveRels == null) {
                                    reflexiveRels = new ArrayList<DbRelationship>(1);
                                    reflexiveDbEntities.put(destination, reflexiveRels);
                                }
                                reflexiveRels.add(candidate);
                                newReflexive = false;
                            }

                            List<DbAttribute> fks = referentialDigraph.getArc(origin, destination);
                            if (fks == null) {
                                fks = new ArrayList<DbAttribute>();
                                referentialDigraph.putArc(origin, destination, fks);
                            }

                            fks.add(targetAttribute);
                        }
                    }
                }
            }

        }

        StrongConnection<DbEntity, List<DbAttribute>> contractor = new StrongConnection<DbEntity, List<DbAttribute>>(
                referentialDigraph);

        Digraph<Collection<DbEntity>, Collection<List<DbAttribute>>> contractedReferentialDigraph = new MapDigraph<Collection<DbEntity>, Collection<List<DbAttribute>>>();
        contractor.contract(contractedReferentialDigraph);

        IndegreeTopologicalSort<Collection<DbEntity>> sorter = new IndegreeTopologicalSort<Collection<DbEntity>>(
                contractedReferentialDigraph);

        Map<DbEntity, ComponentRecord> components = new HashMap<DbEntity, ComponentRecord>(
                contractedReferentialDigraph.order());
        int componentIndex = 0;
        while (sorter.hasNext()) {
            Collection<DbEntity> component = sorter.next();
            ComponentRecord rec = new ComponentRecord(componentIndex++, component);

            for (DbEntity table : component) {
                components.put(table, rec);
            }
        }

        this.reflexiveDbEntities = reflexiveDbEntities;
        this.components = components;
    }

    /**
     * @since 3.1
     */
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
        this.dirty = true;
    }

    public void sortDbEntities(List<DbEntity> dbEntities, boolean deleteOrder) {
        indexSorter();
        Collections.sort(dbEntities, getDbEntityComparator(deleteOrder));
    }

    public void sortObjEntities(List<ObjEntity> objEntities, boolean deleteOrder) {
        indexSorter();
        Collections.sort(objEntities, getObjEntityComparator(deleteOrder));
    }

    public void sortObjectsForEntity(ObjEntity objEntity, List<?> objects, boolean deleteOrder) {

        indexSorter();

        List<Persistent> persistent = (List<Persistent>) objects;

        DbEntity dbEntity = objEntity.getDbEntity();

        // if no sorting is required
        if (!isReflexive(dbEntity)) {
            return;
        }

        int size = persistent.size();
        if (size == 0) {
            return;
        }

        EntityResolver resolver = persistent.get(0).getObjectContext().getEntityResolver();
        ClassDescriptor descriptor = resolver.getClassDescriptor(objEntity.getName());

        List<DbRelationship> reflexiveRels = reflexiveDbEntities.get(dbEntity);
        String[] reflexiveRelNames = new String[reflexiveRels.size()];
        for (int i = 0; i < reflexiveRelNames.length; i++) {
            DbRelationship dbRel = reflexiveRels.get(i);
            ObjRelationship objRel = (dbRel != null ? objEntity.getRelationshipForDbRelationship(dbRel) : null);
            reflexiveRelNames[i] = (objRel != null ? objRel.getName() : null);
        }

        List<Persistent> sorted = new ArrayList<Persistent>(size);

        Digraph<Persistent, Boolean> objectDependencyGraph = new MapDigraph<Persistent, Boolean>();
        Object[] masters = new Object[reflexiveRelNames.length];
        for (int i = 0; i < size; i++) {
            Persistent current = (Persistent) objects.get(i);
            objectDependencyGraph.addVertex(current);
            int actualMasterCount = 0;
            for (int k = 0; k < reflexiveRelNames.length; k++) {
                String reflexiveRelName = reflexiveRelNames[k];

                if (reflexiveRelName == null) {
                    continue;
                }

                masters[k] = descriptor.getProperty(reflexiveRelName).readProperty(current);

                if (masters[k] == null) {
                    masters[k] = findReflexiveMaster(current, objEntity.getRelationship(reflexiveRelName), current
                            .getObjectId().getEntityName());
                }

                if (masters[k] != null) {
                    actualMasterCount++;
                }
            }

            int mastersFound = 0;
            for (int j = 0; j < size && mastersFound < actualMasterCount; j++) {

                if (i == j) {
                    continue;
                }

                Persistent masterCandidate = persistent.get(j);
                for (Object master : masters) {
                    // if (masterCandidate.equals(master)) {
                    if (masterCandidate == master) {
                        objectDependencyGraph.putArc(masterCandidate, current, Boolean.TRUE);
                        mastersFound++;
                    }
                }
            }
        }

        IndegreeTopologicalSort<Persistent> sorter = new IndegreeTopologicalSort<Persistent>(objectDependencyGraph);

        while (sorter.hasNext()) {
            Persistent o = sorter.next();
            if (o == null)
                throw new CayenneRuntimeException("Sorting objects for " + objEntity.getClassName()
                        + " failed. Cycles found.");
            sorted.add(o);
        }

        // since API requires sorting within the same array,
        // simply replace all objects with objects in the right order...
        // may come up with something cleaner later
        persistent.clear();
        persistent.addAll(sorted);

        if (deleteOrder) {
            Collections.reverse(persistent);
        }
    }

    protected Object findReflexiveMaster(Persistent object, ObjRelationship toOneRel, String targetEntityName) {

        DbRelationship finalRel = toOneRel.getDbRelationships().get(0);
        ObjectContext context = object.getObjectContext();

        // find committed snapshot - so we can't fetch from the context as it
        // will return
        // dirty snapshot; must go down the stack instead

        // how do we handle this for NEW objects correctly? For now bail from
        // the method
        if (object.getObjectId().isTemporary()) {
            return null;
        }

        ObjectIdQuery query = new ObjectIdQuery(object.getObjectId(), true, ObjectIdQuery.CACHE);
        QueryResponse response = context.getChannel().onQuery(null, query);
        List<?> result = response.firstList();
        if (result == null || result.size() == 0) {
            return null;
        }

        DataRow snapshot = (DataRow) result.get(0);

        ObjectId id = snapshot.createTargetObjectId(targetEntityName, finalRel);

        // not using 'localObject', looking up in context instead, as within the
        // sorter
        // we only care about objects participating in transaction, so no need
        // to create
        // hollow objects
        return (id != null) ? context.getGraphManager().getNode(id) : null;
    }

    protected Comparator<DbEntity> getDbEntityComparator(boolean dependantFirst) {
        Comparator<DbEntity> c = dbEntityComparator;
        if (dependantFirst) {
            c = new ReverseComparator(c);
        }
        return c;
    }

    protected Comparator<ObjEntity> getObjEntityComparator(boolean dependantFirst) {
        Comparator<ObjEntity> c = objEntityComparator;
        if (dependantFirst) {
            c = new ReverseComparator(c);
        }
        return c;
    }

    protected boolean isReflexive(DbEntity metadata) {
        return reflexiveDbEntities.containsKey(metadata);
    }

    private final class ObjEntityComparator implements Comparator<ObjEntity> {

        public int compare(ObjEntity o1, ObjEntity o2) {
            if (o1 == o2)
                return 0;
            DbEntity t1 = o1.getDbEntity();
            DbEntity t2 = o2.getDbEntity();
            return dbEntityComparator.compare(t1, t2);
        }
    }

    private final class DbEntityComparator implements Comparator<DbEntity> {

        public int compare(DbEntity t1, DbEntity t2) {
            int result = 0;

            if (t1 == t2)
                return 0;
            if (t1 == null)
                result = -1;
            else if (t2 == null)
                result = 1;
            else {
                ComponentRecord rec1 = components.get(t1);
                ComponentRecord rec2 = components.get(t2);
                int index1 = rec1.index;
                int index2 = rec2.index;
                result = (index1 > index2 ? 1 : (index1 < index2 ? -1 : 0));
                if (result != 0 && rec1.component == rec2.component)
                    result = 0;
            }
            return result;
        }
    }

    private final static class ComponentRecord {

        ComponentRecord(int index, Collection<DbEntity> component) {
            this.index = index;
            this.component = component;
        }

        int index;
        Collection<DbEntity> component;
    }

}
