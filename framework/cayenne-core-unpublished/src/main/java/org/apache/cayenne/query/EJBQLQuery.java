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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * An EJBQL query representation in Cayenne.
 * 
 * @since 3.0
 */
public class EJBQLQuery implements Query, XMLSerializable {

    protected String name;
    protected DataMap dataMap;
    protected String ejbqlStatement;
    
    protected Map<String, Object> namedParameters;
    protected Map<Integer, Object> positionalParameters;

    protected transient EJBQLCompiledExpression expression;
    EJBQLQueryMetadata metadata = new EJBQLQueryMetadata();

    public EJBQLQuery(String ejbqlStatement) {
        this.ejbqlStatement = ejbqlStatement;
    }

    public EJBQLQuery() {
    }
    
    /**
     * @since 3.1
     */
    public DataMap getDataMap() {
        return dataMap;
    }
    
    
    /**
     * @since 3.1
     */
    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }
    
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    public void initWithProperties(Map<String, ?> properties) {

        // must init defaults even if properties are empty
        if (properties == null) {
            properties = Collections.EMPTY_MAP;
        }
        metadata.initWithProperties(properties);
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {
        metadata.resolve(resolver, this);
        return metadata;
    }

    public boolean isFetchingDataRows() {
        return metadata.isFetchingDataRows();
    }

    public void setFetchingDataRows(boolean flag) {
        metadata.setFetchingDataRows(flag);
    }

    public String[] getCacheGroups() {
        return metadata.getCacheGroups();
    }

    public QueryCacheStrategy getCacheStrategy() {
        return metadata.getCacheStrategy();
    }

    public void setCacheGroups(String... cacheGroups) {
        this.metadata.setCacheGroups(cacheGroups);
    }

    public void setCacheStrategy(QueryCacheStrategy strategy) {
        metadata.setCacheStrategy(strategy);
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

    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        DataMap map = getMetaData(resolver).getDataMap();

        if (map == null) {
            throw new CayenneRuntimeException("No DataMap found, can't route query "
                    + this);
        }

        router.route(router.engineForDataMap(map), this, substitutedQuery);
    }

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.ejbqlAction(this);
    }

    /**
     * Returns an unparsed EJB QL statement used to initialize this query.
     */
    public String getEjbqlStatement() {
        return ejbqlStatement;
    }

    /**
     * Returns lazily initialized EJBQLCompiledExpression for this query EJBQL.
     */
    public EJBQLCompiledExpression getExpression(EntityResolver resolver)
            throws EJBQLException {
        if (expression == null) {
            this.expression = EJBQLParserFactory.getParser().compile(
                    ejbqlStatement, 
                    resolver);
        }

        return expression;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns unmodifiable map of combined named and positional parameters. Positional
     * parameter keys are Integers, while named parameter keys are strings.
     */

    public Map<String, Object> getNamedParameters() {
        return namedParameters != null
                ? Collections.unmodifiableMap(namedParameters)
                : Collections.EMPTY_MAP;
    }

    public Map<Integer, Object> getPositionalParameters() {
        return positionalParameters != null ? Collections
                .unmodifiableMap(positionalParameters) : Collections.EMPTY_MAP;
    }

    /**
     * Sets a named query parameter value.
     */
    public void setParameter(String name, Object object) {

        // do a minimal sanity check
        if (name == null || name.length() < 1) {
            throw new IllegalArgumentException("Null or empty parameter name");
        }

        // TODO: andrus, 6/12/2007 - validate against available query parameters - JPA
        // spec requires it.

        if (namedParameters == null) {
            namedParameters = new HashMap<String, Object>();
        }

        namedParameters.put(name, object);
    }

    /**
     * Sets a positional query parameter value. Note that parameter indexes are starting
     * from 1.
     */
    public void setParameter(int position, Object object) {

        if (position < 1) {
            throw new IllegalArgumentException("Parameter position must be >= 1: "
                    + position);
        }

        // TODO: andrus, 6/12/2007 - validate against available query parameters - JPA
        // spec requires it.

        if (positionalParameters == null) {
            positionalParameters = new HashMap<Integer, Object>();
        }

        positionalParameters.put(Integer.valueOf(position), object);
    }

    /**
     * Returns the fetchLimit property indicating the maximum number of rows this query
     * would return.
     */
    public int getFetchLimit() {
        return metadata.getFetchLimit();
    }

    /**
     * Sets the fetchLimit property indicating the maximum number of rows this query would
     * return.
     */
    public void setFetchLimit(int fetchLimit) {
        metadata.setFetchLimit(fetchLimit);
    }

    public int getFetchOffset() {
        return metadata.getFetchOffset();
    }

    public void setFetchOffset(int fetchOffset) {
        metadata.setFetchOffset(fetchOffset);
    }

    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<query name=\"");
        encoder.print(getName());
        encoder.print("\" factory=\"");
        encoder.print("org.apache.cayenne.map.EjbqlBuilder");

        encoder.println("\">");

        encoder.indent(1);

        metadata.encodeAsXML(encoder);

        if (ejbqlStatement != null) {
            encoder.print("<ejbql><![CDATA[");
            encoder.print(ejbqlStatement);
            encoder.println("]]></ejbql>");
        }

        encoder.indent(-1);
        encoder.println("</query>");
    }

    public void setEjbqlStatement(String text) {
        this.ejbqlStatement = text;
    }

    public int getPageSize() {
        return metadata.getPageSize();
    }

    public void setPageSize(int pageSize) {
        metadata.setPageSize(pageSize);
    }
    
    /**
     * Sets statement's fetch size (0 for no default size)
     * @since 3.0 
     */
    public void setStatementFetchSize(int size) {
        metadata.setStatementFetchSize(size);
    }
    
    /**
     * @return statement's fetch size
     * @since 3.0
     */
    public int getStatementFetchSize() {
        return metadata.getStatementFetchSize();
    }
    
    
}
