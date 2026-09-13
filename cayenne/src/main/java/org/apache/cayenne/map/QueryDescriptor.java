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
package org.apache.cayenne.map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic descriptor of a Cayenne query.
 *
 * @since 4.0
 */
public class QueryDescriptor implements ConfigurationNode, XMLSerializable {

    public static final String SELECT_QUERY = "SelectQuery";
    public static final String SQL_TEMPLATE = "SQLTemplate";
    public static final String EJBQL_QUERY = "EJBQLQuery";
    public static final String PROCEDURE_QUERY = "ProcedureQuery";

    /**
     * @since 4.1
     */
    public static final String OBJ_ENTITY_ROOT = "obj-entity";

    /**
     * @since 4.1
     */
    public static final String DB_ENTITY_ROOT = "db-entity";

    /**
     * @since 4.1
     */
    public static final String PROCEDURE_ROOT = "procedure";

    /**
     * @since 4.1
     */
    public static final String DATA_MAP_ROOT = "data-map";

    /**
     * @since 4.1
     */
    public static final String JAVA_CLASS_ROOT = "java-class";

    /**
     * Name of the descriptor property holding the query {@link #getFetchLimit() fetch limit}.
     *
     * @since 5.0
     */
    public static final String FETCH_LIMIT_PROPERTY = "cayenne.GenericSelectQuery.fetchLimit";

    /**
     * Name of the descriptor property holding the query {@link #getFetchOffset() fetch offset}.
     *
     * @since 5.0
     */
    public static final String FETCH_OFFSET_PROPERTY = "cayenne.GenericSelectQuery.fetchOffset";

    /**
     * Name of the descriptor property holding the query {@link #getPageSize() page size}.
     *
     * @since 5.0
     */
    public static final String PAGE_SIZE_PROPERTY = "cayenne.GenericSelectQuery.pageSize";

    /**
     * Name of the descriptor property holding the query {@link #isFetchingDataRows() data rows} flag.
     *
     * @since 5.0
     */
    public static final String FETCHING_DATA_ROWS_PROPERTY = "cayenne.GenericSelectQuery.fetchingDataRows";

    /**
     * Name of the descriptor property holding the query {@link #getCacheStrategy() cache strategy}.
     *
     * @since 5.0
     */
    public static final String CACHE_STRATEGY_PROPERTY = "cayenne.GenericSelectQuery.cacheStrategy";

    /**
     * Name of the descriptor property holding the query {@link #getCacheGroup() cache group}. The plural name is
     * historical: older projects could store a comma-separated list of groups under this property.
     *
     * @since 5.0
     */
    public static final String CACHE_GROUPS_PROPERTY = "cayenne.GenericSelectQuery.cacheGroups";

    /**
     * Name of the descriptor property holding the query {@link #getStatementFetchSize() statement fetch size}.
     *
     * @since 5.0
     */
    public static final String STATEMENT_FETCH_SIZE_PROPERTY = "cayenne.GenericSelectQuery.statementFetchSize";

    /**
     * Creates new SelectQuery query descriptor.
     */
    public static SelectQueryDescriptor selectQueryDescriptor() {
        return new SelectQueryDescriptor();
    }

    /**
     * Creates new SQLTemplate query descriptor.
     */
    public static SQLTemplateDescriptor sqlTemplateDescriptor() {
        return new SQLTemplateDescriptor();
    }

    /**
     * Creates new ProcedureQuery query descriptor.
     */
    public static ProcedureQueryDescriptor procedureQueryDescriptor() {
        return new ProcedureQueryDescriptor();
    }

    /**
     * Creates new EJBQLQuery query descriptor.
     */
    public static EJBQLQueryDescriptor ejbqlQueryDescriptor() {
        return new EJBQLQueryDescriptor();
    }

    /**
     * Creates query descriptor of a given type.
     */
    public static QueryDescriptor descriptor(String type) {
        switch (type) {
            case SELECT_QUERY:
                return selectQueryDescriptor();
            case SQL_TEMPLATE:
                return sqlTemplateDescriptor();
            case EJBQL_QUERY:
                return ejbqlQueryDescriptor();
            case PROCEDURE_QUERY:
                return procedureQueryDescriptor();
            default:
                return new QueryDescriptor(type);
        }
    }

    protected String name;
    protected String type;
    protected DataMap dataMap;
    protected Object root;

    protected Map<String, String> properties = new HashMap<>();

    protected QueryDescriptor(String type) {
        this.type = type;
    }

