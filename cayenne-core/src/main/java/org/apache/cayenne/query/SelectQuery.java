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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A query that selects persistent objects of a certain type or "raw data" (aka
 * DataRows). Supports expression qualifier, multiple orderings and a number of
 * other parameters that serve as runtime hints to Cayenne on how to optimize
 * the fetch and result processing.
 */
public class SelectQuery<T> extends QualifiedQuery implements ParameterizedQuery, XMLSerializable, Select<T> {

    public static final String DISTINCT_PROPERTY = "cayenne.SelectQuery.distinct";
    public static final boolean DISTINCT_DEFAULT = false;

    protected List<Ordering> orderings;
    protected boolean distinct;

    SelectQueryMetadata metaData = new SelectQueryMetadata();

    /**
     * Creates a SelectQuery that selects objects of a given persistent class.
     * 
     * @param rootClass
     *            the Class of objects fetched by this query.
     * 
     * @since 3.2
     */
    public static <T> SelectQuery<T> query(Class<T> rootClass) {
        return new SelectQuery<T>(rootClass);
    }

    /**
     * Creates a SelectQuery that selects objects of a given persistent class
     * that match supplied qualifier.
     * 
     * @param rootClass
     *            the Class of objects fetched by this query.
     * @param qualifier
     *            an Expression indicating which objects should be fetched.
     * 
     * @since 3.2
     */
    public static <T> SelectQuery<T> query(Class<T> rootClass, Expression qualifier) {
        return new SelectQuery<T>(rootClass, qualifier);
    }

    /**
     * Creates a SelectQuery that selects objects of a given persistent class
     * that match supplied qualifier.
     * 
     * @param rootClass
     *            the Class of objects fetched by this query.
     * @param qualifier
     *            an Expression indicating which objects should be fetched.
     * @param orderings
     *            defines how to order the results, may be null.
     * 
     * @since 3.2
     */
    public static <T> SelectQuery<T> query(Class<T> rootClass, Expression qualifier, List<? extends Ordering> orderings) {
        return new SelectQuery<T>(rootClass, qualifier, orderings);
    }

    /**
     * @since 3.2
     */
    public static SelectQuery<DataRow> dataRowQuery(Class<?> rootClass) {
        // create a query replica that would fetch DataRows
        SelectQuery<DataRow> query = new SelectQuery<DataRow>();

        query.setRoot(rootClass);
        query.metaData.setFetchingDataRows(true);

        return query;
    }

    /**
     * Creates a SelectQuery that selects DataRows that correspond to a given
     * persistent class that match supplied qualifier.
     * 
     * @param rootClass
     *            the Class of objects that correspond to DataRows entity.
     * @param qualifier
     *            an Expression indicating which objects should be fetched.
     * 
     * @since 3.2
     */
    public static SelectQuery<DataRow> dataRowQuery(Class<?> rootClass, Expression qualifier) {
        SelectQuery<DataRow> query = dataRowQuery(rootClass);
        query.setQualifier(qualifier);
        return query;
    }

    /**
     * @since 3.2
     */
    public static SelectQuery<DataRow> dataRowQuery(Class<?> rootClass, Expression qualifier, List<Ordering> orderings) {
        SelectQuery<DataRow> query = dataRowQuery(rootClass, qualifier);
        query.addOrderings(orderings);
        return query;
    }

    /** Creates an empty SelectQuery. */
    public SelectQuery() {
    }

    /**
     * Creates a SelectQuery with null qualifier, for the specifed ObjEntity
     * 
     * @param root
     *            the ObjEntity this SelectQuery is for.
     */
    public SelectQuery(ObjEntity root) {
        this(root, null);
    }

    /**
     * Creates a SelectQuery for the specified ObjEntity with the given
     * qualifier.
     * 
     * @param root
     *            the ObjEntity this SelectQuery is for.
     * @param qualifier
     *            an Expression indicating which objects should be fetched
     */
    public SelectQuery(ObjEntity root, Expression qualifier) {
        this(root, qualifier, null);
    }

