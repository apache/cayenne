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
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.QueryBuilder;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A query based on Procedure. Can be used as a select query, or as a query of an
 * arbitrary complexity, performing data modification, selecting data (possibly with
 * multiple result sets per call), returning values via OUT parameters.
 * <h3>Execution with DataContext</h3>
 * <h4>Reading OUT parameters</h4>
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
 * 
 * @author Andrus Adamchik
 */
public class ProcedureQuery extends AbstractQuery implements ParameterizedQuery,
        XMLSerializable {

    /**
     * @since 1.2
     */
    protected String resultEntityName;

    /**
     * @since 1.2
     */
    protected Class<?> resultClass;

    protected Map<String, Object> parameters = new HashMap<String, Object>();

    ProcedureQueryMetadata metaData = new ProcedureQueryMetadata();

    // TODO: ColumnDescriptor is not XMLSerializable so we can't store
    // it in a DataMap
    /**
     * @since 1.2
     */
    protected List<ColumnDescriptor[]> resultDescriptors;

    /**
     * Creates an empty procedure query.
     */
    public ProcedureQuery() {
        // for backwards compatibility we go against usual default...
        metaData.setFetchingDataRows(true);
    }

    /**
     * Creates a ProcedureQuery based on a Procedure object.
     */
    public ProcedureQuery(Procedure procedure) {
        // for backwards compatibility we go against usual default...
        metaData.setFetchingDataRows(true);
        setRoot(procedure);
    }

    /**
     * Creates a ProcedureQuery based on a stored procedure.
     * <p>
     * Performance Note: with current EntityResolver implementation it is preferrable to
     * use Procedure object instead of String as a query root. String root can cause
     * unneeded EntityResolver reindexing on every call. See this mailing list thread: <a
     * href="http://objectstyle.org/cayenne/lists/cayenne-user/2005/01/0109.html">
     * http://objectstyle.org/cayenne/lists/cayenne-user/2005/01/0109.html</a>
     * </p>
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
    public ProcedureQuery(Procedure procedure, Class resultType) {
        setRoot(procedure);

        this.resultClass = resultType;
    }

    /**
     * <p>
     * Performance Note: with current EntityResolver implementation it is preferrable to
     * use Procedure object instead of String as a query root. String root can cause
     * unneeded EntityResolver reindexing on every call. See this mailing list thread: <a
     * href="http://objectstyle.org/cayenne/lists/cayenne-user/2005/01/0109.html">
     * http://objectstyle.org/cayenne/lists/cayenne-user/2005/01/0109.html</a>
     * </p>
     * 
     * @since 1.1
     */
    public ProcedureQuery(String procedureName, Class resultType) {
        setRoot(procedureName);

        this.resultClass = resultType;
    }

    /**
     * @since 1.2
     */
    public QueryMetadata getMetaData(EntityResolver resolver) {

        metaData.resolve(root, resultClass != null
                ? (Object) resultClass
                : resultEntityName, resolver, this);
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
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.procedureAction(this);
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
        encoder.print(QueryBuilder.PROCEDURE_ROOT);

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

    public String getCachePolicy() {
        return metaData.getCachePolicy();
    }

    public void setCachePolicy(String policy) {
        this.metaData.setCachePolicy(policy);
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
    public void setCacheGroups(String[] cachGroups) {
        this.metaData.setCacheGroups(cachGroups);
    }

    public int getFetchLimit() {
        return metaData.getFetchLimit();
    }

    public void setFetchLimit(int fetchLimit) {
        this.metaData.setFetchLimit(fetchLimit);
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

    public boolean isRefreshingObjects() {
        return metaData.isRefreshingObjects();
    }

    public void setRefreshingObjects(boolean flag) {
        metaData.setRefreshingObjects(flag);
    }

    public boolean isResolvingInherited() {
        return metaData.isResolvingInherited();
    }

    public void setResolvingInherited(boolean b) {
        metaData.setResolvingInherited(b);
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
}
