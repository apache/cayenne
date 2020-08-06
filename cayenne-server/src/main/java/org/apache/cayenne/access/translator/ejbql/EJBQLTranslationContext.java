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
package org.apache.cayenne.access.translator.ejbql;

import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.ScalarResultSegment;
import org.apache.cayenne.reflect.ClassDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A context used for translating of EJBQL to SQL.
 * 
 * @since 3.0
 */
public class EJBQLTranslationContext {

    private EJBQLCompiledExpression compiledExpression;
    protected Map<String, Object> namedParameters;
    protected Map<Integer, Object> positionalParameters;
    private EJBQLTranslatorFactory translatorFactory;
    private QuotingStrategy quotingStrategy;
    private EntityResolver entityResolver;
    private List<Object> resultSetMetadata;

    private Map<String, String> tableAliases;
    private Map<String, Object> boundParameters;
    private Map<String, Object> attributes;
    private Map<String, String> idAliases;
    private int resultDescriptorPosition;
    private boolean usingAliases;
    private boolean caseInsensitive;
    private List<StringBuilder> bufferStack;
    private List<StringBuilder> bufferChain;
    private StringBuilder stackTop;
    private int subselectCount;
    private QueryMetadata queryMetadata;

    // a flag indicating whether column expressions should be treated as result columns or
    // not.
    private boolean appendingResultColumns;

    public EJBQLTranslationContext(EntityResolver entityResolver, EJBQLQuery query,
            EJBQLCompiledExpression compiledExpression,
            EJBQLTranslatorFactory translatorFactory, QuotingStrategy quotingStrategy) {

        this.entityResolver = entityResolver;
        this.compiledExpression = compiledExpression;
        this.resultSetMetadata = query.getMetaData(entityResolver).getResultSetMapping();

        this.namedParameters = query.getNamedParameters();
        this.positionalParameters = query.getPositionalParameters();
        this.translatorFactory = translatorFactory;
        this.usingAliases = true;
        this.caseInsensitive = false;
        this.queryMetadata = query.getMetaData(entityResolver);
        this.quotingStrategy = quotingStrategy;

        // buffer stack will hold named buffers during translation in the order they were
        // requested
        this.bufferStack = new ArrayList<>();

        // buffer chain will hold named and unnamed buffers in the order they should be
        // concatenated
        this.bufferChain = new ArrayList<>();

        stackTop = new StringBuilder();
        bufferChain.add(stackTop);
        bufferStack.add(stackTop);
    }

    public SQLTemplate getQuery() {

        // concatenate buffers...
        StringBuilder main = bufferChain.get(0);
        for (int i = 1; i < bufferChain.size(); i++) {
            main.append(bufferChain.get(i));
        }

        String sql = main.length() > 0 ? main.toString() : null;
        SQLTemplate query = new SQLTemplate(compiledExpression
                .getRootDescriptor()
                .getObjectClass(), sql);
        query.setParams(boundParameters);
        return query;
    }
    
    public QueryMetadata getMetadata(){
        return queryMetadata;
    }

    private String resolveId(String id) {
        if (idAliases == null) {
            return id;
        }

        String resolvedAlias = idAliases.get(id);
        if (resolvedAlias != null) {
            return resolvedAlias;
        }

        return id;
    }

    EJBQLTranslatorFactory getTranslatorFactory() {
        return translatorFactory;
    }

    EntityResolver getEntityResolver() {
        return entityResolver;
    }

    /**
     * Looks up entity descriptor for an identifier that can be a compiled expression id
     * or one of the aliases.
     */
    public ClassDescriptor getEntityDescriptor(String id) {
        return compiledExpression.getEntityDescriptor(resolveId(id));
    }

    List<DbRelationship> getIncomingRelationships(EJBQLTableId id) {

        List<DbRelationship> incoming = compiledExpression
                .getIncomingRelationships(resolveId(id.getEntityId()));

        // append tail of flattened relationships...
        if (id.getDbPath() != null) {

            DbEntity entity;

            if (incoming == null || incoming.isEmpty()) {
                entity = compiledExpression
                        .getEntityDescriptor(id.getEntityId())
                        .getEntity()
                        .getDbEntity();
            }
            else {
                DbRelationship last = incoming.get(incoming.size() - 1);
                entity = last.getTargetEntity();
            }

            incoming = new ArrayList<>(incoming);

            Iterator<?> it = entity.resolvePathComponents(id.getDbPath());
            while (it.hasNext()) {
                incoming.add((DbRelationship) it.next());
            }
        }

        return incoming;
    }