    /**
     * Creates a SelectQuery for the specified ObjEntity with the given
     * qualifier and orderings.
     * 
     * @param root
     *            the ObjEntity this SelectQuery is for.
     * @param qualifier
     *            an Expression indicating which objects should be fetched.
     * @param orderings
     *            defines how to order the results, may be null.
     * @since 3.1
     */
    public SelectQuery(ObjEntity root, Expression qualifier, List<? extends Ordering> orderings) {
        this();
        this.init(root, qualifier);
        addOrderings(orderings);
    }

    /**
     * Creates a SelectQuery that selects all objects of a given persistent
     * class.
     * 
     * @param rootClass
     *            the Class of objects fetched by this query.
     */
    public SelectQuery(Class<T> rootClass) {
        this(rootClass, null);
    }

    /**
     * Creates a SelectQuery that selects objects of a given persistent class
     * that match supplied qualifier.
     * 
     * @param rootClass
     *            the Class of objects fetched by this query.
     * @param qualifier
     *            an Expression indicating which objects should be fetched.
     */
    public SelectQuery(Class<T> rootClass, Expression qualifier) {
        this(rootClass, qualifier, null);
    }

    /**
     * Creates a SelectQuery that selects objects of a given persistent class
     * that match supplied qualifier.
     * 
     * @param rootClass
     *            the Class of objects fetched by this query.
     * @param qualifier
     *            an Expression indicating which objects should be fetched.
     * @param orderings
     *            defines how to order the results, may be null.
     * @since 3.1
     */
    public SelectQuery(Class<T> rootClass, Expression qualifier, List<? extends Ordering> orderings) {
        init(rootClass, qualifier);
        addOrderings(orderings);
    }

    /**
     * Creates a SelectQuery for the specified DbEntity.
     * 
     * @param root
     *            the DbEntity this SelectQuery is for.
     * @since 1.1
     */
    public SelectQuery(DbEntity root) {
        this(root, null);
    }

    /**
     * Creates a SelectQuery for the specified DbEntity with the given
     * qualifier.
     * 
     * @param root
     *            the DbEntity this SelectQuery is for.
     * @param qualifier
     *            an Expression indicating which objects should be fetched.
     * @since 1.1
     */
    public SelectQuery(DbEntity root, Expression qualifier) {
        this(root, qualifier, null);
    }

    /**
     * Creates a SelectQuery for the specified DbEntity with the given qualifier
     * and orderings.
     * 
     * @param root
     *            the DbEntity this SelectQuery is for.
     * @param qualifier
     *            an Expression indicating which objects should be fetched.
     * @param orderings
     *            defines how to order the results, may be null.
     * @since 3.1
     */
    public SelectQuery(DbEntity root, Expression qualifier, List<? extends Ordering> orderings) {
        this();
        this.init(root, qualifier);
        addOrderings(orderings);
    }

    /**
     * Creates SelectQuery with <code>objEntityName</code> parameter.
     */
    public SelectQuery(String objEntityName) {
        this(objEntityName, null);
    }

    /**
     * Creates SelectQuery with <code>objEntityName</code> and
     * <code>qualifier</code> parameters.
     */
    public SelectQuery(String objEntityName, Expression qualifier) {
        this(objEntityName, qualifier, null);
    }

    /**
     * Creates a SelectQuery that selects objects of a given persistent class
     * that match supplied qualifier.
     * 
     * @param objEntityName
     *            the name of the ObjEntity to fetch from.
     * @param qualifier
     *            an Expression indicating which objects should be fetched.
     * @param orderings
     *            defines how to order the results, may be null.
     * @since 3.1
     */
    public SelectQuery(String objEntityName, Expression qualifier, List<? extends Ordering> orderings) {
        init(objEntityName, qualifier);
        addOrderings(orderings);
    }

    private void init(Object root, Expression qualifier) {
        this.setRoot(root);
        this.setQualifier(qualifier);
    }

    /**
     * @since 1.2
     */
    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {
        metaData.resolve(root, resolver, this);

        // must force DataRows if DbEntity is fetched
        if (root instanceof DbEntity) {
            QueryMetadataWrapper wrapper = new QueryMetadataWrapper(metaData);
            wrapper.override(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY, Boolean.TRUE);
            return wrapper;
        } else {
            return metaData;
        }
    }

