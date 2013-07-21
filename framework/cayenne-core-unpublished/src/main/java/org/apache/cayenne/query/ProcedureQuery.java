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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A query based on Procedure. Can be used as a select query, or as a query of an
 * arbitrary complexity, performing data modification, selecting data (possibly with
 * multiple result sets per call), returning values via OUT parameters. <h3>Execution with
 * DataContext</h3> <h4>Reading OUT parameters</h4>
 * <p>
 * If a ProcedureQuery has OUT parameters, they are wrapped in a separate List in the
 * query result. Such list will contain a single Map with OUT parameter values.
 * </p>
 * <h4>Using ProcedureQuery as a GenericSelectQuery</h4>
 * <p>
 * Executing ProcedureQuery via
 * {@link org.apache.cayenne.access.DataContext#performQuery(Query)} makes sense only if
 * the stored procedure returns a single result set (or alternatively returns a result via
 * OUT parameters and no other result sets). It is still OK if data modification occurs as
 * a side effect. However if the query returns more then one result set, a more generic
 * form should be used:
 * {@link org.apache.cayenne.access.DataContext#performGenericQuery(Query)}.
 * </p>
 */
public class ProcedureQuery extends AbstractQuery implements ParameterizedQuery,
        XMLSerializable {

    static final String COLUMN_NAME_CAPITALIZATION_PROPERTY = "cayenne.ProcedureQuery.columnNameCapitalization";

    /**
     * @since 1.2
     */
    protected String resultEntityName;

    /**
     * @since 1.2
     */
    protected Class<?> resultClass;
    protected CapsStrategy columnNamesCapitalization;

    protected Map<String, Object> parameters = new HashMap<String, Object>();

    ProcedureQueryMetadata metaData = new ProcedureQueryMetadata();

    // TODO: ColumnDescriptor is not XMLSerializable so we can't store
    // it in a DataMap
    /**
     * @since 1.2
     */
    protected List<ColumnDescriptor[]> resultDescriptors;

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
    public ProcedureQuery(Procedure procedure, Class<?> resultType) {
        setRoot(procedure);

        this.resultClass = resultType;
    }

    /**
     * @since 1.1
     */
    public ProcedureQuery(String procedureName, Class<?> resultType) {
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
     * only one mechanism for returning result sets. </i>
     * </p>
     * 
     * @since 1.2
     */
    public List<ColumnDescriptor[]> getResultDescriptors() {
        return resultDescriptors != null ? resultDescriptors : Collections.EMPTY_LIST;
    }

    /**
     * Adds a descriptor for a single ResultSet. More than one descriptor can be added by
     * calling this method multiple times in the order of described ResultSet appearance
     * in the procedure results.
     * 
     * @since 1.2
     */
    public synchronized void addResultDescriptor(ColumnDescriptor[] descriptor) {
        if (resultDescriptors == null) {
            resultDescriptors = new ArrayList<ColumnDescriptor[]>(2);
        }

        resultDescriptors.add(descriptor);
    }

    /**
     * Removes result descriptor from the list of descriptors.
     * 
     * @since 1.2
     */
    public void removeResultDescriptor(ColumnDescriptor[] descriptor) {
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
     * Initializes query parameters using a set of properties.
     * 
     * @since 1.1
     */
    public void initWithProperties(Map<String, ?> properties) {

        // must init defaults even if properties are empty
        if (properties == null) {
            properties = Collections.EMPTY_MAP;
        }
        Object columnNamesCapitalization = properties
                .get(COLUMN_NAME_CAPITALIZATION_PROPERTY);
        this.columnNamesCapitalization = (columnNamesCapitalization != null)
                ? CapsStrategy
                        .valueOf(columnNamesCapitalization.toString().toUpperCase())
                : null;

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
        encoder.print("org.apache.cayenne.map.ProcedureQueryBuilder");

        encoder.print("\" root=\"");
        encoder.print(MapLoader.PROCEDURE_ROOT);

        String rootString = null;

        if (root instanceof String) {
            rootString = root.toString();
        }
        else if (root instanceof Procedure) {
            rootString = ((Procedure) root).getName();
        }

        if (rootString != null) {
            encoder.print("\" root-name=\"");
            encoder.print(rootString);
        }

        if (resultEntityName != null) {
            encoder.print("\" result-entity=\"");
            encoder.print(resultEntityName);
        }

        encoder.println("\">");
        encoder.indent(1);

        metaData.encodeAsXML(encoder);
        if (getColumnNamesCapitalization() != CapsStrategy.DEFAULT) {
            encoder.printProperty(
                    COLUMN_NAME_CAPITALIZATION_PROPERTY,
                    getColumnNamesCapitalization().name());
        }

        encoder.indent(-1);
        encoder.println("</query>");
    }

    /**
     * Creates and returns a new ProcedureQuery built using this query as a prototype and
     * substituting template parameters with the values from the map.
     * 
     * @since 1.1
     */
    public Query createQuery(Map<String, ?> parameters) {
        // create a query replica
        ProcedureQuery query = new ProcedureQuery();

        if (root != null) {
            query.setRoot(root);
        }

        query.setResultEntityName(resultEntityName);
        query.metaData.copyFromInfo(this.metaData);
        query.setParameters(parameters);

        // TODO: implement algorithm for building the name based on the original name and
        // the hashcode of the map of parameters. This way query clone can take advantage
        // of caching.
        return query;
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
}
