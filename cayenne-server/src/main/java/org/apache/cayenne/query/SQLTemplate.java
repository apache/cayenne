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
import java.util.TreeSet;

import org.apache.cayenne.access.QueryEngine;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;

/**
 * A query that executes unchanged (except for template preprocessing) "raw" SQL specified
 * by the user. <h3>Template Script</h3>
 * <p>
 * SQLTemplate stores a dynamic template for the SQL query that supports parameters and
 * customization using Velocity scripting language. The most straightforward use of
 * scripting abilities is to build parameterized queries. For example:
 * </p>
 * 
 * <pre>
 *                  SELECT ID, NAME FROM SOME_TABLE WHERE NAME LIKE $a
 * </pre>
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
 */
public class SQLTemplate extends AbstractQuery implements ParameterizedQuery,
        XMLSerializable {

    static final String COLUMN_NAME_CAPITALIZATION_PROPERTY = "cayenne.SQLTemplate.columnNameCapitalization";

    private static final Transformer nullMapTransformer = new Transformer() {

        public Object transform(Object input) {
            return (input != null) ? input : Collections.EMPTY_MAP;
        }
    };

    protected String defaultTemplate;
    protected Map<String, String> templates;
    protected Map<String, ?>[] parameters;
    protected CapsStrategy columnNamesCapitalization;
    protected SQLResult result;
    private String dataNodeName;

    SQLTemplateMetadata metaData = new SQLTemplateMetadata();

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
     * Creates a SQLTemplate without an explicit root.
     * 
     * @since 3.2
     */
    public SQLTemplate(String defaultTemplate, boolean isFetchingDataRows) {
        setDefaultTemplate(defaultTemplate);
        setRoot(null);
        setFetchingDataRows(isFetchingDataRows);
    }
    
    @Override
    public void setRoot(Object value) {
        // allow null root...
        if (value == null) {
            this.root = value;
        } else {
            super.setRoot(value);
        }
    }
    
    @Override
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        DataMap map = getMetaData(resolver).getDataMap();

        QueryEngine engine;
        if (map != null) {
            engine = router.engineForDataMap(map);
        } else {
            engine = router.engineForName(getDataNodeName());
        }

        router.route(engine, this, substitutedQuery);
    }

    /**
     * @since 3.1
     */
    public SQLTemplate(DataMap rootMap, String defaultTemplate, boolean isFetchingDataRows) {
        setDefaultTemplate(defaultTemplate);
        setRoot(rootMap);
        setFetchingDataRows(isFetchingDataRows);
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
    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {
        metaData.resolve(root, resolver, this);
        return metaData;
    }

    /**
     * Calls <em>sqlAction(this)</em> on the visitor.
     * 
     * @since 1.2
     */
    @Override
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
            rootType = MapLoader.OBJ_ENTITY_ROOT;
            rootString = root.toString();
        }
        else if (root instanceof ObjEntity) {
            rootType = MapLoader.OBJ_ENTITY_ROOT;
            rootString = ((ObjEntity) root).getName();
        }
        else if (root instanceof DbEntity) {
            rootType = MapLoader.DB_ENTITY_ROOT;
            rootString = ((DbEntity) root).getName();
        }
        else if (root instanceof Procedure) {
            rootType = MapLoader.PROCEDURE_ROOT;
            rootString = ((Procedure) root).getName();
        }
        else if (root instanceof Class<?>) {
            rootType = MapLoader.JAVA_CLASS_ROOT;
            rootString = ((Class<?>) root).getName();
        }
        else if (root instanceof DataMap) {
            rootType = MapLoader.DATA_MAP_ROOT;
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

        metaData.encodeAsXML(encoder);

        if (getColumnNamesCapitalization() != CapsStrategy.DEFAULT) {
            encoder.printProperty(
                    COLUMN_NAME_CAPITALIZATION_PROPERTY,
                    getColumnNamesCapitalization().name());
        }

        // encode default SQL
        if (defaultTemplate != null) {
            encoder.print("<sql><![CDATA[");
            encoder.print(defaultTemplate);
            encoder.println("]]></sql>");
        }

        // encode adapter SQL
        if (templates != null && !templates.isEmpty()) {
            
            //sorting entries by adapter name
            TreeSet<String> keys = new TreeSet<String>(templates.keySet());
            for (String key : keys) {
                String value = templates.get(key);

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
        metaData.initWithProperties(properties);

        if (properties == null) {
            properties = Collections.EMPTY_MAP;
        }

        Object columnNamesCapitalization = properties
                .get(COLUMN_NAME_CAPITALIZATION_PROPERTY);
        this.columnNamesCapitalization = (columnNamesCapitalization != null)
                ? CapsStrategy
                        .valueOf(columnNamesCapitalization.toString().toUpperCase())
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
    public SQLTemplate queryWithParameters(Map<String, ?>... parameters) {
        // create a query replica
        SQLTemplate query = new SQLTemplate();

        query.setRoot(root);
        query.setDefaultTemplate(getDefaultTemplate());

        if (templates != null) {
            query.templates = new HashMap<String, String>(templates);
        }

        query.metaData.copyFromInfo(this.metaData);
        query.setParameters(parameters);
        query.setColumnNamesCapitalization(this.getColumnNamesCapitalization());

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
        Map<String, ?> map = (parameters != null && parameters.length > 0)
                ? parameters[0]
                : null;
        return (map != null) ? map : Collections.EMPTY_MAP;
    }

    /**
     * Utility method to initialize query with one or more sets of parameters.
     */
    public void setParameters(Map<String, ?>... parameters) {

        if (parameters == null) {
            this.parameters = null;
        }
        else {
            // clone parameters to ensure that we don't have immutable maps that are not
            // serializable with Hessian...
            this.parameters = new Map[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                this.parameters[i] = parameters[i] != null ? new HashMap<String, Object>(
                        parameters[i]) : new HashMap<String, Object>();
            }
        }
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
     * Returns a column name capitalization policy applied to selecting queries. This is
     * used to simplify mapping of the queries like "SELECT * FROM ...", ensuring that a
     * chosen Cayenne column mapping strategy (e.g. all column names in uppercase) is
     * portable across database engines that can have varying default capitalization.
     * Default (null) value indicates that column names provided in result set are used
     * unchanged.
     * 
     * @since 3.0
     */
    public CapsStrategy getColumnNamesCapitalization() {
        return columnNamesCapitalization != null
                ? columnNamesCapitalization
                : CapsStrategy.DEFAULT;
    }

    /**
     * Sets a column name capitalization policy applied to selecting queries. This is used
     * to simplify mapping of the queries like "SELECT * FROM ...", ensuring that a chosen
     * Cayenne column mapping strategy (e.g. all column names in uppercase) is portable
     * across database engines that can have varying default capitalization. Default
     * (null) value indicates that column names provided in result set are used unchanged.
     * <p/>
     * Note that while a non-default setting is useful for queries that do not rely on a
     * #result directive to describe columns, it works for all SQLTemplates the same way.
     * 
     * @since 3.0
     */
    public void setColumnNamesCapitalization(CapsStrategy columnNameCapitalization) {
        this.columnNamesCapitalization = columnNameCapitalization;
    }

    /**
     * Sets an optional explicit mapping of the result set. If result set mapping is
     * specified, the result of SQLTemplate may not be a normal list of Persistent objects
     * or DataRows, instead it will follow the {@link SQLResult} rules.
     * 
     * @since 3.0
     */
    public void setResult(SQLResult resultSet) {
        this.result = resultSet;
    }

    /**
     * @since 3.0
     */
    public SQLResult getResult() {
        return result;
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
     * Returns a name of the DataNode to use with this SQLTemplate. This
     * information will be used during query execution if no other routing
     * information is provided such as entity name or class, etc.
     * 
     * @since 3.2
     */
    public String getDataNodeName() {
        return dataNodeName;
    }

    /**
     * Sets a name of the DataNode to use with this SQLTemplate. This
     * information will be used during query execution if no other routing
     * information is provided such as entity name or class, etc.
     * 
     * @since 3.2
     */
    public void setDataNodeName(String dataNodeName) {
        this.dataNodeName = dataNodeName;
    }
}