    /**
     * Routes itself and if there are any prefetches configured, creates
     * prefetch queries and routes them as well.
     * 
     * @since 1.2
     */
    @Override
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        super.route(router, resolver, substitutedQuery);

        // suppress prefetches for paginated queries.. instead prefetches will
        // be resolved
        // per row...
        if (metaData.getPageSize() <= 0) {
            routePrefetches(router, resolver);
        }
    }

    /**
     * Creates and routes extra disjoint prefetch queries.
     * 
     * @since 1.2
     */
    void routePrefetches(QueryRouter router, EntityResolver resolver) {
        new SelectQueryPrefetchRouterAction().route(this, router, resolver);
    }

    /**
     * Calls "makeSelect" on the visitor.
     * 
     * @since 1.2
     */
    @Override
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.objectSelectAction(this);
    }

    /**
     * Initializes query parameters using a set of properties.
     * 
     * @since 1.1
     */
    public void initWithProperties(Map<String, ?> properties) {

        // must init defaults even if properties are empty
        if (properties == null) {
            properties = Collections.emptyMap();
        }

        Object distinct = properties.get(DISTINCT_PROPERTY);

        // init ivars from properties
        this.distinct = (distinct != null) ? "true".equalsIgnoreCase(distinct.toString()) : DISTINCT_DEFAULT;

        metaData.initWithProperties(properties);
    }

    /**
     * Prints itself as XML to the provided PrintWriter.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<query name=\"");
        encoder.print(getName());
        encoder.print("\" factory=\"");
        encoder.print("org.apache.cayenne.map.SelectQueryBuilder");

        String rootString = null;
        String rootType = null;

        if (root instanceof String) {
            rootType = MapLoader.OBJ_ENTITY_ROOT;
            rootString = root.toString();
        } else if (root instanceof ObjEntity) {
            rootType = MapLoader.OBJ_ENTITY_ROOT;
            rootString = ((ObjEntity) root).getName();
        } else if (root instanceof DbEntity) {
            rootType = MapLoader.DB_ENTITY_ROOT;
            rootString = ((DbEntity) root).getName();
        } else if (root instanceof Procedure) {
            rootType = MapLoader.PROCEDURE_ROOT;
            rootString = ((Procedure) root).getName();
        } else if (root instanceof Class<?>) {
            rootType = MapLoader.JAVA_CLASS_ROOT;
            rootString = ((Class<?>) root).getName();
        }

        if (rootType != null) {
            encoder.print("\" root=\"");
            encoder.print(rootType);
            encoder.print("\" root-name=\"");
            encoder.print(rootString);
        }

        encoder.println("\">");

        encoder.indent(1);

        // print properties
        if (distinct != DISTINCT_DEFAULT) {
            encoder.printProperty(DISTINCT_PROPERTY, distinct);
        }

        metaData.encodeAsXML(encoder);

        // encode qualifier
        if (qualifier != null) {
            encoder.print("<qualifier>");
            qualifier.encodeAsXML(encoder);
            encoder.println("</qualifier>");
        }

        // encode orderings
        if (orderings != null && !orderings.isEmpty()) {
            for (Ordering ordering : orderings) {
                ordering.encodeAsXML(encoder);
            }
        }

        encoder.indent(-1);
        encoder.println("</query>");
    }

    /**
     * A shortcut for {@link #queryWithParameters(Map, boolean)}that prunes
     * parts of qualifier that have no parameter value set.
     */
    public SelectQuery<T> queryWithParameters(Map<String, ?> parameters) {
        return queryWithParameters(parameters, true);
    }

    /**
     * Returns a query built using this query as a prototype, using a set of
     * parameters to build the qualifier.
     * 
     * @see org.apache.cayenne.exp.Expression#expWithParameters(java.util.Map,
     *      boolean) parameter substitution.
     */
    public SelectQuery<T> queryWithParameters(Map<String, ?> parameters, boolean pruneMissing) {
        // create a query replica
        SelectQuery<T> query = new SelectQuery<T>();
        query.setDistinct(distinct);

        query.metaData.copyFromInfo(this.metaData);
        query.setRoot(root);

        if (orderings != null) {
            query.addOrderings(orderings);
        }

        // substitute qualifier parameters
        if (qualifier != null) {
            query.setQualifier(qualifier.expWithParameters(parameters, pruneMissing));
        }

        return query;
    }

    /**
     * Creates and returns a new SelectQuery built using this query as a
     * prototype and substituting qualifier parameters with the values from the
     * map.
     * 
     * @since 1.1
     */
    public SelectQuery<T> createQuery(Map<String, ?> parameters) {
        return queryWithParameters(parameters);
    }

    /**
     * Adds ordering specification to this query orderings.
     */
    public void addOrdering(Ordering ordering) {
        nonNullOrderings().add(ordering);
    }

    /**
     * Adds a list of orderings.
     */
    public void addOrderings(List<? extends Ordering> orderings) {
        // If the supplied list of orderings is null, do not attempt to add
        // to the collection (addAll() will NPE otherwise).
        if (orderings != null)
            nonNullOrderings().addAll(orderings);
    }

    /**
     * Adds ordering specification to this query orderings.
     * 
     * @since 3.0
     */
    public void addOrdering(String sortPathSpec, SortOrder order) {
        addOrdering(new Ordering(sortPathSpec, order));
    }

    /**
     * Removes ordering.
     * 
     * @since 1.1
     */
    public void removeOrdering(Ordering ordering) {
        if (orderings != null) {
            orderings.remove(ordering);
        }
    }

    /**
     * Returns a list of orderings used by this query.
     */
    public List<Ordering> getOrderings() {
        return (orderings != null) ? orderings : Collections.<Ordering> emptyList();
    }

    /**
     * Clears all configured orderings.
     */
    public void clearOrderings() {
        orderings = null;
    }

    /**
     * Returns true if this query returns distinct rows.
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Sets <code>distinct</code> property that determines whether this query
     * returns distinct row.
     */
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    /**
     * Adds one or more aliases for the qualifier expression path. Aliases serve
     * to instruct Cayenne to generate separate sets of joins for overlapping
     * paths, that maybe needed for complex conditions. An example of an
     * <i>implicit</i> splits is this method:
     * {@link ExpressionFactory#matchAllExp(String, Object...)}.
     * 
     * @since 3.0
     */
    public void aliasPathSplits(String path, String... aliases) {
        metaData.addPathSplitAliases(path, aliases);
    }

    /**
     * @since 1.2
     */
    public PrefetchTreeNode getPrefetchTree() {
        return metaData.getPrefetchTree();
    }

    /**
     * @since 1.2
     */
    public void setPrefetchTree(PrefetchTreeNode prefetchTree) {
        metaData.setPrefetchTree(prefetchTree);
    }

    /**
     * Adds a prefetch with specified relationship path to the query.
     * 
     * @since 3.2
     */
    public PrefetchTreeNode addPrefetch(PrefetchTreeNode prefetchElement) {
        String path = prefetchElement.getPath();
        int semantics = prefetchElement.getSemantics();

        return metaData.addPrefetch(path, semantics);
    }

    /**
     * Adds a prefetch with specified relationship path to the query.
     * 
     * @since 1.2 signature changed to return created PrefetchTreeNode.
     */
    public PrefetchTreeNode addPrefetch(String prefetchPath) {
        return metaData.addPrefetch(prefetchPath, PrefetchTreeNode.UNDEFINED_SEMANTICS);
    }

    /**
     * Clears all stored prefetch paths.
     */
    public void clearPrefetches() {
        metaData.clearPrefetches();
    }

    /**
     * Removes prefetch.
     * 
     * @since 1.1
     */
    public void removePrefetch(String prefetchPath) {
        metaData.removePrefetch(prefetchPath);
    }

    /**
     * Returns <code>true</code> if this query should produce a list of data
     * rows as opposed to DataObjects, <code>false</code> for DataObjects. This
     * is a hint to QueryEngine executing this query.
     * 
     * @deprecated since 3.2, use {@link #dataRowQuery(Class, Expression)} to
     *             create DataRow query instead.
     */
    public boolean isFetchingDataRows() {
        return (root instanceof DbEntity) || metaData.isFetchingDataRows();
    }

    /**
     * Sets query result type. If <code>flag</code> parameter is
     * <code>true</code>, then results will be in the form of data rows.
     * <p>
     * <i>Note that if the root of this query is a {@link DbEntity}, this
     * setting has no effect, and data rows are always fetched. </i>
     * </p>
     * 
     * @deprecated since 3.2, use {@link #dataRowQuery(Class, Expression)} to
     *             create DataRow query instead.
     */
    public void setFetchingDataRows(boolean flag) {
        metaData.setFetchingDataRows(flag);
    }

    /**
     * @since 3.0
     */
    public QueryCacheStrategy getCacheStrategy() {
        return metaData.getCacheStrategy();
    }

    /**
     * @since 3.0
     */
    public void setCacheStrategy(QueryCacheStrategy strategy) {
        metaData.setCacheStrategy(strategy);
    }

    /**
     * @since 3.0
     */
    public String[] getCacheGroups() {
        return metaData.getCacheGroups();
    }

    /**
     * @since 3.0
     */
    public void setCacheGroups(String... cacheGroups) {
        this.metaData.setCacheGroups(cacheGroups);
    }

    /**
     * Instructs Cayenne to look for query results in the "local" cache when
     * running the query. This is a short-hand notation for:
     * 
     * <pre>
     * query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
     * query.setCacheGroups(&quot;group1&quot;, &quot;group2&quot;);
     * </pre>
     * 
     * @since 3.2
     */
    public void useLocalCache(String... cacheGroups) {
        setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        setCacheGroups(cacheGroups);
    }

    /**
     * Instructs Cayenne to look for query results in the "shared" cache when
     * running the query. This is a short-hand notation for:
     * 
     * <pre>
     * query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
     * query.setCacheGroups(&quot;group1&quot;, &quot;group2&quot;);
     * </pre>
     * 
     * @since 3.2
     */
    public void useSharedCache(String... cacheGroups) {
        setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        setCacheGroups(cacheGroups);
    }

    /**
     * Returns the fetchOffset.
     * 
     * @since 3.0
     */
    public int getFetchOffset() {
        return metaData.getFetchOffset();
    }

    /**
     * Returns the fetchLimit.
     */
    public int getFetchLimit() {
        return metaData.getFetchLimit();
    }

    /**
     * Sets the fetchLimit.
     */
    public void setFetchLimit(int fetchLimit) {
        this.metaData.setFetchLimit(fetchLimit);
    }

    /**
     * @since 3.0
     */
    public void setFetchOffset(int fetchOffset) {
        this.metaData.setFetchOffset(fetchOffset);
    }

    /**
     * Returns <code>pageSize</code> property. See setPageSize for more details.
     */
    public int getPageSize() {
        return metaData.getPageSize();
    }

    /**
     * Sets <code>pageSize</code> property.
     * 
     * By setting a page size, the Collection returned by performing a query
     * will return <i>hollow</i> DataObjects. This is considerably faster and
     * uses a tiny fraction of the memory compared to a non-paged query when
     * large numbers of objects are returned in the result. When a hollow
     * DataObject is accessed all DataObjects on the same page will be faulted
     * into memory. There will be a small delay when faulting objects while the
     * data is fetched from the data source, but otherwise you do not need to do
     * anything special to access data in hollow objects. The first page is
     * always faulted into memory immediately.
     * 
     * @param pageSize
     *            The pageSize to set
     */
    public void setPageSize(int pageSize) {
        metaData.setPageSize(pageSize);
    }

    /**
     * Returns a list that internally stores orderings, creating it on demand.
     * 
     * @since 1.2
     */
    List<Ordering> nonNullOrderings() {
        if (orderings == null) {
            orderings = new ArrayList<Ordering>(3);
        }

        return orderings;
    }

    /**
     * Sets statement's fetch size (0 for default size)
     * 
     * @since 3.0
     */
    public void setStatementFetchSize(int size) {
        metaData.setStatementFetchSize(size);
    }

    /**
     * @return statement's fetch size
     * @since 3.0
     */
    public int getStatementFetchSize() {
        return metaData.getStatementFetchSize();
    }
}
