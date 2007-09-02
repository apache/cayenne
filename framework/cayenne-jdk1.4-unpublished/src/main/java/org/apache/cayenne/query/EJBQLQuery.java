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
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;

/**
 * An EJBQL query representation in Cayenne.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class EJBQLQuery implements Query {

    protected String name;
    protected String ejbqlStatement;
    protected Map parameters;

    protected transient EJBQLCompiledExpression expression;
    EJBQLQueryMetadata metadata = new EJBQLQueryMetadata();

    public EJBQLQuery(String ejbqlStatement) {
        this.ejbqlStatement = ejbqlStatement;
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {
        metadata.resolve(resolver, this);
        return metadata;
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
    public Map getParameters() {
        return parameters != null
                ? Collections.unmodifiableMap(parameters)
                : Collections.EMPTY_MAP;
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

        if (parameters == null) {
            parameters = new HashMap();
        }

        parameters.put(name, object);
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

        if (parameters == null) {
            parameters = new HashMap();
        }

        parameters.put(new Integer(position), object);
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
        this.metadata.setFetchLimit(fetchLimit);
    }
}
