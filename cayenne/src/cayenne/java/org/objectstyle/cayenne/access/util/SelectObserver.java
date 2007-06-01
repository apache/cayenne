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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.ObjectStore;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.ToManyList;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

/** 
 * OperationObserver that accumulates select query results provided 
 * by callback methods. Later the results can be retrieved
 * via different <code>getResults</code> methods. Also supports instantiating
 * DataObjects within a provided DataContext.
 * 
 * <p>Thsi class is used as a default OperationObserver by DataContext.
 * Also it can serve as a helper for classes that work with 
 * DataNode directly, bypassing DataContext.
 * </p>
 * 
 * <p>If exceptions happen during the execution, they are immediately rethrown.
 * </p>
 * 
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 * 
 *  @author Andrei Adamchik
 */
public class SelectObserver extends DefaultOperationObserver {
    private static Logger logObj = Logger.getLogger(SelectObserver.class);

    protected Map results = new HashMap();
    protected int selectCount;

    public SelectObserver() {
        this(QueryLogger.DEFAULT_LOG_LEVEL);
    }

    public SelectObserver(Level logLevel) {
        super.setLoggingLevel(logLevel);
    }

    /** 
     * Returns a count of select queries that returned results
     * since the last time "clear" was called, or since this object
     * was created.
     */
    public int getSelectCount() {
        return selectCount;
    }

    /** 
     * Returns a list of result snapshots for the specified query,
     * or null if this query has never produced any results.
     */
    public List getResults(Query q) {
        return (List) results.get(q);
    }

    /** 
     * Returns query results accumulated during query execution with this
     * object as an operation observer. 
     */
    public Map getResults() {
        return results;
    }

    /** Clears fetched objects stored in an internal list. */
    public void clear() {
        selectCount = 0;
        results.clear();
    }

    /** 
     * Stores all objects in <code>dataRows</code> in an internal
     * result list. 
     */
    public void nextDataRows(Query query, List dataRows) {

        super.nextDataRows(query, dataRows);
        if (dataRows != null) {
            results.put(query, dataRows);
        }

        selectCount++;
    }

    /** 
      * Returns results for a given query object as DataObjects. <code>rootQuery</code> argument
      * is assumed to be the root query, and the rest are either independent queries or queries
      * prefetching relationships for the root query. 
      * 
      * <p>If no results are found, an empty immutable list is returned. Most common case for this
      * is when a delegate has blocked the query from execution.
      * </p>
      * 
      * <p>Side effect of this method call is that all data rows currently stored in this
      * SelectObserver are loaded as objects to a given DataContext (thus resolving
      * prefetched to-one relationships). Any to-many relationships for the root query
      * are resolved as well.</p>
      * 
      * @since 1.1
      */
    public List getResultsAsObjects(DataContext dataContext, Query rootQuery) {
        ObjEntity entity = dataContext.getEntityResolver().lookupObjEntity(rootQuery);
        
        // sanity check
        if (entity == null) {
            throw new CayenneRuntimeException(
                    "Can't instantiate DataObjects from resutls. ObjEntity is undefined for query: "
                            + rootQuery);
        }
        
        boolean refresh =
            (rootQuery instanceof GenericSelectQuery)
                ? ((GenericSelectQuery) rootQuery).isRefreshingObjects()
                : true;

        boolean resolveHierarchy =
            (rootQuery instanceof GenericSelectQuery)
                ? ((GenericSelectQuery) rootQuery).isResolvingInherited()
                : false;

        return new PrefetchTreeNode(entity, rootQuery).resolveObjectTree(
            dataContext,
            entity,
            refresh,
            resolveHierarchy);
    }

    /** 
     * Overrides super implementation to rethrow an exception immediately. 
     */
    public void nextQueryException(Query query, Exception ex) {
        super.nextQueryException(query, ex);
        throw new CayenneRuntimeException("Query exception.", Util.unwindException(ex));
    }

