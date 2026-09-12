/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.query;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.util.ToStringBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A query based on Procedure. Can be used as a select query, or as a query of an
 * arbitrary complexity, performing data modification, selecting data (possibly with
 * multiple result sets per call), returning values via OUT parameters.
 * <h3>Execution with DataContext</h3>
 * <h4>Reading OUT parameters</h4>
 * <p>
 * If a ProcedureQuery has OUT parameters, their values are reported as a separate item of the query response, a Map
 * keyed by parameter name (see {@link QueryResult.OutParameters}).
 * </p>
 * <h4>Using ProcedureQuery as a Select</h4>
 * <p>
 * Executing ProcedureQuery via {@link ObjectContext#select(Select)} makes sense only if
 * the stored procedure returns a single result set (or alternatively returns a result via
 * OUT parameters and no other result sets). It is still OK if data modification occurs as
 * a side effect. However, if the query returns more then one result set, a more generic
 * form should be used: {@link ObjectContext#execute(Query)}.
 * </p>
 *
 * @param <T> the type of the result elements: a {@link org.apache.cayenne.DataRow} by default, or a persistent
 *            object when a result type is specified.
 */
public class ProcedureQuery<T> extends CacheableQuery implements Select<T> {

    public static final String COLUMN_NAME_CAPITALIZATION_PROPERTY = "cayenne.ProcedureQuery.columnNameCapitalization";

    protected String resultEntityName;
    protected Class<?> resultClass;
    protected CapsStrategy columnNamesCapitalization;
    protected Map<String, Object> parameters = new HashMap<>();
    ProcedureQueryMetadata metaData = new ProcedureQueryMetadata();
    protected List<ProcedureColumn[]> resultDescriptors;

    /**
     * The root object of this query. May be an entity name, Java class, ObjEntity or
     * DbEntity, depending on the specific query and how it was constructed.
     */
    protected Object root;

    /**
     * Returns the root of this query.
     */
    public Object getRoot() {
        return root;
    }

    /**
     * Sets the root of the query.
     *
     * @param value The new root
     * @throws IllegalArgumentException if value is not a String, ObjEntity, DbEntity,
     *             Procedure, DataMap, Class or null.
     */
    public void setRoot(Object value) {
        if (value != null && !(value instanceof String
                || value instanceof ObjEntity
                || value instanceof DbEntity
                || value instanceof Class
                || value instanceof Procedure
                || value instanceof DataMap)) {

            throw new IllegalArgumentException(("%s: \"setRoot(..)\" takes a DataMap, String, ObjEntity, DbEntity, "
                    + "Procedure, or Class. It was passed a %s")
                    .formatted(getClass().getName(), value.getClass().getName()));
        }

        this.root = value;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("root", root)
                .toString();
    }

    /**
     * Creates an empty procedure query. The query would fetch DataRows. Fetching
     * Persistent objects can be achieved either by using
     * {@link #ProcedureQuery(String, Class)} constructor or by calling
     * {@link #setFetchingDataRows(boolean)} and {@link #setResultEntityName(String)}
     * methods.
     */
    public ProcedureQuery() {
        // for backwards compatibility we go against usual default...
        metaData.setFetchingDataRows(true);
    }

    /**
     * Creates a ProcedureQuery based on a Procedure object. The query would fetch
     * DataRows. Fetching Persistent objects can be achieved either by using
     * {@link #ProcedureQuery(String, Class)} constructor or by calling
     * {@link #setFetchingDataRows(boolean)} and {@link #setResultEntityName(String)}
     * methods.
     */
    public ProcedureQuery(Procedure procedure) {
        // for backwards compatibility we go against usual default...
        metaData.setFetchingDataRows(true);
        setRoot(procedure);
    }

    /**
     * Creates a ProcedureQuery based on a stored procedure. The query would fetch
     * DataRows. Fetching Persistent objects can be achieved either by using
     * {@link #ProcedureQuery(String, Class)} constructor or by calling
     * {@link #setFetchingDataRows(boolean)} and {@link #setResultEntityName(String)}
     * methods.
     * 
     * @param procedureName A name of the stored procedure. For this query to work, a
     *            procedure with this name must be mapped in Cayenne.
     */
    public ProcedureQuery(String procedureName) {
        // for backwards compatibility we go against usual default...
        metaData.setFetchingDataRows(true);

        setRoot(procedureName);
    }

    /**
     * @since 1.1
     */
    public ProcedureQuery(Procedure procedure, Class<T> resultType) {
        setRoot(procedure);

        this.resultClass = resultType;
    }

    /**
     * @since 1.1
     */
    public ProcedureQuery(String procedureName, Class<T> resultType) {
        setRoot(procedureName);

        this.resultClass = resultType;
    }

    /**
     * @since 1.2
     */
    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {

        metaData.resolve(
                root,
                resultClass != null ? resultClass : resultEntityName,
                resolver,
                this);
        return metaData;
    }

    /**
     * Returns a List of descriptors for query ResultSets in the order they are returned
     * by the stored procedure.
     * <p>
     * <i>Note that if a procedure returns ResultSet in an OUT parameter, it is returned
     * prior to any other result sets (though in practice database engines usually support
     * only one mechanism for returning result sets).</i>
     * </p>
     * 
     * @since 5.0
     */
    public List<ProcedureColumn[]> getResultDescriptors() {
        return resultDescriptors != null ? resultDescriptors : Collections.emptyList();
    }

    /**
     * Adds a descriptor for a single ResultSet. More than one descriptor can be added by
     * calling this method multiple times in the order of described ResultSet appearance
     * in the procedure results.
     * 
     * @since 5.0
     */
    public synchronized void addResultDescriptor(ProcedureColumn[] descriptor) {
        if (resultDescriptors == null) {
            resultDescriptors = new ArrayList<>(2);
        }

        resultDescriptors.add(descriptor);
    }

    /**
     * Removes result descriptor from the list of descriptors.
     * 
     * @since 5.0
     */
    public void removeResultDescriptor(ProcedureColumn[] descriptor) {
        if (resultDescriptors != null) {
            resultDescriptors.remove(descriptor);
        }
    }

    /**
     * Calls "makeProcedure" on the visitor.
     * 
     * @since 1.2
     */
    @Override
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.procedureAction(this);
    }

    /**
     * @since 5.0
     */
    @Override
    public T selectFirst(ObjectContext context) {
        setFetchLimit(1);
        return context.selectFirst(this);
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
        Object columnNamesCapitalization = properties
                .get(COLUMN_NAME_CAPITALIZATION_PROPERTY);
        this.columnNamesCapitalization = (columnNamesCapitalization != null)
                ? CapsStrategy
                        .valueOf(columnNamesCapitalization.toString().toUpperCase())
                : null;

        metaData.initWithProperties(properties);
    }

    @Override
    protected BaseQueryMetadata getBaseMetaData() {
        return metaData;
    }

    public int getFetchLimit() {
        return metaData.getFetchLimit();
    }

    public void setFetchLimit(int fetchLimit) {
        this.metaData.setFetchLimit(fetchLimit);
    }

    /**
     * @since 3.0
     */
    public int getFetchOffset() {
        return metaData.getFetchOffset();
    }

    /**
     * @since 3.0
     */
    public void setFetchOffset(int fetchOffset) {
        metaData.setFetchOffset(fetchOffset);
    }

    public int getPageSize() {
        return metaData.getPageSize();
    }

    public void setPageSize(int pageSize) {
        metaData.setPageSize(pageSize);
    }

    public void setFetchingDataRows(boolean flag) {
        metaData.setFetchingDataRows(flag);
    }

    public boolean isFetchingDataRows() {
        return metaData.isFetchingDataRows();
    }

    /**
     * Adds a named parameter to the internal map of parameters.
     * 
     * @since 1.1
     */
    public synchronized void addParameter(String name, Object value) {
        parameters.put(name, value);
    }

    /**
     * @since 1.1
     */
    public synchronized void removeParameter(String name) {
        parameters.remove(name);
    }

    /**
     * Returns a map of procedure parameters.
     * 
     * @since 1.1
     */
    public Map<String, ?> getParameters() {
        return parameters;
    }

    /**
     * Sets a map of parameters.
     * 
     * @since 1.1
     */
    public synchronized void setParameters(Map<String, ?> parameters) {
        this.parameters.clear();

        if (parameters != null) {
            this.parameters.putAll(parameters);
        }
    }

    /**
     * Cleans up all configured parameters.
     * 
     * @since 1.1
     */
    public synchronized void clearParameters() {
        this.parameters.clear();
    }

    /**
     * @since 1.2
     */
    public PrefetchTreeNode getPrefetchTree() {
        return metaData.getPrefetchTree();
    }

    /**
     * Adds a prefetch.
     * 
     * @since 1.2
     */
    public PrefetchTreeNode addPrefetch(String prefetchPath) {
        // by default use JOINT_PREFETCH_SEMANTICS
        return metaData.addPrefetch(
                prefetchPath,
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
    }

    /**
     * @since 1.2
     */
    public void removePrefetch(String prefetch) {
        metaData.removePrefetch(prefetch);
    }

    /**
     * Adds all prefetches from a provided collection.
     * 
     * @since 1.2
     */
    public void addPrefetches(Collection<String> prefetches) {
        metaData.addPrefetches(prefetches, PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
    }

    /**
     * Clears all prefetches.
     * 
     * @since 1.2
     */
    public void clearPrefetches() {
        metaData.clearPrefetches();
    }

    /**
     * @since 1.2
     */
    public String getResultEntityName() {
        return resultEntityName;
    }

    /**
     * @since 1.2
     */
    public void setResultEntityName(String resultEntityName) {
        this.resultEntityName = resultEntityName;
    }

    /**
     * @since 3.0
     */
    public CapsStrategy getColumnNamesCapitalization() {
        return columnNamesCapitalization != null
                ? columnNamesCapitalization
                : CapsStrategy.DEFAULT;
    }

    /**
     * @since 3.0
     */
    public void setColumnNamesCapitalization(CapsStrategy columnNameCapitalization) {
        this.columnNamesCapitalization = columnNameCapitalization;
    }

    /**
     * Sets statement's fetch size (0 for no default size)
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

    /**
     * @return query timeout
     * @since 4.2
     */
    public int getQueryTimeout() {
        return metaData.getQueryTimeout();
    }

    /**
     * Set's query timeout
     * @since 4.2
     */
    public void setQueryTimeout(int timeout) {
        metaData.setQueryTimeout(timeout);
    }
}
