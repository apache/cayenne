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
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Default mutable implementation of {@link QueryMetadata}.
 * 
 * @since 1.1
 */
class BaseQueryMetadata implements QueryMetadata, XMLSerializable, Serializable {

    int fetchLimit = QueryMetadata.FETCH_LIMIT_DEFAULT;
    int fetchOffset = QueryMetadata.FETCH_OFFSET_DEFAULT;

    int statementFetchSize = QueryMetadata.FETCH_OFFSET_DEFAULT;

    int pageSize = QueryMetadata.PAGE_SIZE_DEFAULT;
    boolean fetchingDataRows = QueryMetadata.FETCHING_DATA_ROWS_DEFAULT;
    QueryCacheStrategy cacheStrategy = QueryCacheStrategy.getDefaultStrategy();

    PrefetchTreeNode prefetchTree;
    String cacheKey;
    String[] cacheGroups;

    transient List<Object> resultSetMapping;
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
        this.cacheStrategy = info.getCacheStrategy();
        this.cacheKey = info.getCacheKey();
        this.cacheGroups = info.getCacheGroups();
        this.resultSetMapping = info.getResultSetMapping();

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
                if (root instanceof Class<?>) {
                    entity = resolver.getObjEntity((Class<?>) root);
                    if (entity == null) { // entity not found, try to resolve it
                                          // with
                        // client resolver
                        EntityResolver clientResolver = resolver.getClientEntityResolver();
                        if (clientResolver != resolver) {
                            ObjEntity clientEntity = clientResolver.getObjEntity((Class<?>) root);

                            if (clientEntity != null) {
                                entity = resolver.getObjEntity(clientEntity.getName());
                            }
                        }
                    }

                    if (entity != null) {
                        this.dbEntity = entity.getDbEntity();
                        this.dataMap = entity.getDataMap();
                    }
                } else if (root instanceof ObjEntity) {
                    entity = (ObjEntity) root;
                    this.dbEntity = entity.getDbEntity();
                    this.dataMap = entity.getDataMap();
                } else if (root instanceof String) {
                    entity = resolver.getObjEntity((String) root);
                    if (entity != null) {
                        this.dbEntity = entity.getDbEntity();
                        this.dataMap = entity.getDataMap();
                    }
                } else if (root instanceof DbEntity) {
                    this.dbEntity = (DbEntity) root;
                    this.dataMap = dbEntity.getDataMap();
                } else if (root instanceof DataMap) {
                    this.dataMap = (DataMap) root;
                } else if (root instanceof Persistent) {
                    entity = resolver.getObjEntity((Persistent) root);
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

        Object fetchOffset = properties.get(QueryMetadata.FETCH_OFFSET_PROPERTY);
        Object fetchLimit = properties.get(QueryMetadata.FETCH_LIMIT_PROPERTY);
        Object pageSize = properties.get(QueryMetadata.PAGE_SIZE_PROPERTY);
        Object statementFetchSize = properties.get(QueryMetadata.STATEMENT_FETCH_SIZE_PROPERTY);
        Object fetchingDataRows = properties.get(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY);

        Object cacheStrategy = properties.get(QueryMetadata.CACHE_STRATEGY_PROPERTY);

        Object cacheGroups = properties.get(QueryMetadata.CACHE_GROUPS_PROPERTY);

        // init ivars from properties
        this.fetchOffset = (fetchOffset != null) ? Integer.parseInt(fetchOffset.toString())
                : QueryMetadata.FETCH_OFFSET_DEFAULT;

        this.fetchLimit = (fetchLimit != null) ? Integer.parseInt(fetchLimit.toString())
                : QueryMetadata.FETCH_LIMIT_DEFAULT;

        this.pageSize = (pageSize != null) ? Integer.parseInt(pageSize.toString()) : QueryMetadata.PAGE_SIZE_DEFAULT;

        this.statementFetchSize = (statementFetchSize != null) ? Integer.parseInt(statementFetchSize.toString())
                : QueryMetadata.STATEMENT_FETCH_SIZE_DEFAULT;

        this.fetchingDataRows = (fetchingDataRows != null) ? "true".equalsIgnoreCase(fetchingDataRows.toString())
                : QueryMetadata.FETCHING_DATA_ROWS_DEFAULT;

        this.cacheStrategy = (cacheStrategy != null) ? QueryCacheStrategy.safeValueOf(cacheStrategy.toString())
                : QueryCacheStrategy.getDefaultStrategy();

        this.cacheGroups = null;
        if (cacheGroups instanceof String[]) {
            this.cacheGroups = (String[]) cacheGroups;
        } else if (cacheGroups instanceof String) {
            StringTokenizer toks = new StringTokenizer(cacheGroups.toString(), ",");
            this.cacheGroups = new String[toks.countTokens()];
            for (int i = 0; i < this.cacheGroups.length; i++) {
                this.cacheGroups[i] = toks.nextToken();
            }
        }
    }