    /** 
     * Overrides superclass implementation to rethrow an exception
     * immediately. 
     */
    public void nextGlobalException(Exception ex) {
        super.nextGlobalException(ex);
        throw new CayenneRuntimeException("Global exception.", Util.unwindException(ex));
    }

    /**
     * Organizes a list of objects in a map keyed by the source related object for
     * the "incoming" relationship.
     * 
     * @since 1.1
     */
    static Map partitionBySource(ObjRelationship incoming, List prefetchedObjects) {
        Class sourceObjectClass =
            ((ObjEntity) incoming.getSourceEntity()).getJavaClass(
                Configuration.getResourceLoader());
        ObjRelationship reverseRelationship = incoming.getReverseRelationship();

        // Might be used later on... obtain and cast only once
        DbRelationship dbRelationship =
            (DbRelationship) incoming.getDbRelationships().get(0);

        Factory listFactory = new Factory() {
            public Object create() {
                return new ArrayList();
            }
        };

        Map toManyLists = MapUtils.lazyMap(new HashMap(), listFactory);
        Iterator destIterator = prefetchedObjects.iterator();
        while (destIterator.hasNext()) {
            DataObject destinationObject = (DataObject) destIterator.next();
            DataObject sourceObject = null;
            if (reverseRelationship != null) {
                sourceObject =
                    (DataObject) destinationObject.readProperty(
                        reverseRelationship.getName());
            }
            else {
                // Reverse relationship doesn't exist... match objects manually
                DataContext context = destinationObject.getDataContext();
                ObjectStore objectStore = context.getObjectStore();

                Map sourcePk =
                    dbRelationship.srcPkSnapshotWithTargetSnapshot(
                        objectStore.getSnapshot(
                            destinationObject.getObjectId(),
                            context));

                // if object does not exist yet, don't create it
                // the reason for its absense is likely due to the absent intermediate prefetch
                sourceObject =
                    objectStore.getObject(new ObjectId(sourceObjectClass, sourcePk));
            }

            // don't attach to hollow objects
            if (sourceObject != null
                && sourceObject.getPersistenceState() != PersistenceState.HOLLOW) {
                List relatedObjects = (List) toManyLists.get(sourceObject);
                relatedObjects.add(destinationObject);
            }
        }

        return toManyLists;
    }

    // ====================================================
    // Represents a tree of prefetch queries intended to
    // resolve prefetch relationships in the correct order
    // ====================================================
    final class PrefetchTreeNode {
        ObjRelationship incomingRelationship;
        List dataRows;
        List objects;
        Map children;

        // creates root node of prefetch tree
        PrefetchTreeNode(ObjEntity entity, Query rootQuery) {
            // add children
            Iterator it = results.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();

                Query query = (Query) entry.getKey();
                List dataRows = (List) entry.getValue();

                if (dataRows == null) {
                    logObj.warn("Can't find prefetch results for query: " + query);
                    continue;

                    // ignore null result (this shouldn't happen), however do not ignore
                    // empty result, since it should be used to
                    // update the source objects...
                }

                if (rootQuery == query) {
                    this.dataRows = dataRows;
                    continue;
                }

                // add prefetch queries to the tree
                if (query instanceof PrefetchSelectQuery) {
                    PrefetchSelectQuery prefetchQuery = (PrefetchSelectQuery) query;

                    if (prefetchQuery.getParentQuery() == rootQuery) {
                        addChildWithPath(
                            entity,
                            prefetchQuery.getPrefetchPath(),
                            dataRows);
                    }
                }
            }
        }

        PrefetchTreeNode(ObjRelationship incomingRelationship) {
            this.incomingRelationship = incomingRelationship;
        }