    /**
     * Creates a previously unused id alias for an entity identified by an id.
     */
    String createIdAlias(String id) {

        if (idAliases == null) {
            idAliases = new HashMap<>();
        }

        for (int i = 0; i < 1000; i++) {
            String alias = id + "_alias" + i;
            if (idAliases.containsKey(alias)) {
                continue;
            }

            if (compiledExpression.getEntityDescriptor(alias) != null) {
                continue;
            }

            idAliases.put(alias, id);
            return alias;
        }

        throw new EJBQLException("Failed to create id alias");
    }

    /**
     * Inserts a marker in the SQL, mapped to a StringBuilder that can be later filled
     * with content.
     */
    void markCurrentPosition(String marker) {

        StringBuilder buffer = findOrCreateMarkedBuffer(marker);
        bufferChain.add(buffer);

        // immediately create unmarked buffer after the marked one and replace the bottom
        // of the stack with it
        StringBuilder tailBuffer = new StringBuilder();
        bufferChain.add(tailBuffer);
        bufferStack.set(0, tailBuffer);
        stackTop = bufferStack.get(bufferStack.size() - 1);
    }

    /**
     * Switches the current buffer to a marked buffer, pushing the currently used buffer
     * on the stack. Note that this can be done even before the marker is inserted in the
     * main buffer. If "reset" is true, any previous contents of the marker are cleared.
     */
    public void pushMarker(String marker, boolean reset) {

        stackTop = findOrCreateMarkedBuffer(marker);
        if (reset) {
            stackTop.delete(0, stackTop.length());
        }

        bufferStack.add(stackTop);
    }

    /**
     * Pops a marker stack, switching to the previously used marker.
     */
    void popMarker() {
        int lastIndex = bufferStack.size() - 1;
        bufferStack.remove(lastIndex);
        stackTop = bufferStack.get(lastIndex - 1);
    }

    StringBuilder findOrCreateMarkedBuffer(String marker) {
        StringBuilder buffer = (StringBuilder) getAttribute(marker);
        if (buffer == null) {
            buffer = new StringBuilder();

            // register mapping of internal to external marker
            setAttribute(marker, buffer);
        }

        return buffer;
    }

    /**
     * Returns a context "attribute" stored for the given name. Attributes is a state
     * preservation mechanism used by translators and have the same scope as the context.
     */
    Object getAttribute(String name) {
        return attributes != null ? attributes.get(name) : null;
    }

    /**
     * Sets a context "attribute". Attributes is a state preservation mechanism used by
     * translators and have the same scope as the context.
     */
    void setAttribute(String var, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        attributes.put(var, value);
    }

    /**
     * Appends a piece of SQL to the internal buffer.
     */
    public EJBQLTranslationContext append(String chunk) {
        stackTop.append(chunk);
        return this;
    }

    /**
     * Appends a piece of SQL to the internal buffer.
     */
    public EJBQLTranslationContext append(char chunk) {
        stackTop.append(chunk);
        return this;
    }

    /**
     * Deletes a specified number of characters from the end of the current buffer.
     */
    EJBQLTranslationContext trim(int n) {
        int len = stackTop.length();

        if (len >= n) {
            stackTop.delete(len - n, len);
        }
        return this;
    }

    EJBQLCompiledExpression getCompiledExpression() {
        return compiledExpression;
    }

    String bindPositionalParameter(int position) {
        return bindParameter(positionalParameters.get(position));
    }

    /**
     * <p>This is used in the processing of parameters into lists for the IN clause and
     * is able to return a list of values that can be used to represent the bound
     * parameter.</p>
     */

    List<String> bindPositionalParameterFlatteningCollection(int position) {
        return bindParameters(positionalParameters.get(position));
    }

    String bindNamedParameter(String name) {
        return bindParameter(namedParameters.get(name));
    }

    /**
     * <p>This is used in the processing of parameters into lists for the IN clause and
     * is able to return a list of values that can be used to represent the bound
     * parameter.</p>
     */

