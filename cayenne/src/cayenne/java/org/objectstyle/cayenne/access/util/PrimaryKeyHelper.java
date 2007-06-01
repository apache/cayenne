/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.access.util;

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
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * PrimaryKeyHelper resolves primary key dependencies for entities
 * related to the supported query engine via topological sorting. It is
 * directly based on ASHWOOD. In addition it provides means for primary key
 * generation relying on DbAdapter in this.
 *
 * @author Andriy Shapochka
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
            throw new CayenneRuntimeException("No suitable DataNode to handle primary key generation.");
        }

        PkGenerator pkGenerator = owner.getAdapter().getPkGenerator();
        List pkAttributes = dbEntity.getPrimaryKey();

        HashMap idMap = null;
        boolean pkFromMaster = true;
        for (Iterator i = dataObjects.iterator(); i.hasNext();) {

            DataObject object = (DataObject) i.next();
            ObjectId id = object.getObjectId();
            if (!(id instanceof TempObjectId)) {
                continue;
                //If the id is not a temp, then it must be permanent.  Do nothing else
            }

            if (id.getReplacementId() != null) {
                continue;
                //An id already exists... nothing further required (definitely do not create another)
            }

            idMap = new HashMap(idMap != null ? idMap.size() : 1);

            // first get values delivered via relationships
            if (pkFromMaster)
                pkFromMaster =
                    appendPkFromMasterRelationships(idMap, object, objEntity, dbEntity);

            boolean autoPkDone = false;
            Iterator it = pkAttributes.iterator();
            while (it.hasNext()) {
                DbAttribute dbAttr = (DbAttribute) it.next();
                String dbAttrName = dbAttr.getName();
                if (idMap.containsKey(dbAttrName)) {
                    continue;
                }

                ObjAttribute objAttr = objEntity.getAttributeForDbAttribute(dbAttr);
                if (objAttr != null) {
                    idMap.put(dbAttrName, object.readPropertyDirectly(objAttr.getName()));
                    continue;
                }

                if (autoPkDone) {
                    throw new CayenneException("Primary Key autogeneration only works for a single attribute.");
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

            // create permanent ObjectId and attach it to the temporary id
            id.setReplacementId(new ObjectId(id.getObjClass(), idMap));
        }
    }

    private boolean appendPkFromMasterRelationships(
        Map map,
        DataObject dataObject,
        ObjEntity objEntity,
        DbEntity dbEntity)
        throws CayenneException {
        boolean useful = false;
        Iterator it = dbEntity.getRelationshipMap().values().iterator();
        while (it.hasNext()) {
            DbRelationship dbRel = (DbRelationship) it.next();
            if (!dbRel.isToMasterPK())
                continue;

            ObjRelationship rel = objEntity.getRelationshipForDbRelationship(dbRel);
            if (rel == null)
                continue;

            DataObject targetDo =
                (DataObject) dataObject.readPropertyDirectly(rel.getName());

            if (targetDo == null)
                throw new CayenneException(
                    "Null master object, can't create primary key for: "
                        + dataObject.getClass()
                        + "."
                        + dbRel.getName());

            ObjectId targetKey = targetDo.getObjectId();
            Map idMap = targetKey.getIdSnapshot();
            if (idMap == null)
                throw new CayenneException(
                    noMasterPkMsg(
                        objEntity.getName(),
                        targetKey.getObjClass().toString(),
                        dbRel.getName()));
            map.putAll(dbRel.srcFkSnapshotWithTargetSnapshot(idMap));
            useful = true;
        }
        return useful;
    }

    private String noMasterPkMsg(String src, String dst, String rel) {
        StringBuffer msg =
            new StringBuffer("Can't create primary key, master object has no PK snapshot.");
        msg
            .append("\nrelationship name: ")
            .append(rel)
            .append(", src object: ")
            .append(src)
            .append(", target obj: ")
            .append(dst);
        return msg.toString();
    }

    private List collectAllDbEntities() {
        List entities = new ArrayList(32);
        for (Iterator i = queryEngine.getDataMaps().iterator(); i.hasNext();) {
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
            IndegreeTopologicalSort sorter =
                new IndegreeTopologicalSort(pkDependencyGraph);
            while (sorter.hasNext())
                indexedDbEntities.put(sorter.next(), new Integer(index++));
        }
        else {
            StrongConnection contractor =
                new StrongConnection(
                    pkDependencyGraph,
                    CollectionFactory.ARRAYLIST_FACTORY);
            Digraph contractedDigraph = new MapDigraph(MapDigraph.HASHMAP_FACTORY);
            contractor.contract(contractedDigraph, CollectionFactory.ARRAYLIST_FACTORY);
            IndegreeTopologicalSort sorter =
                new IndegreeTopologicalSort(contractedDigraph);
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