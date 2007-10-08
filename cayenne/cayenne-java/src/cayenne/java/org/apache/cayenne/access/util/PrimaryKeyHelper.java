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


package org.apache.cayenne.access.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ComparatorUtils;
import org.objectstyle.ashwood.graph.CollectionFactory;
import org.objectstyle.ashwood.graph.Digraph;
import org.objectstyle.ashwood.graph.GraphUtils;
import org.objectstyle.ashwood.graph.IndegreeTopologicalSort;
import org.objectstyle.ashwood.graph.MapDigraph;
import org.objectstyle.ashwood.graph.StrongConnection;
import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.QueryEngine;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * PrimaryKeyHelper resolves primary key dependencies for entities related to the
 * supported query engine via topological sorting. It is directly based on ASHWOOD. In
 * addition it provides means for primary key generation relying on DbAdapter in this.
 * 
 * @author Andriy Shapochka
 * @deprecated since 1.2 replaced with a non-public utility class.
 */
public class PrimaryKeyHelper {

    private Map indexedDbEntities;
    private QueryEngine queryEngine;
    private DbEntityComparator dbEntityComparator;
    private ObjEntityComparator objEntityComparator;

    public PrimaryKeyHelper(QueryEngine queryEngine) {
        this.queryEngine = queryEngine;
        init();
        dbEntityComparator = new DbEntityComparator();
        objEntityComparator = new ObjEntityComparator();
    }

    public void reset() {
        init();
    }

    public Comparator getDbEntityComparator() {
        return dbEntityComparator;
    }

    public Comparator getObjEntityComparator() {
        return objEntityComparator;
    }

    public void createPermIdsForObjEntity(ObjEntity objEntity, List dataObjects)
            throws CayenneException {

        if (dataObjects.isEmpty()) {
            return;
        }

        DbEntity dbEntity = objEntity.getDbEntity();
        DataNode owner = queryEngine.lookupDataNode(objEntity.getDataMap());
        if (owner == null) {
            throw new CayenneRuntimeException(
                    "No suitable DataNode to handle primary key generation.");
        }

        PkGenerator pkGenerator = owner.getAdapter().getPkGenerator();
        boolean supportsGeneratedKeys = owner.getAdapter().supportsGeneratedKeys();
        List pkAttributes = dbEntity.getPrimaryKey();

        boolean pkFromMaster = true;
        Iterator i = dataObjects.iterator();
        while (i.hasNext()) {

            DataObject object = (DataObject) i.next();
            ObjectId id = object.getObjectId();
            if (id == null || !id.isTemporary()) {
                continue;
            }

            // modify replacement id directly...
            Map idMap = id.getReplacementIdMap();

            // first get values delivered via relationships
            if (pkFromMaster) {
                pkFromMaster = appendPkFromMasterRelationships(
                        idMap,
                        object,
                        objEntity,
                        dbEntity,
                        supportsGeneratedKeys);
            }

            boolean autoPkDone = false;
            Iterator it = pkAttributes.iterator();
            while (it.hasNext()) {
                DbAttribute dbAttr = (DbAttribute) it.next();
                String dbAttrName = dbAttr.getName();

                if (idMap.containsKey(dbAttrName)) {
                    continue;
                }

                // put a "null" for generated key, so that potential dependent objects
                // could record a change in their snapshot
                if (supportsGeneratedKeys && dbAttr.isGenerated()) {
                    idMap.put(dbAttrName, null);
                    continue;
                }

                ObjAttribute objAttr = objEntity.getAttributeForDbAttribute(dbAttr);
                if (objAttr != null) {
                    idMap.put(dbAttrName, object.readPropertyDirectly(objAttr.getName()));
                    continue;
                }

                // only a single key can be generated from DB... if this is done already
                // in this loop, we must bail out.
                if (autoPkDone) {
                    throw new CayenneException(
                            "Primary Key autogeneration only works for a single attribute.");
                }

                // finally, use database generation mechanism
                try {
                    Object pkValue = pkGenerator.generatePkForDbEntity(owner, dbEntity);
                    idMap.put(dbAttrName, pkValue);
                    autoPkDone = true;
                }
                catch (Exception ex) {
                    throw new CayenneException(
                            "Error generating PK: " + ex.getMessage(),
                            ex);
                }
            }
        }
    }

