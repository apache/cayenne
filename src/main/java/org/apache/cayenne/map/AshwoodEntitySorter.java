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

package org.apache.cayenne.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.commons.collections.comparators.ReverseComparator;
import org.objectstyle.ashwood.dbutil.DbUtils;
import org.objectstyle.ashwood.dbutil.ForeignKey;
import org.objectstyle.ashwood.dbutil.Table;
import org.objectstyle.ashwood.graph.CollectionFactory;
import org.objectstyle.ashwood.graph.Digraph;
import org.objectstyle.ashwood.graph.IndegreeTopologicalSort;
import org.objectstyle.ashwood.graph.MapDigraph;
import org.objectstyle.ashwood.graph.StrongConnection;

/**
 * Implements dependency sorting algorithms for ObjEntities, DbEntities and DataObjects.
 * Presently it works for acyclic database schemas with possible multi-reflexive tables.
 * The class uses topological sorting from the <a
 * href="http://objectstyle.org/ashwood/">Ashwood library</a>.
 * 
 * @author Andriy Shapochka, Andrus Adamchik
 * @since 1.1
 */
public class AshwoodEntitySorter implements EntitySorter {

    protected Collection dataMaps;
    protected Map dbEntityToTableMap;
    protected Digraph referentialDigraph;
    protected Digraph contractedReferentialDigraph;
    protected Map components;
    protected Map reflexiveDbEntities;

    protected TableComparator tableComparator;
    protected DbEntityComparator dbEntityComparator;
    protected ObjEntityComparator objEntityComparator;

    // used for lazy initialization
    protected boolean dirty;

    public AshwoodEntitySorter(Collection dataMaps) {
        tableComparator = new TableComparator();
        dbEntityComparator = new DbEntityComparator();
        objEntityComparator = new ObjEntityComparator();

        setDataMaps(dataMaps);
    }

    /**
     * Reindexes internal sorter.
     */
    protected synchronized void _indexSorter() {
        if (!dirty) {
            return;
        }

        Collection tables = new ArrayList(64);
        dbEntityToTableMap = new HashMap(64);
        reflexiveDbEntities = new HashMap(32);
        for (Iterator i = dataMaps.iterator(); i.hasNext();) {
            DataMap map = (DataMap) i.next();
            Iterator entitiesToConvert = map.getDbEntities().iterator();
            while (entitiesToConvert.hasNext()) {
                DbEntity entity = (DbEntity) entitiesToConvert.next();
                Table table = new Table(entity.getCatalog(), entity.getSchema(), entity
                        .getName());
                fillInMetadata(table, entity);
                dbEntityToTableMap.put(entity, table);
                tables.add(table);
            }
        }
        referentialDigraph = new MapDigraph(MapDigraph.HASHMAP_FACTORY);
        DbUtils.buildReferentialDigraph(referentialDigraph, tables);
        StrongConnection contractor = new StrongConnection(
                referentialDigraph,
                CollectionFactory.ARRAYLIST_FACTORY);
        contractedReferentialDigraph = new MapDigraph(MapDigraph.HASHMAP_FACTORY);
        contractor.contract(
                contractedReferentialDigraph,
                CollectionFactory.ARRAYLIST_FACTORY);
        IndegreeTopologicalSort sorter = new IndegreeTopologicalSort(
                contractedReferentialDigraph);
        components = new HashMap(contractedReferentialDigraph.order());
        int componentIndex = 0;
        while (sorter.hasNext()) {
            Collection component = (Collection) sorter.next();
            ComponentRecord rec = new ComponentRecord(componentIndex++, component);
            for (Iterator i = component.iterator(); i.hasNext();) {
                components.put(i.next(), rec);
            }
        }

        // clear dirty flag
        this.dirty = false;
    }

    /**
     * @since 1.1
     */
    public synchronized void setDataMaps(Collection dataMaps) {
        this.dirty = true;
        this.dataMaps = dataMaps != null ? dataMaps : Collections.EMPTY_LIST;
    }

    public void sortDbEntities(List dbEntities, boolean deleteOrder) {
        _indexSorter();
        Collections.sort(dbEntities, getDbEntityComparator(deleteOrder));
    }

    public void sortObjEntities(List objEntities, boolean deleteOrder) {
        _indexSorter();
        Collections.sort(objEntities, getObjEntityComparator(deleteOrder));
    }

    public void sortObjectsForEntity(
            ObjEntity objEntity,
            List objects,
            boolean deleteOrder) {

        // don't forget to index the sorter
        _indexSorter();

        DbEntity dbEntity = objEntity.getDbEntity();

        // if no sorting is required
        if (!isReflexive(dbEntity)) {
            return;
        }

        int size = objects.size();
        if (size == 0) {
            return;
        }

        EntityResolver resolver = ((Persistent) objects.get(0))
                .getObjectContext()
                .getEntityResolver();
        ClassDescriptor descriptor = resolver.getClassDescriptor(objEntity.getName());

        List reflexiveRels = (List) reflexiveDbEntities.get(dbEntity);
        String[] reflexiveRelNames = new String[reflexiveRels.size()];
        for (int i = 0; i < reflexiveRelNames.length; i++) {
            DbRelationship dbRel = (DbRelationship) reflexiveRels.get(i);
            ObjRelationship objRel = (dbRel != null ? objEntity
                    .getRelationshipForDbRelationship(dbRel) : null);
            reflexiveRelNames[i] = (objRel != null ? objRel.getName() : null);
        }

        List sorted = new ArrayList(size);

        Digraph objectDependencyGraph = new MapDigraph(MapDigraph.HASHMAP_FACTORY);
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

                masters[k] = (reflexiveRelName != null) ? descriptor.getProperty(
                        reflexiveRelName).readProperty(current) : null;

                if (masters[k] == null) {
                    masters[k] = findReflexiveMaster(current, (ObjRelationship) objEntity
                            .getRelationship(reflexiveRelName), current
                            .getObjectId()
                            .getEntityName());
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

                Object masterCandidate = objects.get(j);
                for (int k = 0; k < masters.length; k++) {
                    if (masterCandidate.equals(masters[k])) {
                        objectDependencyGraph.putArc(
                                masterCandidate,
                                current,
                                Boolean.TRUE);
                        mastersFound++;
                    }
                }
            }
        }