        // adds a possibly indirect child
        void addChildWithPath(ObjEntity rootEntity, String prefetchPath, List dataRows) {
            Iterator it = rootEntity.resolvePathComponents(prefetchPath);

            if (!it.hasNext()) {
                return;
            }

            PrefetchTreeNode lastChild = this;

            while (it.hasNext()) {
                ObjRelationship r = (ObjRelationship) it.next();
                lastChild = lastChild.addChild(r);
            }

            if (lastChild != null) {
                lastChild.dataRows = dataRows;
            }
        }

        // adds a direct child
        PrefetchTreeNode addChild(ObjRelationship outgoingRelationship) {
            PrefetchTreeNode child = null;

            if (children == null) {
                children = new LinkedMap();
            }
            else {
                child = (PrefetchTreeNode) children.get(outgoingRelationship.getName());
            }

            if (child == null) {
                child = new PrefetchTreeNode(outgoingRelationship);
                children.put(outgoingRelationship.getName(), child);
            }

            return child;
        }

        // method called on root to get its children 
        // and trigger all prefetch resolution
        List resolveObjectTree(
            DataContext dataContext,
            ObjEntity entity,
            boolean refresh,
            boolean resolveHierarchy) {

            // resolve objects
            this.objects =
                dataContext.objectsFromDataRows(
                    entity,
                    dataRows,
                    refresh,
                    resolveHierarchy);

            // resolve children
            if (children != null) {
                Iterator it = children.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    PrefetchTreeNode node = (PrefetchTreeNode) entry.getValue();
                    node.resolveObjectTree(
                        PrefetchTreeNode.this,
                        dataContext,
                        refresh,
                        resolveHierarchy);
                }
            }

            return this.objects;
        }

        // main processing method
        // resolves this node objects and all child prefetches
        void resolveObjectTree(
            PrefetchTreeNode parent,
            DataContext dataContext,
            boolean refresh,
            boolean resolveHierarchy) {

            // skip most operations on a "phantom" node that had no prefetch query
            if (dataRows != null) {
                // resolve objects;
                this.objects =
                    dataContext.objectsFromDataRows(
                        (ObjEntity) incomingRelationship.getTargetEntity(),
                        dataRows,
                        refresh,
                        resolveHierarchy);

                // connect to parent
                if (parent != null
                    && incomingRelationship != null
                    && incomingRelationship.isToMany()) {

                    Map partitioned =
                        partitionBySource(incomingRelationship, this.objects);

                    // depending on whether parent is a "phantom" node,
                    // use different strategy

                    if (parent.objects != null && parent.objects.size() > 0) {
                        connectToNodeParents(parent.objects, partitioned);
                    }
                    else {
                        connectToFaultedParents(partitioned);
                    }
                }
            }

            //  resolve children
            if (children != null) {
                Iterator it = children.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    PrefetchTreeNode node = (PrefetchTreeNode) entry.getValue();
                    node.resolveObjectTree(
                        PrefetchTreeNode.this,
                        dataContext,
                        refresh,
                        resolveHierarchy);
                }
            }
        }

        void connectToNodeParents(List parentObjects, Map partitioned) {

            // destinationObjects has now been partitioned into a list per
            // source object... Now init their "toMany"

            Iterator it = parentObjects.iterator();
            while (it.hasNext()) {
                DataObject root = (DataObject) it.next();
                List related = (List) partitioned.get(root);

                if (related == null) {
                    related = new ArrayList(1);
                }

                ToManyList toManyList =
                    (ToManyList) root.readProperty(incomingRelationship.getName());
                toManyList.setObjectList(related);
            }
        }

        void connectToFaultedParents(Map partitioned) {
            Iterator it = partitioned.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();

                DataObject root = (DataObject) entry.getKey();
                List related = (List) entry.getValue();

                ToManyList toManyList =
                    (ToManyList) root.readProperty(incomingRelationship.getName());

                // TODO: if a list is modified, should we
                // merge to-many instead of simply overwriting it?
                toManyList.setObjectList(related);
            }
        }
    }
}