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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.QueryBuilder;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A query that selects persistent objects of a certain type or "raw data" (aka DataRows).
 * Supports expression qualifier, multiple orderings and a number of other parameters that
 * serve as runtime hints to Cayenne on how to optimize the fetch and result processing.
 * 
 * @author Andrus Adamchik
 */
public class SelectQuery extends QualifiedQuery implements ParameterizedQuery,
        XMLSerializable {

    public static final String DISTINCT_PROPERTY = "cayenne.SelectQuery.distinct";
    public static final boolean DISTINCT_DEFAULT = false;

    protected List customDbAttributes;
    protected List orderings;
    protected boolean distinct;

    /**
     * since 3.0M2 (scheduled for removal in 3.0M3) as DerivedDbEntity is deprecated.
     */
    protected Expression parentQualifier;
    
    /**
     * since 3.0M2 (scheduled for removal in 3.0M3) as DerivedDbEntity is deprecated.
     */
    protected String parentObjEntityName;

    SelectQueryMetadata selectInfo = new SelectQueryMetadata();

    /** Creates an empty SelectQuery. */
    public SelectQuery() {
    }

    /**
     * Creates a SelectQuery with null qualifier, for the specifed ObjEntity
     * 
     * @param root the ObjEntity this SelectQuery is for.
     */
    public SelectQuery(ObjEntity root) {
        this(root, null);
    }

    /**
     * Creates a SelectQuery for the specifed ObjEntity with the given qualifier
     * 
     * @param root the ObjEntity this SelectQuery is for.
     * @param qualifier an Expression indicating which objects should be fetched
     */
    public SelectQuery(ObjEntity root, Expression qualifier) {
        this();
        this.init(root, qualifier);
    }

    /**
     * Creates a SelectQuery that selects all objects of a given persistent class.
     * 
     * @param rootClass the Class of objects fetched by this query.
     */
    public SelectQuery(Class rootClass) {
        this(rootClass, null);
    }

    /**
     * Creates a SelectQuery that selects objects of a given persistent class that match
     * supplied qualifier.
     * 
     * @param rootClass the Class of objects fetched by this query.
     */
    public SelectQuery(Class rootClass, Expression qualifier) {
        init(rootClass, qualifier);
    }

    /**
     * Creates a SelectQuery for the specifed DbEntity.
     * 
     * @param root the DbEntity this SelectQuery is for.
     * @since 1.1
     */
    public SelectQuery(DbEntity root) {
        this(root, null);
    }

    /**
     * Creates a SelectQuery for the specifed DbEntity with the given qualifier.
     * 
     * @param root the DbEntity this SelectQuery is for.
     * @param qualifier an Expression indicating which objects should be fetched
     * @since 1.1
     */
    public SelectQuery(DbEntity root, Expression qualifier) {
        this();
        this.init(root, qualifier);
    }

    /**
     * Creates SelectQuery with <code>objEntityName</code> parameter.
     */
    public SelectQuery(String objEntityName) {
        this(objEntityName, null);
    }

    /**
     * Creates SelectQuery with <code>objEntityName</code> and <code>qualifier</code>
     * parameters.
     */
    public SelectQuery(String objEntityName, Expression qualifier) {
        init(objEntityName, qualifier);
    }

    private void init(Object root, Expression qualifier) {
        this.setRoot(root);
        this.setQualifier(qualifier);
    }

    /**
     * @since 1.2
     */
    public QueryMetadata getMetaData(EntityResolver resolver) {
        selectInfo.resolve(root, resolver, this);

        // must force DataRows if custom attributes are fetched
        if (isFetchingCustomAttributes()) {
            QueryMetadataWrapper wrapper = new QueryMetadataWrapper(selectInfo);
            wrapper.override(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY, Boolean.TRUE);
            return wrapper;
        }
        else {
            return selectInfo;
        }
    }

    /**
     * Routes itself and if there are any prefetches configured, creates prefetch queries
     * and routes them as well.
     * 
     * @since 1.2
     */
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        super.route(router, resolver, substitutedQuery);
        routePrefetches(router, resolver);
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
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.objectSelectAction(this);
    }

    /**
     * Initializes query parameters using a set of properties.
     * 
     * @since 1.1
     */
    public void initWithProperties(Map properties) {

        // must init defaults even if properties are empty
        if (properties == null) {
            properties = Collections.EMPTY_MAP;
        }

        Object distinct = properties.get(DISTINCT_PROPERTY);

        // init ivars from properties
        this.distinct = (distinct != null)
                ? "true".equalsIgnoreCase(distinct.toString())
                : DISTINCT_DEFAULT;

        selectInfo.initWithProperties(properties);
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
            rootType = QueryBuilder.OBJ_ENTITY_ROOT;
            rootString = root.toString();
        }
        else if (root instanceof ObjEntity) {
            rootType = QueryBuilder.OBJ_ENTITY_ROOT;
            rootString = ((ObjEntity) root).getName();
        }
        else if (root instanceof DbEntity) {
            rootType = QueryBuilder.DB_ENTITY_ROOT;
            rootString = ((DbEntity) root).getName();
        }
        else if (root instanceof Procedure) {
            rootType = QueryBuilder.PROCEDURE_ROOT;
            rootString = ((Procedure) root).getName();
        }
        else if (root instanceof Class) {
            rootType = QueryBuilder.JAVA_CLASS_ROOT;
            rootString = ((Class) root).getName();
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

        selectInfo.encodeAsXML(encoder);

        // encode qualifier
        if (qualifier != null) {
            encoder.print("<qualifier>");
            qualifier.encodeAsXML(encoder);
            encoder.println("</qualifier>");
        }

        // encode orderings
        if (orderings != null && !orderings.isEmpty()) {
            Iterator it = orderings.iterator();
            while (it.hasNext()) {
                Ordering ordering = (Ordering) it.next();
                ordering.encodeAsXML(encoder);
            }
        }

        encoder.indent(-1);
        encoder.println("</query>");
    }

    /**
     * A shortcut for {@link #queryWithParameters(Map, boolean)}that prunes parts of
     * qualifier that have no parameter value set.
     */
    public SelectQuery queryWithParameters(Map parameters) {
        return queryWithParameters(parameters, true);
    }

    /**
     * Returns a query built using this query as a prototype, using a set of parameters to
     * build the qualifier.
     * 
     * @see org.apache.cayenne.exp.Expression#expWithParameters(java.util.Map, boolean)
     *      parameter substitution.
     */
    public SelectQuery queryWithParameters(Map parameters, boolean pruneMissing) {
        // create a query replica
        SelectQuery query = new SelectQuery();
        query.setDistinct(distinct);

        query.selectInfo.copyFromInfo(this.selectInfo);
        query.setParentObjEntityName(parentObjEntityName);
        query.setParentQualifier(parentQualifier);
        query.setRoot(root);

        // The following algorithm is for building the new query name based
        // on the original query name and a hashcode of the map of parameters.
        // This way the query clone can take advantage of caching. Fixes
        // problem reported in CAY-360.

        if (!Util.isEmptyString(name)) {
            StringBuffer buffer = new StringBuffer(name);

            if (parameters != null && !parameters.isEmpty()) {
                buffer.append(parameters.hashCode());
            }

            query.setName(buffer.toString());
        }

        if (orderings != null) {
            query.addOrderings(orderings);
        }

        if (customDbAttributes != null) {
            query.addCustomDbAttributes(customDbAttributes);
        }

        // substitute qualifier parameters
        if (qualifier != null) {
            query.setQualifier(qualifier.expWithParameters(parameters, pruneMissing));
        }

        return query;
    }

    /**
     * Creates and returns a new SelectQuery built using this query as a prototype and
     * substituting qualifier parameters with the values from the map.
     * 
     * @since 1.1
     */
    public Query createQuery(Map parameters) {
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
    public void addOrderings(List orderings) {
        nonNullOrderings().addAll(orderings);
    }

    /** Adds ordering specification to this query orderings. */
    public void addOrdering(String sortPathSpec, boolean isAscending) {
        this.addOrdering(new Ordering(sortPathSpec, isAscending));
    }

    /** Adds ordering specification to this query orderings. */
    public void addOrdering(String sortPathSpec, boolean isAscending, boolean ignoreCase) {
        this.addOrdering(new Ordering(sortPathSpec, isAscending, ignoreCase));
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
    public List getOrderings() {
        return (orderings != null) ? orderings : Collections.EMPTY_LIST;
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
     * Sets <code>distinct</code> property that determines whether this query returns
     * distinct row.
     */
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    /**
     * Returns a list of attributes that will be included in the results of this query.
     */
    public List getCustomDbAttributes() {
        // if query root is DbEntity, and no custom attributes
        // are defined, return DbEntity attributes.
        if ((customDbAttributes == null || customDbAttributes.isEmpty())
                && (root instanceof DbEntity)) {
            Collection attributes = ((DbEntity) root).getAttributes();
            List attributeNames = new ArrayList(attributes.size());
            Iterator it = attributes.iterator();
            while (it.hasNext()) {
                DbAttribute attribute = (DbAttribute) it.next();
                attributeNames.add(attribute.getName());
            }

            return attributeNames;
        }
        else {
            return (customDbAttributes != null)
                    ? customDbAttributes
                    : Collections.EMPTY_LIST;
        }
    }

    /**
     * Adds a path to the DbAttribute that should be included in the results of this
     * query. Valid paths would look like <code>ARTIST_NAME</code>,
     * <code>PAINTING_ARRAY.PAINTING_ID</code>, etc.
     */
    public void addCustomDbAttribute(String attributePath) {
        nonNullCustomDbAttributes().add(attributePath);
    }

    public void addCustomDbAttributes(List attrPaths) {
        nonNullCustomDbAttributes().addAll(attrPaths);
    }

    /**
     * Returns <code>true</code> if there is at least one custom query attribute
     * specified, otherwise returns <code>false</code> for the case when the query
     * results will contain only the root entity attributes.
     * <p>
     * Note that queries that are fetching custom attributes always return data rows
     * instead of DataObjects.
     * </p>
     */
    public boolean isFetchingCustomAttributes() {
        return (root instanceof DbEntity)
                || (customDbAttributes != null && !customDbAttributes.isEmpty());
    }

    /**
     * @since 1.2
     */
    public PrefetchTreeNode getPrefetchTree() {
        return selectInfo.getPrefetchTree();
    }

    /**
     * @since 1.2
     */
    public void setPrefetchTree(PrefetchTreeNode prefetchTree) {
        selectInfo.setPrefetchTree(prefetchTree);
    }

    /**
     * Adds a prefetch with specified relationship path to the query.
     * 
     * @since 1.2 signature changed to return created PrefetchTreeNode.
     */
    public PrefetchTreeNode addPrefetch(String prefetchPath) {
        return selectInfo.addPrefetch(prefetchPath, PrefetchTreeNode.UNDEFINED_SEMANTICS);
    }

    /**
     * Clears all stored prefetch paths.
     */
    public void clearPrefetches() {
        selectInfo.clearPrefetches();
    }

    /**
     * Removes prefetch.
     * 
     * @since 1.1
     */
    public void removePrefetch(String prefetchPath) {
        selectInfo.removePrefetch(prefetchPath);
    }

    /**
     * Returns <code>true</code> if this query should produce a list of data rows as
     * opposed to DataObjects, <code>false</code> for DataObjects. This is a hint to
     * QueryEngine executing this query.
     */
    public boolean isFetchingDataRows() {
        return this.isFetchingCustomAttributes() || selectInfo.isFetchingDataRows();
    }

    /**
     * Sets query result type. If <code>flag</code> parameter is <code>true</code>,
     * then results will be in the form of data rows.
     * <p>
     * <i>Note that if <code>isFetchingCustAttributes()</code> returns <code>true</code>,
     * this setting has no effect, and data rows are always fetched. </i>
     * </p>
     */
    public void setFetchingDataRows(boolean flag) {
        selectInfo.setFetchingDataRows(flag);
    }

    /**
     * Returns refresh policy of this query. Default is <code>true</code>.
     * 
     * @since 1.1
     */
    public boolean isRefreshingObjects() {
        return selectInfo.isRefreshingObjects();
    }

    /**
     * @since 1.1
     */
    public void setRefreshingObjects(boolean flag) {
        selectInfo.setRefreshingObjects(flag);
    }

    /**
     * @since 1.1
     */
    public String getCachePolicy() {
        return selectInfo.getCachePolicy();
    }

    /**
     * @since 1.1
     */
    public void setCachePolicy(String policy) {
        this.selectInfo.setCachePolicy(policy);
    }

    /**
     * @since 3.0
     */
    public String[] getCacheGroups() {
        return selectInfo.getCacheGroups();
    }

    /**
     * @since 3.0
     */
    public void setCacheGroups(String[] cachGroups) {
        this.selectInfo.setCacheGroups(cachGroups);
    }

    /**
     * Returns the fetchLimit.
     * 
     * @return int
     */
    public int getFetchLimit() {
        return selectInfo.getFetchLimit();
    }

    /**
     * Sets the fetchLimit.
     * 
     * @param fetchLimit The fetchLimit to set
     */
    public void setFetchLimit(int fetchLimit) {
        this.selectInfo.setFetchLimit(fetchLimit);
    }

    /** 
     * Setter for query's parent entity qualifier. 
     * 
     * @deprecated since 3.0M2 (scheduled for removal in 3.0M3) as DerivedDbEntity is deprecated.
     */
    public void setParentQualifier(Expression parentQualifier) {
        this.parentQualifier = parentQualifier;
    }

    /** 
     * Getter for query parent entity qualifier. 
     * 
     * @deprecated since 3.0M2 (scheduled for removal in 3.0M3) as DerivedDbEntity is deprecated.
     */
    public Expression getParentQualifier() {
        return parentQualifier;
    }

    /**
     * Adds specified parent entity qualifier to the existing parent entity qualifier
     * joining it using "AND".
     * 
     * @deprecated since 3.0M2 (scheduled for removal in 3.0M3) as DerivedDbEntity is deprecated.
     */
    public void andParentQualifier(Expression e) {
        parentQualifier = (parentQualifier != null) ? parentQualifier.andExp(e) : e;
    }

    /**
     * Adds specified parent entity qualifier to the existing qualifier joining it using
     * "OR".
     * 
     * @deprecated since 3.0M2 (scheduled for removal in 3.0M3) as DerivedDbEntity is deprecated.
     */
    public void orParentQualifier(Expression e) {
        parentQualifier = (parentQualifier != null) ? parentQualifier.orExp(e) : e;
    }

    /**
     * Returns the name of parent ObjEntity.
     * 
     * @deprecated since 3.0M2 (scheduled for removal in 3.0M3) as DerivedDbEntity is deprecated.
     */
    public String getParentObjEntityName() {
        return parentObjEntityName;
    }

    /**
     * Sets the name of parent ObjEntity. If query's root ObjEntity maps to a derived
     * entity in the DataMap, this query qualifier will resolve to a HAVING clause of an
     * SQL statement. To allow fine tuning the query before applying GROUP BY and HAVING,
     * callers can setup the name of parent ObjEntity and parent qualifier that will be
     * used to create WHERE clause preceeding GROUP BY.
     * <p>
     * For instance this is helpful to qualify the fetch on a related entity attributes,
     * since HAVING does not allow joins.
     * </p>
     * 
     * @param parentObjEntityName The parentObjEntityName to set
     * @deprecated since 3.0M2 (scheduled for removal in 3.0M3) as DerivedDbEntity is deprecated.
     */
    public void setParentObjEntityName(String parentObjEntityName) {
        this.parentObjEntityName = parentObjEntityName;
    }

    /**
     * Returns <code>true</code> if this query has an extra qualifier that uses a parent
     * entity of the query root entity for additional result filtering.
     * 
     * @deprecated since 3.0M2 (scheduled for removal in 3.0M3) as DerivedDbEntity is deprecated.
     */
    public boolean isQualifiedOnParent() {
        return getParentObjEntityName() != null && parentQualifier != null;
    }

    /**
     * Returns <code>pageSize</code> property. Page size is a hint telling Cayenne
     * QueryEngine that query result should use paging instead of reading the whole result
     * in the memory.
     * 
     * @return int
     */
    public int getPageSize() {
        return selectInfo.getPageSize();
    }

    /**
     * Sets <code>pageSize</code> property.
     * 
     * @param pageSize The pageSize to set
     */
    public void setPageSize(int pageSize) {
        selectInfo.setPageSize(pageSize);
    }

    /**
     * Returns true if objects fetched via this query should be fully resolved according
     * to the inheritance hierarchy.
     * 
     * @since 1.1
     */
    public boolean isResolvingInherited() {
        return selectInfo.isResolvingInherited();
    }

    /**
     * Sets whether the objects fetched via this query should be fully resolved according
     * to the inheritance hierarchy.
     * 
     * @since 1.1
     */
    public void setResolvingInherited(boolean b) {
        selectInfo.setResolvingInherited(b);
    }

    /**
     * Returns a list that internally stores custom db attributes, creating it on demand.
     * 
     * @since 1.2
     */
    List nonNullCustomDbAttributes() {
        if (customDbAttributes == null) {
            customDbAttributes = new ArrayList();
        }

        return customDbAttributes;
    }

    /**
     * Returns a list that internally stores orderings, creating it on demand.
     * 
     * @since 1.2
     */
    List nonNullOrderings() {
        if (orderings == null) {
            orderings = new ArrayList();
        }

        return orderings;
    }
}
