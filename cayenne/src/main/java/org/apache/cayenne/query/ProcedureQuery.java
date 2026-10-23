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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.OutParametersResult;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.EntityResolver;
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
 * keyed by parameter name (see {@link OutParametersResult}).
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
 * <h3>Query Root</h3>
 * <p>
 * A ProcedureQuery is defined by the name of a mapped stored procedure and, optionally, by the type of the persistent
 * objects it fetches, given either as a Java class ({@link #query(String, Class)}) or as an ObjEntity name
 * ({@link #entityQuery(String, String)}). Both are fixed when the query is created. A query with no result type
 * ({@link #query(String)}) fetches DataRows.
 * </p>
 *
 * @param <T> the type of the result elements: a {@link DataRow} by default, or a persistent
 *            object when a result type is specified.
 */
public class ProcedureQuery<T> implements Select<T> {

    /**
     * Creates a query calling the named stored procedure and fetching DataRows.
     *
     * @since 6.0
     */
    public static ProcedureQuery<DataRow> query(String procedureName) {
        return new ProcedureQuery<>(procedureName);
    }

    /**
     * Creates a query calling the named stored procedure and fetching objects of the given persistent type.
     *
     * @since 6.0
     */
    public static <T> ProcedureQuery<T> query(String procedureName, Class<T> resultType) {
        return new ProcedureQuery<>(procedureName, resultType);
    }

    /**
     * Creates a query calling the named stored procedure and fetching objects of the named ObjEntity. This is the
     * form of the queries mapped in a DataMap, where the entity class is not necessarily known.
     *
     * @since 6.0
     */
    public static ProcedureQuery<Persistent> entityQuery(String procedureName, String resultEntityName) {
        return new ProcedureQuery<>(procedureName, null, resultEntityName);
    }

    private final String procedureName;

    // the type of the fetched objects: at most one of the two is set
    private final Class<T> resultType;
    private final String resultEntityName;

    protected CapsStrategy columnNamesCapitalization;
    protected Map<String, Object> parameters = new HashMap<>();
    ProcedureQueryMetadata metaData = new ProcedureQueryMetadata();
    protected List<ProcedureColumn[]> resultDescriptors;

    /**
     * Creates a query calling the named stored procedure and fetching DataRows.
     *
     * @param procedureName A name of the stored procedure. For this query to work, a
     *            procedure with this name must be mapped in Cayenne.
     */
    public ProcedureQuery(String procedureName) {
        this(procedureName, null, null);

        // for backwards compatibility we go against usual default...
        metaData.setFetchingDataRows(true);
    }

    /**
     * Creates a query calling the named stored procedure and fetching objects of the given persistent type.
     *
     * @since 1.1
     */
    public ProcedureQuery(String procedureName, Class<T> resultType) {
        this(procedureName, resultType, null);
    }

    private ProcedureQuery(String procedureName, Class<T> resultType, String resultEntityName) {
        this.procedureName = procedureName;
        this.resultType = resultType;
        this.resultEntityName = resultEntityName;
    }

    String getProcedureName() {
        return procedureName;
    }

    Class<T> getResultType() {
        return resultType;
    }

    String getResultEntityName() {
        return resultEntityName;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this).append("procedureName", procedureName);
        if (resultType != null) {
            builder.append("resultType", resultType.getName());
        } else if (resultEntityName != null) {
            builder.append("resultEntityName", resultEntityName);
        }
        return builder.toString();
    }

    /**
     * @since 1.2
     */
    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {
        metaData.resolve(resolver, this);
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
     * @since 4.0
     */
    public String getCacheGroup() {
        return metaData.getCacheGroup();
    }

    /**
     * @since 4.0
     */
    public void setCacheGroup(String cacheGroup) {
        metaData.setCacheGroup(cacheGroup);
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
