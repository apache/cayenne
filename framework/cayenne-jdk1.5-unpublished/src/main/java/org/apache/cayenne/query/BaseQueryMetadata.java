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

package org.apache.cayenne.query;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

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
    String[] cacheGroups;

    transient DbEntity dbEntity;
    transient DataMap dataMap;
    transient Object lastRoot;
    transient ClassDescriptor classDescriptor;
    transient EntityResolver lastEntityResolver;

    /**
     * Copies values of another QueryMetadata object to this object.
     */
    void copyFromInfo(QueryMetadata info) {
        this.lastEntityResolver = null;
        this.lastRoot = null;
        this.classDescriptor = null;
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

            this.classDescriptor = null;
            this.dbEntity = null;
            this.dataMap = null;

            ObjEntity entity = null;

            if (root != null) {
                if (root instanceof Class) {
                    entity = resolver.lookupObjEntity((Class<?>) root);

                    if (entity != null) {
                        this.dbEntity = entity.getDbEntity();
                        this.dataMap = entity.getDataMap();
                    }
                }
                else if (root instanceof ObjEntity) {
                    entity = (ObjEntity) root;
                    this.dbEntity = entity.getDbEntity();
                    this.dataMap = entity.getDataMap();
                }
                else if (root instanceof String) {
                    entity = resolver.getObjEntity((String) root);
                    if (entity != null) {
                        this.dbEntity = entity.getDbEntity();
                        this.dataMap = entity.getDataMap();
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
                    entity = resolver.lookupObjEntity(root);
                    if (entity != null) {
                        this.dbEntity = entity.getDbEntity();
                        this.dataMap = entity.getDataMap();
                    }
                }
            }

            if (entity != null) {
                this.classDescriptor = resolver.getClassDescriptor(entity.getName());
            }

            this.lastRoot = root;
            this.lastEntityResolver = resolver;
            return true;
        }

        return false;
    }

    void initWithProperties(Map<String, ?> properties) {
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
        Object cacheGroups = properties.get(QueryMetadata.CACHE_GROUPS_PROPERTY);

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

        this.cacheGroups = null;
        if (cacheGroups instanceof String[]) {
            this.cacheGroups = (String[]) cacheGroups;
        }
        else if (cacheGroups instanceof String) {
            StringTokenizer toks = new StringTokenizer(cacheGroups.toString(), ",");
            this.cacheGroups = new String[toks.countTokens()];
            for (int i = 0; i < this.cacheGroups.length; i++) {
                this.cacheGroups[i] = toks.nextToken();
            }
        }
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

        if (cacheGroups != null && cacheGroups.length > 0) {
            StringBuilder buffer = new StringBuilder(cacheGroups[0]);
            for (int i = 1; i < cacheGroups.length; i++) {
                buffer.append(',').append(cacheGroups[i]);
            }
            encoder.printProperty(QueryMetadata.CACHE_GROUPS_PROPERTY, cachePolicy);
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
        return classDescriptor != null ? classDescriptor.getEntity() : null;
    }

    /**
     * @since 3.0
     */
    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }
    
    /**
     * Always returns null, as this is not supported for most classic queries.
     * 
     * @since 3.0
     */
    public SQLResultSetMapping getResultSetMapping() {
        return null;
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

    /**
     * @since 3.0
     */
    public String[] getCacheGroups() {
        return cacheGroups;
    }

    /**
     * @since 3.0
     */
    void setCacheGroups(String[] groups) {
        this.cacheGroups = groups;
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
    void addPrefetches(Collection<String> prefetches, int semantics) {
        if (prefetches != null) {
            for (String prefetch : prefetches) {
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
