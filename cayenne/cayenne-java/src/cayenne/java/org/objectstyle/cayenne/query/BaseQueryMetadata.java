/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.query;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;
import org.objectstyle.cayenne.util.XMLSerializable;

/**
 * Default mutable implementation of {@link QueryMetadata}.
 * 
 * @author Andrus Adamchik
 * @since 1.1
 */
class BaseQueryMetadata implements QueryMetadata, XMLSerializable, Serializable {

    int fetchLimit = QueryMetadata.FETCH_LIMIT_DEFAULT;
    int pageSize = QueryMetadata.PAGE_SIZE_DEFAULT;
    boolean fetchingDataRows = QueryMetadata.FETCHING_DATA_ROWS_DEFAULT;
    boolean refreshingObjects = QueryMetadata.REFRESHING_OBJECTS_DEFAULT;
    boolean resolvingInherited = QueryMetadata.RESOLVING_INHERITED_DEFAULT;
    String cachePolicy = QueryMetadata.CACHE_POLICY_DEFAULT;

    PrefetchTreeNode prefetchTree;
    String cacheKey;

    transient ObjEntity objEntity;
    transient DbEntity dbEntity;
    transient DataMap dataMap;
    transient Object lastRoot;
    transient EntityResolver lastEntityResolver;
    

    /**
     * Copies values of this object to another SelectInfo object.
     */
    void copyFromInfo(QueryMetadata info) {
        this.lastEntityResolver = null;
        this.lastRoot = null;
        this.objEntity = null;
        this.dbEntity = null;
        this.dataMap = null;

        this.fetchingDataRows = info.isFetchingDataRows();
        this.fetchLimit = info.getFetchLimit();
        this.pageSize = info.getPageSize();
        this.refreshingObjects = info.isRefreshingObjects();
        this.resolvingInherited = info.isResolvingInherited();
        this.cachePolicy = info.getCachePolicy();
        this.cacheKey = info.getCacheKey();

        setPrefetchTree(info.getPrefetchTree());
    }

    boolean resolve(Object root, EntityResolver resolver, String cacheKey) {

        if (lastRoot != root || lastEntityResolver != resolver) {

            this.cacheKey = cacheKey;

            this.objEntity = null;
            this.dbEntity = null;
            this.dataMap = null;

            if (root != null) {
                if (root instanceof Class) {
                    this.objEntity = resolver.lookupObjEntity((Class) root);

                    if (this.objEntity != null) {
                        this.dbEntity = objEntity.getDbEntity();
                        this.dataMap = objEntity.getDataMap();
                    }
                }
                else if (root instanceof ObjEntity) {
                    this.objEntity = (ObjEntity) root;
                    this.dbEntity = objEntity.getDbEntity();
                    this.dataMap = objEntity.getDataMap();
                }
                else if (root instanceof String) {
                    this.objEntity = resolver.lookupObjEntity((String) root);
                    if (this.objEntity != null) {
                        this.dbEntity = objEntity.getDbEntity();
                        this.dataMap = objEntity.getDataMap();
                    }
                }
                else if (root instanceof DbEntity) {
                    this.dbEntity = (DbEntity) root;
                    this.dataMap = dbEntity.getDataMap();
                }
                else if (root instanceof DataMap) {
                    this.dataMap = (DataMap) root;
                }
                else if (root instanceof Persistent) {
                    this.objEntity = resolver.lookupObjEntity((Persistent) root);
                    if (this.objEntity != null) {
                        this.dbEntity = objEntity.getDbEntity();
                        this.dataMap = objEntity.getDataMap();
                    }
                }
            }

            this.lastRoot = root;
            this.lastEntityResolver = resolver;
            return true;
        }

        return false;
    }

    void initWithProperties(Map properties) {
        // must init defaults even if properties are empty
        if (properties == null) {
            properties = Collections.EMPTY_MAP;
        }

        Object fetchLimit = properties.get(QueryMetadata.FETCH_LIMIT_PROPERTY);
        Object pageSize = properties.get(QueryMetadata.PAGE_SIZE_PROPERTY);
        Object refreshingObjects = properties
                .get(QueryMetadata.REFRESHING_OBJECTS_PROPERTY);
        Object fetchingDataRows = properties
                .get(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY);

        Object resolvingInherited = properties
                .get(QueryMetadata.RESOLVING_INHERITED_PROPERTY);

        Object cachePolicy = properties.get(QueryMetadata.CACHE_POLICY_PROPERTY);

        // init ivars from properties
        this.fetchLimit = (fetchLimit != null)
                ? Integer.parseInt(fetchLimit.toString())
                : QueryMetadata.FETCH_LIMIT_DEFAULT;

        this.pageSize = (pageSize != null)
                ? Integer.parseInt(pageSize.toString())
                : QueryMetadata.PAGE_SIZE_DEFAULT;

        this.refreshingObjects = (refreshingObjects != null)
                ? "true".equalsIgnoreCase(refreshingObjects.toString())
                : QueryMetadata.REFRESHING_OBJECTS_DEFAULT;

        this.fetchingDataRows = (fetchingDataRows != null)
                ? "true".equalsIgnoreCase(fetchingDataRows.toString())
                : QueryMetadata.FETCHING_DATA_ROWS_DEFAULT;

        this.resolvingInherited = (resolvingInherited != null)
                ? "true".equalsIgnoreCase(resolvingInherited.toString())
                : QueryMetadata.RESOLVING_INHERITED_DEFAULT;

        this.cachePolicy = (cachePolicy != null)
                ? cachePolicy.toString()
                : QueryMetadata.CACHE_POLICY_DEFAULT;
    }