        IndegreeTopologicalSort sorter = new IndegreeTopologicalSort(
                objectDependencyGraph);

        while (sorter.hasNext()) {
            Object o = sorter.next();
            if (o == null)
                throw new CayenneRuntimeException("Sorting objects for "
                        + objEntity.getClassName()
                        + " failed. Cycles found.");
            sorted.add(o);
        }

        // since API requires sorting within the same array,
        // simply replace all objects with objects in the right order...
        // may come up with something cleaner later
        objects.clear();
        objects.addAll(sorted);

        if (deleteOrder) {
            Collections.reverse(objects);
        }
    }

    protected void fillInMetadata(Table table, DbEntity entity) {
        // in this case quite a dummy
        short keySequence = 1;
        Iterator i = entity.getRelationshipMap().values().iterator();

        while (i.hasNext()) {
            DbRelationship candidate = (DbRelationship) i.next();
            if ((!candidate.isToMany() && !candidate.isToDependentPK())
                    || candidate.isToMasterPK()) {
                DbEntity target = (DbEntity) candidate.getTargetEntity();
                boolean newReflexive = entity.equals(target);
                Iterator j = candidate.getJoins().iterator();
                while (j.hasNext()) {
                    DbJoin join = (DbJoin) j.next();
                    DbAttribute targetAttribute = join.getTarget();
                    if (targetAttribute.isPrimaryKey()) {
                        ForeignKey fk = new ForeignKey();
                        fk.setPkTableCatalog(target.getCatalog());
                        fk.setPkTableSchema(target.getSchema());
                        fk.setPkTableName(target.getName());
                        fk.setPkColumnName(targetAttribute.getName());
                        fk.setColumnName(join.getSourceName());
                        fk.setKeySequence(keySequence++);
                        table.addForeignKey(fk);

                        if (newReflexive) {
                            List reflexiveRels = (List) reflexiveDbEntities.get(entity);
                            if (reflexiveRels == null) {
                                reflexiveRels = new ArrayList(1);
                                reflexiveDbEntities.put(entity, reflexiveRels);
                            }
                            reflexiveRels.add(candidate);
                            newReflexive = false;
                        }
                    }
                }
            }
        }
    }

    protected Object findReflexiveMaster(
            Persistent object,
            ObjRelationship toOneRel,
            String targetEntityName) {

        DbRelationship finalRel = (DbRelationship) toOneRel.getDbRelationships().get(0);
        ObjectContext context = object.getObjectContext();

        // find snapshot
        ObjectIdQuery query = new ObjectIdQuery(
                object.getObjectId(),
                true,
                ObjectIdQuery.CACHE);
        DataRow snapshot = (DataRow) DataObjectUtils.objectForQuery(context, query);

        ObjectId id = snapshot.createTargetObjectId(targetEntityName, finalRel);
        return (id != null) ? context.localObject(id, null) : null;
    }

    protected Comparator getDbEntityComparator(boolean dependantFirst) {
        Comparator c = dbEntityComparator;
        if (dependantFirst) {
            c = new ReverseComparator(c);
        }
        return c;
    }

    protected Comparator getObjEntityComparator(boolean dependantFirst) {
        Comparator c = objEntityComparator;
        if (dependantFirst) {
            c = new ReverseComparator(c);
        }
        return c;
    }

    protected Table getTable(DbEntity dbEntity) {
        return (dbEntity != null) ? (Table) dbEntityToTableMap.get(dbEntity) : null;
    }

    protected Table getTable(ObjEntity objEntity) {
        return getTable(objEntity.getDbEntity());
    }

    protected boolean isReflexive(DbEntity metadata) {
        return reflexiveDbEntities.containsKey(metadata);
    }

    private final class DbEntityComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            if (o1 == o2)
                return 0;
            Table t1 = getTable((DbEntity) o1);
            Table t2 = getTable((DbEntity) o2);
            return tableComparator.compare(t1, t2);
        }
    }

    private final class ObjEntityComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            if (o1 == o2)
                return 0;
            Table t1 = getTable((ObjEntity) o1);
            Table t2 = getTable((ObjEntity) o2);
            return tableComparator.compare(t1, t2);
        }
    }

    private final class TableComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            int result = 0;
            Table t1 = (Table) o1;
            Table t2 = (Table) o2;
            if (t1 == t2)
                return 0;
            if (t1 == null)
                result = -1;
            else if (t2 == null)
                result = 1;
            else {
                ComponentRecord rec1 = (ComponentRecord) components.get(t1);
                ComponentRecord rec2 = (ComponentRecord) components.get(t2);
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

        ComponentRecord(int index, Collection component) {
            this.index = index;
            this.component = component;
        }

        int index;
        Collection component;
    }

}