    List<String> bindNamedParameterFlatteningCollection(String name) {
        return bindParameters(namedParameters.get(name));
    }

    /**
     * <p>This method takes a value object which may be a collection or a non-collection.  If it
     * is a collection then it will bind all of the values in the collection.  If it is a non-
     * collection then it will bind that single object.</p>
     * @param value
     * @return
     */

    List<String> bindParameters(Object value) {
        if(Collection.class.isAssignableFrom(value.getClass())) {
            Iterator<?> parameterValueIterator = ((Collection<?>) value).iterator();
            List<String> result = new ArrayList<>();

            while(parameterValueIterator.hasNext()) {
                result.add(bindParameter(parameterValueIterator.next()));
            }

            return result;
        }

        return Collections.singletonList(bindParameter(value));
    }

    /**
     * Creates a new parameter variable, binding provided value to it.
     */
    String bindParameter(Object value) {
        return bindParameter(value, "id");
    }

    void rebindParameter(String boundName, Object newValue) {
        boundParameters.put(boundName, newValue);
    }

    /**
     * Creates a new parameter variable with the specified prefix, binding provided value
     * to it.
     */
    String bindParameter(Object value, String prefix) {
        if (boundParameters == null) {
            boundParameters = new HashMap<>();
        }

        String var = prefix + boundParameters.size();
        boundParameters.put(var, value);
        return var;
    }

    Object getBoundParameter(String name) {
        return boundParameters != null ? boundParameters.get(name) : null;
    }

    /**
     * Retrieves a SQL alias for the combination of EJBQL id variable and a table name. If
     * such alias hasn't been used, it is created on the fly.
     */
    protected String getTableAlias(String idPath, String tableName) {

        if (!isUsingAliases()) {
            return tableName;
        }

        StringBuilder keyBuffer = new StringBuilder();

        // per JPA spec, 4.4.2, "Identification variables are case insensitive.", while
        // relationship path is case-sensitive

        int dot = idPath.indexOf('.');
        if (dot > 0) {
            keyBuffer.append(idPath.substring(0, dot).toLowerCase()).append(
                    idPath.substring(dot));
        }
        else {
            keyBuffer.append(idPath.toLowerCase());
        }

        String key = keyBuffer.append(':').append(tableName).toString();

        String alias;

        if (tableAliases != null) {
            alias = tableAliases.get(key);
        }
        else {
            tableAliases = new HashMap<>();
            alias = null;
        }

        if (alias == null) {
            alias = "t" + tableAliases.size();
            tableAliases.put(key, alias);
        }

        return alias;
    }

    /**
     * Returns a positional EntityResult, incrementing position index on each call.
     */
    EntityResultSegment nextEntityResult() {

        if (resultSetMetadata == null) {
            throw new EJBQLException(
                    "No result set mapping exists for expression, can't map EntityResult");
        }

        return (EntityResultSegment) resultSetMetadata.get(resultDescriptorPosition++);
    }

    /**
     * Returns a positional column alias, incrementing position index on each call.
     */
    String nextColumnAlias() {

        if (resultSetMetadata == null) {
            throw new EJBQLException(
                    "No result set mapping exists for expression, can't map column aliases");
        }

        return ((ScalarResultSegment) resultSetMetadata.get(resultDescriptorPosition++))
                .getColumn();
    }

    public boolean isAppendingResultColumns() {
        return appendingResultColumns;
    }

    void setAppendingResultColumns(boolean appendingResultColumns) {
        this.appendingResultColumns = appendingResultColumns;
    }

    public boolean isUsingAliases() {
        return usingAliases;
    }

    public void setUsingAliases(boolean useAliases) {
        this.usingAliases = useAliases;
    }
    
    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }
    
    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    public QuotingStrategy getQuotingStrategy() {
        return quotingStrategy;
    }

    public void onSubselect() {
        subselectCount++;
    }

    public String makeDistinctMarker() {
        return "DISTINCT_MARKER" + subselectCount;
    }

    String makeWhereMarker() {
        return "WHERE_MARKER" + subselectCount;
    }

    String makeEntityQualifierMarker() {
        return "ENTITY_QUALIIER" + subselectCount;
    }
}