    public void encodeAsXML(XMLEncoder encoder) {
        if (refreshingObjects != QueryMetadata.REFRESHING_OBJECTS_DEFAULT) {
            encoder.printProperty(
                    QueryMetadata.REFRESHING_OBJECTS_PROPERTY,
                    refreshingObjects);
        }

        if (fetchingDataRows != QueryMetadata.FETCHING_DATA_ROWS_DEFAULT) {
            encoder.printProperty(
                    QueryMetadata.FETCHING_DATA_ROWS_PROPERTY,
                    fetchingDataRows);
        }

        if (resolvingInherited != QueryMetadata.RESOLVING_INHERITED_DEFAULT) {
            encoder.printProperty(
                    QueryMetadata.RESOLVING_INHERITED_PROPERTY,
                    resolvingInherited);
        }

        if (fetchLimit != QueryMetadata.FETCH_LIMIT_DEFAULT) {
            encoder.printProperty(QueryMetadata.FETCH_LIMIT_PROPERTY, fetchLimit);
        }

        if (pageSize != QueryMetadata.PAGE_SIZE_DEFAULT) {
            encoder.printProperty(QueryMetadata.PAGE_SIZE_PROPERTY, pageSize);
        }

        if (cachePolicy != null
                && !QueryMetadata.CACHE_POLICY_DEFAULT.equals(cachePolicy)) {
            encoder.printProperty(QueryMetadata.CACHE_POLICY_PROPERTY, cachePolicy);
        }

        if (prefetchTree != null) {
            prefetchTree.encodeAsXML(encoder);
        }
    }

    /**
     * @since 1.2
     */
    public String getCacheKey() {
        return cacheKey;
    }

    /**
     * @since 1.2
     */
    public DataMap getDataMap() {
        return dataMap;
    }

    /**
     * @since 1.2
     */
    public Procedure getProcedure() {
        return null;
    }

    /**
     * @since 1.2
     */
    public DbEntity getDbEntity() {
        return dbEntity;
    }

    /**
     * @since 1.2
     */
    public ObjEntity getObjEntity() {
        return objEntity;
    }

    /**
     * @since 1.2
     */
    public PrefetchTreeNode getPrefetchTree() {
        return prefetchTree;
    }

    void setPrefetchTree(PrefetchTreeNode prefetchTree) {
        if (prefetchTree != null) {
            // importnat: make a clone to allow modification independent from the
            // caller...
            try {
                prefetchTree = (PrefetchTreeNode) Util
                        .cloneViaSerialization(prefetchTree);
            }
            catch (CayenneRuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CayenneRuntimeException("Error cloning prefetch tree", e);
            }
        }

        this.prefetchTree = prefetchTree;
    }

    public String getCachePolicy() {
        return cachePolicy;
    }

    void setCachePolicy(String policy) {
        this.cachePolicy = policy;
    }

    public boolean isFetchingDataRows() {
        return fetchingDataRows;
    }

    public int getFetchLimit() {
        return fetchLimit;
    }

    public int getPageSize() {
        return pageSize;
    }

    /**
     * Always returns -1.
     */
    public int getFetchStartIndex() {
        return -1;
    }

    public boolean isRefreshingObjects() {
        return refreshingObjects;
    }

    public boolean isResolvingInherited() {
        return resolvingInherited;
    }

    void setFetchingDataRows(boolean b) {
        fetchingDataRows = b;
    }

    void setFetchLimit(int i) {
        fetchLimit = i;
    }

    void setPageSize(int i) {
        pageSize = i;
    }

    void setRefreshingObjects(boolean b) {
        refreshingObjects = b;
    }

    void setResolvingInherited(boolean b) {
        resolvingInherited = b;
    }

    /**
     * Adds a joint prefetch.
     * 
     * @since 1.2
     */
    PrefetchTreeNode addPrefetch(String path, int semantics) {
        if (prefetchTree == null) {
            prefetchTree = new PrefetchTreeNode();
        }

        PrefetchTreeNode node = prefetchTree.addPath(path);
        node.setSemantics(semantics);
        node.setPhantom(false);
        return node;
    }

    /**
     * Adds all prefetches from a provided collection.
     * 
     * @since 1.2
     */
    void addPrefetches(Collection prefetches, int semantics) {

        if (prefetches != null && !prefetches.isEmpty()) {

            Iterator it = prefetches.iterator();
            while (it.hasNext()) {
                String prefetch = (String) it.next();
                addPrefetch(prefetch, semantics);
            }
        }
    }

    /**
     * Clears all joint prefetches.
     * 
     * @since 1.2
     */
    void clearPrefetches() {
        prefetchTree = null;
    }

    /**
     * Removes joint prefetch.
     * 
     * @since 1.2
     */
    void removePrefetch(String prefetch) {
        if (prefetchTree != null) {
            prefetchTree.removePath(prefetch);
        }
    }
}