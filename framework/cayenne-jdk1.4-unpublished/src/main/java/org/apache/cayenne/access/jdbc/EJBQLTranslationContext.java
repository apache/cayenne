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
package org.apache.cayenne.access.jdbc;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.SQLResultSetMapping;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A context used for translating of EJBQL to SQL.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class EJBQLTranslationContext {

    private Map tableAliases;
    private Map boundParameters;
    private StringBuffer mainBuffer;
    private StringBuffer currentBuffer;
    private EJBQLCompiledExpression compiledExpression;
    private Map attributes;
    private Map reusableJoins;
    private Map parameters;
    private Map idAliases;
    private int columnAliasPosition;
    private EJBQLTranslatorFactory translatorFactory;

    // a flag indicating whether column expressions should be treated as result columns or
    // not.
    private boolean appendingResultColumns;

    public EJBQLTranslationContext(EJBQLCompiledExpression compiledExpression, Map parameters,
            EJBQLTranslatorFactory translatorFactory) {
        this.compiledExpression = compiledExpression;
        this.mainBuffer = new StringBuffer();
        this.currentBuffer = mainBuffer;
        this.parameters = parameters;
        this.translatorFactory = translatorFactory;
    }

    SQLTemplate getQuery() {
        String sql = mainBuffer.length() > 0 ? mainBuffer.toString() : null;
        SQLTemplate query = new SQLTemplate(compiledExpression
                .getRootDescriptor()
                .getObjectClass(), sql);
        query.setParameters(boundParameters);
        return query;
    }

    private String resolveId(String id) {
        if (idAliases == null) {
            return id;
        }

        String resolvedAlias = (String) idAliases.get(id);
        if (resolvedAlias != null) {
            return resolvedAlias;
        }

        return id;
    }

    EJBQLTranslatorFactory getTranslatorFactory() {
        return translatorFactory;
    }

    /**
     * Looks up entity descriptor for an identifier that can be a compiled expression id
     * or one of the aliases.
     */
    ClassDescriptor getEntityDescriptor(String id) {
        return compiledExpression.getEntityDescriptor(resolveId(id));
    }

    ObjRelationship getIncomingRelationship(String id) {
        return compiledExpression.getIncomingRelationship(resolveId(id));
    }

    /**
     * Creates a previously unused id alias for an entity identified by an id.
     */
    String createIdAlias(String id) {

        if (idAliases == null) {
            idAliases = new HashMap();
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
     * Inserts a marker in the SQL, mapped to a StringBuffer that can be later filled with
     * content.
     */
    void markCurrentPosition(String marker) {
        // ensure buffer is created for the marker
        findOrCreateMarkedBuffer(marker);

        String internalMarker = (String) getAttribute(marker);

        // make sure we mark the main buffer
        StringBuffer current = this.currentBuffer;

        try {
            switchToMainBuffer();
            append("${").append(internalMarker).append("}");
        }
        finally {
            this.currentBuffer = current;
        }
    }

    /**
     * Switches the current buffer to a marked buffer. Note that this can be done even
     * before the marker is inserted in the main buffer. If "reset" is true, any previous
     * contents of the marker are cleared.
     */
    void switchToMarker(String marker, boolean reset) {
        this.currentBuffer = findOrCreateMarkedBuffer(marker);
        if (reset) {
            this.currentBuffer.delete(0, this.currentBuffer.length());
        }
    }

    void switchToMainBuffer() {
        this.currentBuffer = this.mainBuffer;
    }

    private StringBuffer findOrCreateMarkedBuffer(String marker) {
        StringBuffer buffer;

        String internalMarker = (String) getAttribute(marker);
        if (internalMarker == null) {
            buffer = new StringBuffer();
            internalMarker = bindParameter(buffer, "marker");

            // register mapping of internal to external marker
            setAttribute(marker, internalMarker);
        }
        else {
            Object object = boundParameters.get(internalMarker);
            if (!(object instanceof StringBuffer)) {
                throw new IllegalArgumentException(
                        "Invalid or missing buffer for marker: " + marker);
            }

            buffer = (StringBuffer) object;
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
            attributes = new HashMap();
        }

        attributes.put(var, value);
    }

    /**
     * Appends a piece of SQL to the internal buffer.
     */
    public EJBQLTranslationContext append(String chunk) {
        currentBuffer.append(chunk);
        return this;
    }

    /**
     * Appends a piece of SQL to the internal buffer.
     */
    public EJBQLTranslationContext append(char chunk) {
        currentBuffer.append(chunk);
        return this;
    }

    /**
     * Deletes a specified number of characters from the end of the current buffer.
     */
    EJBQLTranslationContext trim(int n) {
        int len = currentBuffer.length();

        if (len >= n) {
            currentBuffer.delete(len - n, len);
        }
        return this;
    }

    EJBQLCompiledExpression getCompiledExpression() {
        return compiledExpression;
    }

    String bindPositionalParameter(int position) {
        return bindParameter(parameters.get(new Integer(position)));
    }

    String bindNamedParameter(String name) {
        return bindParameter(parameters.get(name));
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
            boundParameters = new HashMap();
        }

        String var = prefix + boundParameters.size();
        boundParameters.put(var, value);
        return var;
    }

    Object getBoundParameter(String name) {
        return boundParameters != null ? boundParameters.get(name) : null;
    }

    /**
     * Registers a "reusable" join, returning a preexisting ID if the join is already
     * registered. Reusable joins are the implicit inner joins that are added as a result
     * of processing of path expressions in SELECT or WHERE clauses. Note that if an
     * implicit INNER join overlaps with an explicit INNER join, both joins are added to
     * the query.
     */
    String registerReusableJoin(String sourceIdPath, String relationship, String targetId) {
        if (reusableJoins == null) {
            reusableJoins = new HashMap();
        }

        String key = sourceIdPath + ":" + relationship;

        String oldId = (String) reusableJoins.put(key, targetId);
        if (oldId != null) {
            // revert back to old id
            reusableJoins.put(key, oldId);
            return oldId;
        }

        return null;
    }

    /**
     * Retrieves a SQL alias for the combination of EJBQL id variable and a table name. If
     * such alias hasn't been used, it is created on the fly.
     */
    String getTableAlias(String idPath, String tableName) {

        StringBuffer keyBuffer = new StringBuffer();

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
            alias = (String) tableAliases.get(key);
        }
        else {
            tableAliases = new HashMap();
            alias = null;
        }

        if (alias == null) {
            alias = "t" + tableAliases.size();
            tableAliases.put(key, alias);
        }

        return alias;
    }

    /**
     * Returns a positional column alias, incrementing position index on each call.
     */
    String nextColumnAlias() {

        SQLResultSetMapping resultSetMapping = compiledExpression.getResultSetMapping();
        if (resultSetMapping == null) {
            throw new EJBQLException(
                    "No result set mapping exists for expression, can't map column aliases");
        }

        return (String) resultSetMapping.getColumnResults().get(columnAliasPosition++);
    }

    boolean isAppendingResultColumns() {
        return appendingResultColumns;
    }

    void setAppendingResultColumns(boolean appendingResultColumns) {
        this.appendingResultColumns = appendingResultColumns;
    }
}
