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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.QueryBuilder;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;

/**
 * A query that executes unchanged (except for template preprocessing) "raw" SQL specified
 * by the user.
 * <h3>Template Script</h3>
 * <p>
 * SQLTemplate stores a dynamic template for the SQL query that supports parameters and
 * customization using Velocity scripting language. The most straightforward use of
 * scripting abilities is to build parameterized queries. For example:
 * </p>
 * 
 * <pre>
 *                  SELECT ID, NAME FROM SOME_TABLE WHERE NAME LIKE $a
 * </pre>
 * 
 * <p>
 * <i>For advanced scripting options see "Scripting SQLTemplate" chapter in the User
 * Guide. </i>
 * </p>
 * <h3>Per-Database Template Customization</h3>
 * <p>
 * SQLTemplate has a {@link #getDefaultTemplate() default template script}, but also it
 * allows to configure multiple templates and switch them dynamically. This way a single
 * query can have multiple "dialects" specific to a given database.
 * </p>
 * <h3>Parameter Sets</h3>
 * <p>
 * SQLTemplate supports multiple sets of parameters, so a single query can be executed
 * multiple times with different parameters. "Scrolling" through parameter list is done by
 * calling {@link #parametersIterator()}. This iterator goes over parameter sets,
 * returning a Map on each call to "next()"
 * </p>
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public class SQLTemplate extends AbstractQuery implements ParameterizedQuery,
        XMLSerializable {

    static final String COLUMN_NAME_CAPITALIZATION_PROPERTY = "cayenne.SQLTemplate.columnNameCapitalization";

    /**
     * @since 3.0
     */
    public static final String UPPERCASE_COLUMN_NAMES = "upper";

    /**
     * @since 3.0
     */
    public static final String LOWERCASE_COLUMN_NAMES = "lower";

    private static final Transformer nullMapTransformer = new Transformer() {

        public Object transform(Object input) {
            return (input != null) ? input : Collections.EMPTY_MAP;
        }
    };

    protected String defaultTemplate;
    protected Map<String, String> templates;
    protected Map<String, ?>[] parameters;
    protected String columnNamesCapitalization;

    SQLTemplateMetadata selectInfo = new SQLTemplateMetadata();

    /**
     * Creates an empty SQLTemplate. Note this constructor does not specify the "root" of
     * the query, so a user must call "setRoot" later to make sure SQLTemplate can be
     * executed.
     * 
     * @since 1.2
     */
    public SQLTemplate() {
    }

    /**
     * @since 1.2
     */
    public SQLTemplate(DataMap rootMap, String defaultTemplate) {
        setDefaultTemplate(defaultTemplate);
        setRoot(rootMap);
    }

    /**
     * @since 1.2
     */
    public SQLTemplate(ObjEntity rootEntity, String defaultTemplate) {
        setDefaultTemplate(defaultTemplate);
        setRoot(rootEntity);
    }

    /**
     * @since 1.2
     */
    public SQLTemplate(Class<?> rootClass, String defaultTemplate) {
        setDefaultTemplate(defaultTemplate);
        setRoot(rootClass);
    }

    /**
     * @since 1.2
     */
    public SQLTemplate(DbEntity rootEntity, String defaultTemplate) {
        setDefaultTemplate(defaultTemplate);
        setRoot(rootEntity);
    }

    /**
     * @since 1.2
     */
    public SQLTemplate(String objEntityName, String defaultTemplate) {
        setRoot(objEntityName);
        setDefaultTemplate(defaultTemplate);
    }

    /**
     * @since 1.2
     */
    public QueryMetadata getMetaData(EntityResolver resolver) {
        selectInfo.resolve(root, resolver, this);
        return selectInfo;
    }

    /**
     * Calls <em>sqlAction(this)</em> on the visitor.
     * 
     * @since 1.2
     */
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.sqlAction(this);
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
        encoder.print("org.apache.cayenne.map.SQLTemplateBuilder");

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
            rootString = ((Class<?>) root).getName();
        }
        else if (root instanceof DataMap) {
            rootType = QueryBuilder.DATA_MAP_ROOT;
            rootString = ((DataMap) root).getName();
        }

        if (rootType != null) {
            encoder.print("\" root=\"");
            encoder.print(rootType);
            encoder.print("\" root-name=\"");
            encoder.print(rootString);
        }

        encoder.println("\">");

        encoder.indent(1);

        selectInfo.encodeAsXML(encoder);

        if (getColumnNamesCapitalization() != null) {
            encoder.printProperty(
                    COLUMN_NAME_CAPITALIZATION_PROPERTY,
                    getColumnNamesCapitalization());
        }

        // encode default SQL
        if (defaultTemplate != null) {
            encoder.print("<sql><![CDATA[");
            encoder.print(defaultTemplate);
            encoder.println("]]></sql>");
        }

        // encode adapter SQL
        if (templates != null && !templates.isEmpty()) {
            for (Map.Entry<String, String> entry : templates.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key != null && value != null) {
                    String sql = value.trim();
                    if (sql.length() > 0) {
                        encoder.print("<sql adapter-class=\"");
                        encoder.print(key);
                        encoder.print("\"><![CDATA[");
                        encoder.print(sql);
                        encoder.println("]]></sql>");
                    }
                }
            }
        }

        // TODO: support parameter encoding

        encoder.indent(-1);
        encoder.println("</query>");
    }

    /**
     * Initializes query parameters using a set of properties.
     * 
     * @since 1.1
     */
    public void initWithProperties(Map<String, ?> properties) {
        // must init defaults even if properties are empty
        selectInfo.initWithProperties(properties);

        if (properties == null) {
            properties = Collections.EMPTY_MAP;
        }

        Object columnNamesCapitalization = properties
                .get(COLUMN_NAME_CAPITALIZATION_PROPERTY);
        this.columnNamesCapitalization = (columnNamesCapitalization != null)
                ? columnNamesCapitalization.toString()
                : null;
    }

    /**
     * Returns an iterator over parameter sets. Each element returned from the iterator is
     * a java.util.Map.
     */
    public Iterator<?> parametersIterator() {
        return (parameters == null || parameters.length == 0) ? IteratorUtils
                .emptyIterator() : IteratorUtils.transformedIterator(IteratorUtils
                .arrayIterator(parameters), nullMapTransformer);
    }

    /**
     * Returns the number of parameter sets.
     */
    public int parametersSize() {
        return (parameters != null) ? parameters.length : 0;
    }

    /**
     * Returns a new query built using this query as a prototype and a new set of
     * parameters.
     */
    public SQLTemplate queryWithParameters(Map<String, ?> parameters) {
        return queryWithParameters(new Map[] {
            parameters
        });
    }

    /**
     * Returns a new query built using this query as a prototype and a new set of
     * parameters.
     */
    public SQLTemplate queryWithParameters(Map<String, ?>[] parameters) {
        // create a query replica
        SQLTemplate query = new SQLTemplate();

        query.setRoot(root);
        query.setDefaultTemplate(getDefaultTemplate());

        if (templates != null) {
            query.templates = new HashMap<String, String>(templates);
        }

        query.selectInfo.copyFromInfo(this.selectInfo);
        query.setParameters(parameters);

        // The following algorithm is for building the new query name based
        // on the original query name and a hashcode of the map of parameters.
        // This way the query clone can take advantage of caching. Fixes
        // problem reported in CAY-360.

        if (!Util.isEmptyString(name)) {
            StringBuilder buffer = new StringBuilder(name);

            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    if (!parameters[i].isEmpty()) {
                        buffer.append(parameters[i].hashCode());
                    }
                }
            }

            query.setName(buffer.toString());
        }

        return query;
    }

    /**
     * Creates and returns a new SQLTemplate built using this query as a prototype and
     * substituting template parameters with the values from the map.
     * 
     * @since 1.1
     */
    public Query createQuery(Map<String, ?> parameters) {
        return queryWithParameters(parameters);
    }

    public String getCachePolicy() {
        return selectInfo.getCachePolicy();
    }

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

    public int getFetchLimit() {
        return selectInfo.getFetchLimit();
    }

    public void setFetchLimit(int fetchLimit) {
        this.selectInfo.setFetchLimit(fetchLimit);
    }

    public int getPageSize() {
        return selectInfo.getPageSize();
    }

    public void setPageSize(int pageSize) {
        selectInfo.setPageSize(pageSize);
    }

    public void setFetchingDataRows(boolean flag) {
        selectInfo.setFetchingDataRows(flag);
    }

    public boolean isFetchingDataRows() {
        return selectInfo.isFetchingDataRows();
    }

    public boolean isRefreshingObjects() {
        return selectInfo.isRefreshingObjects();
    }

    public void setRefreshingObjects(boolean flag) {
        selectInfo.setRefreshingObjects(flag);
    }

    public boolean isResolvingInherited() {
        return selectInfo.isResolvingInherited();
    }

    public void setResolvingInherited(boolean b) {
        selectInfo.setResolvingInherited(b);
    }

    /**
     * Returns default SQL template for this query.
     */
    public String getDefaultTemplate() {
        return defaultTemplate;
    }

    /**
     * Sets default SQL template for this query.
     */
    public void setDefaultTemplate(String string) {
        defaultTemplate = string;
    }

    /**
     * Returns a template for key, or a default template if a template for key is not
     * found.
     */
    public synchronized String getTemplate(String key) {
        if (templates == null) {
            return defaultTemplate;
        }

        String template = templates.get(key);
        return (template != null) ? template : defaultTemplate;
    }

    /**
     * Returns template for key, or null if there is no template configured for this key.
     * Unlike {@link #getTemplate(String)}this method does not return a default template
     * as a failover strategy, rather it returns null.
     */
    public synchronized String getCustomTemplate(String key) {
        return (templates != null) ? templates.get(key) : null;
    }

    /**
     * Adds a SQL template string for a given key. Note the the keys understood by Cayenne
     * must be fully qualified adapter class names. This way the framework can related
     * current DataNode to the right template. E.g.
     * "org.apache.cayenne.dba.oracle.OracleAdapter" is a key that should be used to setup
     * an Oracle-specific template.
     * 
     * @see #setDefaultTemplate(String)
     */
    public synchronized void setTemplate(String key, String template) {
        if (templates == null) {
            templates = new HashMap<String, String>();
        }

        templates.put(key, template);
    }

    public synchronized void removeTemplate(String key) {
        if (templates != null) {
            templates.remove(key);
        }
    }

    /**
     * Returns a collection of configured template keys.
     */
    public synchronized Collection<String> getTemplateKeys() {
        return (templates != null) ? Collections.unmodifiableCollection(templates
                .keySet()) : Collections.EMPTY_LIST;
    }

    /**
     * Utility method to get the first set of parameters, since most queries will only
     * have one.
     */
    public Map<String, ?> getParameters() {
        Map<String, ?> map = (parameters != null && parameters.length > 0) ? parameters[0] : null;
        return (map != null) ? map : Collections.EMPTY_MAP;
    }

    /**
     * Utility method to initialize query with only a single set of parameters. Useful,
     * since most queries will only have one set. Internally calls
     * {@link #setParameters(Map[])}.
     */
    public void setParameters(Map<String, ?> map) {
        setParameters(map != null ? new Map[] {
            map
        } : null);
    }

    public void setParameters(Map<String, ?>[] parameters) {

        if (parameters == null) {
            this.parameters = null;
        }
        else {
            // clone parameters to ensure that we don't have immutable maps that are not
            // serializable with Hessian...
            this.parameters = new Map[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                this.parameters[i] = parameters[i] != null
                        ? new HashMap<String, Object>(parameters[i])
                        : new HashMap<String, Object>();
            }
        }
    }

    /**
     * @since 1.2
     */
    public PrefetchTreeNode getPrefetchTree() {
        return selectInfo.getPrefetchTree();
    }

    /**
     * Adds a prefetch.
     * 
     * @since 1.2
     */
    public PrefetchTreeNode addPrefetch(String prefetchPath) {
        // by default use JOINT_PREFETCH_SEMANTICS
        return selectInfo.addPrefetch(
                prefetchPath,
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
    }

    /**
     * @since 1.2
     */
    public void removePrefetch(String prefetch) {
        selectInfo.removePrefetch(prefetch);
    }

    /**
     * Adds all prefetches from a provided collection.
     * 
     * @since 1.2
     */
    public void addPrefetches(Collection<String> prefetches) {
        selectInfo.addPrefetches(prefetches, PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
    }

    /**
     * Clears all prefetches.
     * 
     * @since 1.2
     */
    public void clearPrefetches() {
        selectInfo.clearPrefetches();
    }

    /**
     * Returns a column name capitalization policy applied to selecting queries. This is
     * used to simplify mapping of the queries like "SELECT * FROM ...", ensuring that a
     * chosen Cayenne column mapping strategy (e.g. all column names in uppercase) is
     * portable across database engines that can have varying default capitalization.
     * Default (null) value indicates that column names provided in result set are used
     * unchanged.
     * 
     * @since 3.0
     */
    public String getColumnNamesCapitalization() {
        return columnNamesCapitalization;
    }

    /**
     * Sets a column name capitalization policy applied to selecting queries. This is used
     * to simplify mapping of the queries like "SELECT * FROM ...", ensuring that a chosen
     * Cayenne column mapping strategy (e.g. all column names in uppercase) is portable
     * across database engines that can have varying default capitalization. Default
     * (null) value indicates that column names provided in result set are used unchanged.
     * <p/> Note that while a non-default setting is useful for queries that do not rely
     * on a #result directive to describe columns, it works for all SQLTemplates the same
     * way.
     * 
     * @param columnNameCapitalization Can be null of one of
     *            {@link #LOWERCASE_COLUMN_NAMES} or {@link #UPPERCASE_COLUMN_NAMES}.
     * @since 3.0
     */
    public void setColumnNamesCapitalization(String columnNameCapitalization) {
        this.columnNamesCapitalization = columnNameCapitalization;
    }

    /**
     * Sets an optional explicit mapping of the result set. If result set mapping is
     * specified, the result of SQLTemplate may not be a normal list of Persistent objects
     * or DataRows, instead it will follow the {@link SQLResultSetMapping} rules.
     * 
     * @since 3.0
     */
    public void setResultSetMapping(SQLResultSetMapping resultSetMapping) {
        selectInfo.setResultSetMapping(resultSetMapping);
    }
}
