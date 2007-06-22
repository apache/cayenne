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
import org.apache.cayenne.query.SQLTemplate;

/**
 * A context used for translating of EJBQL to SQL.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLTranslationContext {

    static final String FROM_TAIL_MARKER = "FROM_TAIL_MARKER";

    private Map aliases;
    private Map boundParameters;
    private StringBuffer mainBuffer;
    private StringBuffer currentBuffer;
    private EJBQLCompiledExpression compiledExpression;
    private Map attributes;
    private Map reusableJoins;
    private Map parameters;

    EJBQLTranslationContext(EJBQLCompiledExpression compiledExpression, Map parameters) {
        this.compiledExpression = compiledExpression;
        this.mainBuffer = new StringBuffer();
        this.currentBuffer = mainBuffer;
        this.parameters = parameters;
    }

    SQLTemplate getQuery() {
        String sql = mainBuffer.length() > 0 ? mainBuffer.toString() : null;
        SQLTemplate query = new SQLTemplate(compiledExpression
                .getRootDescriptor()
                .getObjectClass(), sql);
        query.setParameters(boundParameters);
        return query;
    }

    /**
     * Inserts a marker in the SQL, mapped to a StringBuffer that can be later filled with
     * content.
     */
    void markCurrentPosition(String marker) {
        // make sure we mark the main buffer
        StringBuffer current = this.currentBuffer;

        try {
            switchToMainBuffer();
            String internalMarker = bindParameter(new StringBuffer(), "marker");
            append("${").append(internalMarker).append("}");

            // register mapping of internal to external marker
            setAttribute(marker, internalMarker);
        }
        finally {
            this.currentBuffer = current;
        }
    }

    void switchToMarker(String marker) {
        String internalMarker = (String) getAttribute(marker);
        if (internalMarker == null) {
            throw new IllegalArgumentException("Invalid marker: " + marker);
        }

        Object object = boundParameters.get(internalMarker);
        if (!(object instanceof StringBuffer)) {
            throw new IllegalArgumentException("Invalid or missing buffer for marker: "
                    + marker);
        }

        this.currentBuffer = (StringBuffer) object;
    }

    void switchToMainBuffer() {
        this.currentBuffer = this.mainBuffer;
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
    EJBQLTranslationContext append(String chunk) {
        currentBuffer.append(chunk);
        return this;
    }

    /**
     * Appends a piece of SQL to the internal buffer.
     */
    EJBQLTranslationContext append(char chunk) {
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
     * registered. Reusable normally means an inner join that can be duplicated implicitly
     * in the path expressions.
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
    String getAlias(String idPath, String tableName) {

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

        if (aliases != null) {
            alias = (String) aliases.get(key);
        }
        else {
            aliases = new HashMap();
            alias = null;
        }

        if (alias == null) {
            alias = "t" + aliases.size();
            aliases.put(key, alias);
        }

        return alias;
    }
}