    public void encodeAsXML(XMLEncoder encoder) {

        if (fetchingDataRows != QueryMetadata.FETCHING_DATA_ROWS_DEFAULT) {
            encoder.printProperty(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY, fetchingDataRows);
        }

        if (fetchOffset != QueryMetadata.FETCH_OFFSET_DEFAULT) {
            encoder.printProperty(QueryMetadata.FETCH_OFFSET_PROPERTY, fetchOffset);
        }

        if (fetchLimit != QueryMetadata.FETCH_LIMIT_DEFAULT) {
            encoder.printProperty(QueryMetadata.FETCH_LIMIT_PROPERTY, fetchLimit);
        }

        if (pageSize != QueryMetadata.PAGE_SIZE_DEFAULT) {
            encoder.printProperty(QueryMetadata.PAGE_SIZE_PROPERTY, pageSize);
        }

        if (cacheStrategy != null && QueryCacheStrategy.getDefaultStrategy() != cacheStrategy) {
            encoder.printProperty(QueryMetadata.CACHE_STRATEGY_PROPERTY, cacheStrategy.name());
        }

        if (statementFetchSize != QueryMetadata.STATEMENT_FETCH_SIZE_DEFAULT) {
            encoder.printProperty(QueryMetadata.STATEMENT_FETCH_SIZE_PROPERTY, statementFetchSize);
        }

        if (prefetchTree != null) {
            prefetchTree.encodeAsXML(encoder);
        }

        if (cacheGroups != null && cacheGroups.length > 0) {
            StringBuilder buffer = new StringBuilder(cacheGroups[0]);
            for (int i = 1; i < cacheGroups.length; i++) {
                buffer.append(',').append(cacheGroups[i]);
            }
            encoder.printProperty(QueryMetadata.CACHE_GROUPS_PROPERTY, buffer.toString());
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
     * @since 3.0
     */
    public Map<String, String> getPathSplitAliases() {
        return Collections.emptyMap();
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
     * @since 3.0
     */
    public List<Object> getResultSetMapping() {
        return resultSetMapping;
    }

    /**
     * @since 1.2
     */
    public PrefetchTreeNode getPrefetchTree() {
        return prefetchTree;
    }

    void setPrefetchTree(PrefetchTreeNode prefetchTree) {
        this.prefetchTree = prefetchTree != null ? deepClone(prefetchTree, null) : null;
    }

    private PrefetchTreeNode deepClone(PrefetchTreeNode source, PrefetchTreeNode targetParent) {

        PrefetchTreeNode target = new PrefetchTreeNode(targetParent, source.getName());
        target.setEjbqlPathEntityId(source.getEjbqlPathEntityId());
        target.setEntityName(source.getEntityName());
        target.setPhantom(source.isPhantom());
        target.setSemantics(source.getSemantics());

        for (PrefetchTreeNode child : source.getChildren()) {
            target.addChild(deepClone(child, target));
        }

        return target;
    }

    /**
     * @since 3.0
     */
    public QueryCacheStrategy getCacheStrategy() {
        return cacheStrategy;
    }

    /**
     * @since 3.0
     */
    void setCacheStrategy(QueryCacheStrategy cacheStrategy) {
        this.cacheStrategy = cacheStrategy;
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
    void setCacheGroups(String... groups) {
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

    public Query getOrginatingQuery() {
        return null;
    }

    /**
     * @since 3.0
     */
    public int getFetchOffset() {
        return fetchOffset;
    }

    public boolean isRefreshingObjects() {
        return true;
    }

    void setFetchingDataRows(boolean b) {
        fetchingDataRows = b;
    }

    void setFetchLimit(int i) {
        fetchLimit = i;
    }

    void setFetchOffset(int i) {
        fetchOffset = i;
    }

    void setPageSize(int i) {
        pageSize = i;
    }

    /**
     * Sets statement's fetch size (0 for no default size)
     * 
     * @since 3.0
     */
    void setStatementFetchSize(int size) {
        this.statementFetchSize = size;
    }

    /**
     * @return statement's fetch size
     * @since 3.0
     */
    public int getStatementFetchSize() {
        return statementFetchSize;
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