    /**
     * Returns name of the query.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name of the query.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns type of the query.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets type of the query.
     */
    public void setType(String type) {
        this.type = type;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * Returns the root of this query.
     */
    public Object getRoot() {
        return root;
    }

    /**
     * Sets the root of this query.
     */
    public void setRoot(Object root) {
        this.root = root;
    }

    /**
     * Returns map of query properties set up for this query. Properties are the raw String values stored in the
     * project XML. Typed accessors like {@link #getFetchLimit()} or {@link #getCacheStrategy()} interpret the
     * well-known properties.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Returns query property by its name.
     */
    public String getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Sets map of query properties for this query.
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Sets single query property. A null value removes the property.
     */
    public void setProperty(String name, String value) {
        if (value == null) {
            this.properties.remove(name);
        } else {
            this.properties.put(name, value);
        }
    }

    /**
     * Returns the fetch limit of the query, or {@link QueryMetadata#FETCH_LIMIT_DEFAULT} if it is not set.
     *
     * @since 5.0
     */
    public int getFetchLimit() {
        return intProperty(FETCH_LIMIT_PROPERTY, QueryMetadata.FETCH_LIMIT_DEFAULT);
    }

    /**
     * @since 5.0
     */
    public void setFetchLimit(int fetchLimit) {
        setProperty(FETCH_LIMIT_PROPERTY, String.valueOf(fetchLimit));
    }

    /**
     * Returns the fetch offset of the query, or {@link QueryMetadata#FETCH_OFFSET_DEFAULT} if it is not set.
     *
     * @since 5.0
     */
    public int getFetchOffset() {
        return intProperty(FETCH_OFFSET_PROPERTY, QueryMetadata.FETCH_OFFSET_DEFAULT);
    }

    /**
     * @since 5.0
     */
    public void setFetchOffset(int fetchOffset) {
        setProperty(FETCH_OFFSET_PROPERTY, String.valueOf(fetchOffset));
    }

    /**
     * Returns the page size of the query, or {@link QueryMetadata#PAGE_SIZE_DEFAULT} if it is not set.
     *
     * @since 5.0
     */
    public int getPageSize() {
        return intProperty(PAGE_SIZE_PROPERTY, QueryMetadata.PAGE_SIZE_DEFAULT);
    }

    /**
     * @since 5.0
     */
    public void setPageSize(int pageSize) {
        setProperty(PAGE_SIZE_PROPERTY, String.valueOf(pageSize));
    }

    /**
     * Returns the JDBC statement fetch size of the query, or {@link QueryMetadata#STATEMENT_FETCH_SIZE_DEFAULT} if
     * it is not set.
     *
     * @since 5.0
     */
    public int getStatementFetchSize() {
        return intProperty(STATEMENT_FETCH_SIZE_PROPERTY, QueryMetadata.STATEMENT_FETCH_SIZE_DEFAULT);
    }

    /**
     * @since 5.0
     */
    public void setStatementFetchSize(int statementFetchSize) {
        setProperty(STATEMENT_FETCH_SIZE_PROPERTY, String.valueOf(statementFetchSize));
    }

    /**
     * Returns whether the query fetches DataRows instead of Persistent objects. Defaults to
     * {@link QueryMetadata#FETCHING_DATA_ROWS_DEFAULT} if the property is not set.
     *
     * @since 5.0
     */
    public boolean isFetchingDataRows() {
        String value = getProperty(FETCHING_DATA_ROWS_PROPERTY);
        return value != null ? Boolean.parseBoolean(value) : QueryMetadata.FETCHING_DATA_ROWS_DEFAULT;
    }

    /**
     * @since 5.0
     */
    public void setFetchingDataRows(boolean fetchingDataRows) {
        setProperty(FETCHING_DATA_ROWS_PROPERTY, String.valueOf(fetchingDataRows));
    }

    /**
     * Returns the cache strategy of the query. Defaults to {@link QueryCacheStrategy#getDefaultStrategy()} if the
     * property is not set or holds an unknown value.
     *
     * @since 5.0
     */
    public QueryCacheStrategy getCacheStrategy() {
        String value = getProperty(CACHE_STRATEGY_PROPERTY);
        return value != null ? QueryCacheStrategy.safeValueOf(value) : QueryCacheStrategy.getDefaultStrategy();
    }

    /**
     * @since 5.0
     */
    public void setCacheStrategy(QueryCacheStrategy cacheStrategy) {
        setProperty(CACHE_STRATEGY_PROPERTY, cacheStrategy != null ? cacheStrategy.name() : null);
    }

    /**
     * Returns the cache group of the query, or null if none is set. If the underlying property holds a legacy
     * comma-separated list of groups, only the first non-empty group is returned.
     *
     * @since 5.0
     */
    public String getCacheGroup() {
        String value = getProperty(CACHE_GROUPS_PROPERTY);
        if (value == null) {
            return null;
        }

        for (String group : value.split(",")) {
            if (!group.isEmpty()) {
                return group;
            }
        }

        return null;
    }

    /**
     * @since 5.0
     */
    public void setCacheGroup(String cacheGroup) {
        setProperty(CACHE_GROUPS_PROPERTY, cacheGroup);
    }

    private int intProperty(String name, int defaultValue) {
        String value = getProperty(name);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    /**
     * Assembles Cayenne query instance of appropriate type from this descriptor.
     */
    public Query buildQuery() {
        throw new CayenneRuntimeException("Unable to build query object of this type.");
    }

    /**
     * Assembles Cayenne query instance of appropriate type from this descriptor, applying a map of named
     * parameters to it. Subclasses that support parameters must override this method. The default
     * implementation ignores the parameters.
     *
     * @since 5.0
     */
    public Query buildQuery(Map<String, ?> parameters) {
        return buildQuery();
    }

    @Override
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("query").attribute("name", getName()).attribute("type", type);

        String rootString = null;
        String rootType = null;

        if (root instanceof String) {
            rootType = OBJ_ENTITY_ROOT;
            rootString = root.toString();
        } else if (root instanceof ObjEntity) {
            rootType = OBJ_ENTITY_ROOT;
            rootString = ((ObjEntity) root).getName();
        } else if (root instanceof DbEntity) {
            rootType = DB_ENTITY_ROOT;
            rootString = ((DbEntity) root).getName();
        } else if (root instanceof Procedure) {
            rootType = PROCEDURE_ROOT;
            rootString = ((Procedure) root).getName();
        } else if (root instanceof Class<?>) {
            rootType = JAVA_CLASS_ROOT;
            rootString = ((Class<?>) root).getName();
        }

        if (rootType != null) {
            encoder.attribute("root", rootType).attribute("root-name", rootString);
        }
        encodeProperties(encoder);

        delegate.visitQuery(this);
        encoder.end();
    }

    void encodeProperties(XMLEncoder encoder) {
        for (Map.Entry<String, String> property : properties.entrySet()) {
            String value = property.getValue();
            if(value == null || value.isEmpty()) {
                continue;
            }
            encoder.property(property.getKey(), value);
        }
    }
}