    private boolean appendPkFromMasterRelationships(
            Map idMap,
            DataObject dataObject,
            ObjEntity objEntity,
            DbEntity dbEntity,
            boolean supportsGeneratedKeys) throws CayenneException {

        boolean useful = false;
        Iterator it = dbEntity.getRelationships().iterator();
        while (it.hasNext()) {
            DbRelationship dbRel = (DbRelationship) it.next();
            if (!dbRel.isToMasterPK()) {
                continue;
            }

            ObjRelationship rel = objEntity.getRelationshipForDbRelationship(dbRel);
            if (rel == null) {
                continue;
            }

            DataObject targetDo = (DataObject) dataObject.readPropertyDirectly(rel
                    .getName());

            if (targetDo == null) {
                throw new CayenneException(
                        "Null master object, can't create primary key for: "
                                + dataObject.getClass()
                                + "."
                                + dbRel.getName());
            }

            ObjectId targetKey = targetDo.getObjectId();
            Map targetKeyMap = targetKey.getIdSnapshot();
            if (targetKeyMap == null) {
                throw new CayenneException(noMasterPkMsg(objEntity.getName(), targetKey
                        .getEntityName(), dbRel.getName()));
            }

            // DbRelationship logic currently throws an exception when some key is
            // missing... so have to implement a similar code here that is more
            // tolerant to the deferred keys.
            // TODO: maybe merge this back to DbRel?

            Iterator joins = dbRel.getJoins().iterator();
            while (joins.hasNext()) {
                DbJoin join = (DbJoin) joins.next();
                Object value = targetKeyMap.get(join.getTargetName());
                if (value == null) {
                    if (supportsGeneratedKeys && join.getTarget().isGenerated()) {
                        // ignore
                        continue;
                    }

                    throw new CayenneRuntimeException(
                            "Some parts of FK are missing in snapshot, join: " + join);
                }

                idMap.put(join.getSourceName(), value);
            }

            useful = true;
        }

        return useful;
    }

    private String noMasterPkMsg(String src, String dst, String rel) {
        StringBuffer msg = new StringBuffer(
                "Can't create primary key, master object has no PK snapshot.");
        msg.append("\nrelationship name: ").append(rel).append(", src object: ").append(
                src).append(", target obj: ").append(dst);
        return msg.toString();
    }

    private List collectAllDbEntities() {
        List entities = new ArrayList(32);
        for (Iterator i = queryEngine.getEntityResolver().getDataMaps().iterator(); i
                .hasNext();) {
            entities.addAll(((DataMap) i.next()).getDbEntities());
        }
        return entities;
    }

    private void init() {
        List dbEntitiesToResolve = collectAllDbEntities();
        Digraph pkDependencyGraph = new MapDigraph(MapDigraph.HASHMAP_FACTORY);
        indexedDbEntities = new HashMap(dbEntitiesToResolve.size());
        for (Iterator i = dbEntitiesToResolve.iterator(); i.hasNext();) {
            DbEntity origin = (DbEntity) i.next();
            for (Iterator j = origin.getRelationships().iterator(); j.hasNext();) {
                DbRelationship relation = (DbRelationship) j.next();
                if (relation.isToDependentPK()) {
                    DbEntity dst = (DbEntity) relation.getTargetEntity();
                    if (origin.equals(dst)) {
                        continue;
                    }
                    pkDependencyGraph.putArc(origin, dst, Boolean.TRUE);
                }
            }
        }
        int index = 0;
        for (Iterator i = dbEntitiesToResolve.iterator(); i.hasNext();) {
            DbEntity entity = (DbEntity) i.next();
            if (!pkDependencyGraph.containsVertex(entity)) {
                indexedDbEntities.put(entity, new Integer(index++));
            }
        }
        boolean acyclic = GraphUtils.isAcyclic(pkDependencyGraph);
        if (acyclic) {
            IndegreeTopologicalSort sorter = new IndegreeTopologicalSort(
                    pkDependencyGraph);
            while (sorter.hasNext())
                indexedDbEntities.put(sorter.next(), new Integer(index++));
        }
        else {
            StrongConnection contractor = new StrongConnection(
                    pkDependencyGraph,
                    CollectionFactory.ARRAYLIST_FACTORY);
            Digraph contractedDigraph = new MapDigraph(MapDigraph.HASHMAP_FACTORY);
            contractor.contract(contractedDigraph, CollectionFactory.ARRAYLIST_FACTORY);
            IndegreeTopologicalSort sorter = new IndegreeTopologicalSort(
                    contractedDigraph);
            while (sorter.hasNext()) {
                Collection component = (Collection) sorter.next();
                for (Iterator i = component.iterator(); i.hasNext();)
                    indexedDbEntities.put(i.next(), new Integer(index++));
            }
        }
    }

    private class DbEntityComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            if (o1.equals(o2)) {
                return 0;
            }
            Integer index1 = (Integer) indexedDbEntities.get(o1);
            Integer index2 = (Integer) indexedDbEntities.get(o2);
            return ComparatorUtils.NATURAL_COMPARATOR.compare(index1, index2);
        }
    }

    private class ObjEntityComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            if (o1.equals(o2)) {
                return 0;
            }
            DbEntity e1 = ((ObjEntity) o1).getDbEntity();
            DbEntity e2 = ((ObjEntity) o2).getDbEntity();
            return dbEntityComparator.compare(e1, e2);
        }
    }
}
