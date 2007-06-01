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

package org.objectstyle.cayenne.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.jdbc.ColumnDescriptor;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.QueryBuilder;
import org.objectstyle.cayenne.util.XMLEncoder;
import org.objectstyle.cayenne.util.XMLSerializable;

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
 * {@link org.objectstyle.cayenne.access.DataContext#performQuery(GenericSelectQuery)}
 * makes sense only if the stored procedure returns a single result set (or alternatively
 * returns a result via OUT parameters and no other result sets). It is still OK if data
 * modification occurs as a side effect. However if the query returns more then one result
 * set, a more generic form should be used:
 * {@link org.objectstyle.cayenne.access.DataContext#performQueries(java.util.Collection, 
 * org.objectstyle.cayenne.access.OperationObserver) DataContextperformQueries(Collection,
 * OperationObserver)}. Use {@link org.objectstyle.cayenne.access.QueryResult}as a
 * convenient generic OperationObserver implementation.
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class ProcedureQuery extends AbstractQuery implements GenericSelectQuery,
        ParameterizedQuery, XMLSerializable {

    /**
     * If set, allows to fetch results as DataObjects.
     * 
     * @since 1.1
     */
    protected String resultClassName;
    
    /**
     * @since 1.1.3
     */
    protected List resultDescriptors;

    protected Map parameters = new HashMap();
    protected SelectExecutionProperties selectProperties = new SelectExecutionProperties();
    protected boolean selecting;

    /**
     * Creates an empty procedure query.
     */
    public ProcedureQuery() {
        // for backwards compatibility we go against usual default...
        selectProperties.setFetchingDataRows(true);
    }

    /**
     * Creates a ProcedureQuery based on a Procedure object.
     */
    public ProcedureQuery(Procedure procedure) {
        // for backwards compatibility we go against usual default...
        selectProperties.setFetchingDataRows(true);
        setRoot(procedure);
    }

    /**
     * Creates a ProcedureQuery based on a stored procedure.
     * 
     * @param procedureName A name of the stored procedure. For this query to work, a
     *            procedure with this name must be mapped in Cayenne.
     */
    public ProcedureQuery(String procedureName) {
        // for backwards compatibility we go against usual default...
        selectProperties.setFetchingDataRows(true);

        setRoot(procedureName);
    }

    /**
     * @since 1.1
     */
    public ProcedureQuery(Procedure procedure, Class resultType) {
        setRoot(procedure);
        setResultClassName(resultType != null ? resultType.getName() : null);
    }

    /**
     * @since 1.1
     */
    public ProcedureQuery(String procedureName, Class resultType) {
        setRoot(procedureName);
        setResultClassName(resultType != null ? resultType.getName() : null);
    }
    
    /**
     * Returns a List of #{@link RowDescriptor}
     * objects describing query ResultSets in the order they are returned by the stored
     * procedure.
     * <p>
     * <i>Note that if a procedure returns ResultSet in an OUT parameter, it is returned
     * prior to any other result sets (though in practice database engines usually support
     * only one mechanism for returning result sets. </i>
     * </p>
     * 
     * @since 1.1.3
     */
    public List getResultDescriptors() {
        return resultDescriptors != null ? resultDescriptors : Collections.EMPTY_LIST;
    }

    /**
     * Adds a descriptor for a single ResultSet. More than one descriptor can be added by
     * calling this method multiple times in the order of described ResultSet appearance
     * in the procedure results.
     * 
     * @since 1.1.3
     */
    public synchronized void addResultDescriptor(ColumnDescriptor[] descriptor) {
        if (resultDescriptors == null) {
            resultDescriptors = new ArrayList(2);
        }

        resultDescriptors.add(descriptor);
    }
    
    /**
     * Removes result descriptor from the list of descriptors.
     * 
     * @since 1.1.3
     */
    public void removeResultDescriptor(ColumnDescriptor[] descriptor) {
        if(resultDescriptors != null) {
            resultDescriptors.remove(descriptor);
        }
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

        selectProperties.initWithProperties(properties);
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
        encoder.print("org.objectstyle.cayenne.map.ProcedureQueryBuilder");

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

        if (resultClassName != null) {
            encoder.print("\" result-type=\"");
            encoder.print(resultClassName);
        }

        if (!selecting) {
            encoder.print("\" selecting=\"false");
        }

        encoder.println("\">");

        encoder.indent(1);

        selectProperties.encodeAsXML(encoder);

        encoder.indent(-1);
        encoder.println("</query>");
    }

    /**
     * Creates and returns a new ProcedureQuery built using this query as a prototype and
     * substituting template parameters with the values from the map.
     * 
     * @since 1.1
     */
    public Query createQuery(Map parameters) {
        // create a query replica
        ProcedureQuery query = new ProcedureQuery();

        if (root != null) {
            query.setRoot(root);
        }

        query.setLoggingLevel(logLevel);
        query.setResultClassName(resultClassName);

        selectProperties.copyToProperties(query.selectProperties);
        query.setParameters(parameters);

        // TODO: implement algorithm for building the name based on the original name and
        // the hashcode of the map of parameters. This way query clone can take advantage
        // of caching.
        return query;
    }

    public String getCachePolicy() {
        return selectProperties.getCachePolicy();
    }

    public void setCachePolicy(String policy) {
        this.selectProperties.setCachePolicy(policy);
    }

    public int getFetchLimit() {
        return selectProperties.getFetchLimit();
    }

    public void setFetchLimit(int fetchLimit) {
        this.selectProperties.setFetchLimit(fetchLimit);
    }

    public int getPageSize() {
        return selectProperties.getPageSize();
    }

    public void setPageSize(int pageSize) {
        selectProperties.setPageSize(pageSize);
    }

    public void setFetchingDataRows(boolean flag) {
        selectProperties.setFetchingDataRows(flag);
    }

    public boolean isFetchingDataRows() {
        return selectProperties.isFetchingDataRows();
    }

    public boolean isRefreshingObjects() {
        return selectProperties.isRefreshingObjects();
    }

    public void setRefreshingObjects(boolean flag) {
        selectProperties.setRefreshingObjects(flag);
    }

    public boolean isResolvingInherited() {
        return selectProperties.isResolvingInherited();
    }

    public void setResolvingInherited(boolean b) {
        selectProperties.setResolvingInherited(b);
    }

    /**
     * @deprecated Since 1.1 use {@link #getParameters()}.
     */
    public Map getParams() {
        return getParameters();
    }

    /**
     * @deprecated Since 1.1 use {@link #addParameter(String, Object)}.
     */
    public void addParam(String name, Object value) {
        addParameter(name, value);
    }

    /**
     * @deprecated Since 1.1 use {@link #removeParameter(String)}.
     */
    public void removeParam(String name) {
        removeParameter(name);
    }

    /**
     * @deprecated Since 1.1 use {@link #clearParameters()}.
     */
    public void clearParams() {
        clearParameters();
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
    public Map getParameters() {
        return parameters;
    }

    /**
     * Sets a map of parameters.
     * 
     * @since 1.1
     */
    public synchronized void setParameters(Map parameters) {
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
     * Returns an optional result type of the query.
     * 
     * @since 1.1
     */
    public String getResultClassName() {
        return resultClassName;
    }

    /**
     * Returns Java class of the DataObjects returned by this query. If ClassLoader
     * argument is null, uses Class.forName(..) to discover result class.
     * 
     * @since 1.1
     */
    public Class getResultClass(ClassLoader classLoader) {
        if (this.getResultClassName() == null) {
            return null;
        }

        try {
            // tolerate null class loader
            if (classLoader == null) {
                return Class.forName(this.getResultClassName());
            }
            else {
                return classLoader.loadClass(this.getResultClassName());
            }
        }
        catch (ClassNotFoundException e) {
            throw new CayenneRuntimeException("Failed to load class for name '"
                    + this.getResultClassName()
                    + "': "
                    + e.getMessage(), e);
        }
    }

    /**
     * Sets optional result type of the query. A Class of the result type must be a
     * DataObject implementation mapped in Cayenne.
     * 
     * @since 1.1
     */
    public void setResultClassName(String resultClassName) {
        this.resultClassName = resultClassName;
    }

    /**
     * Returns true if ProcedureQuery is expected to return a ResultSet.
     * 
     * @since 1.1
     */
    public boolean isSelecting() {
        return selecting;
    }

    /**
     * Sets whether ProcedureQuery is expected to return a ResultSet.
     * 
     * @since 1.1
     */
    public void setSelecting(boolean b) {
        selecting = b;
    }
}