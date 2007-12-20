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
package org.apache.cayenne.jpa;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TransactionRequiredException;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ParameterizedQuery;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;

/**
 * A JPA Query that wraps a Cayenne Query.
 */
public class JpaQuery implements Query {

    protected Map<String, Object> parameters = new HashMap<String, Object>();
    protected org.apache.cayenne.query.Query cayenneQuery;
    protected ObjectContext context;

    public JpaQuery(ObjectContext ctxt) {
        this.context = ctxt;
    }

    /**
     * Construct a named query.
     */
    public JpaQuery(ObjectContext context, String name) {
        this(context);

        org.apache.cayenne.query.Query q = context.getEntityResolver().lookupQuery(name);

        if (q == null) {
            throw new IllegalArgumentException("Non-existing query: " + name);
        }

        setQuery(q);
    }

    protected void setQuery(org.apache.cayenne.query.Query q) {
        this.cayenneQuery = q;
    }

    protected org.apache.cayenne.query.Query getQuery() {
        return cayenneQuery;
    }

    /**
     * Return the same query with parameters set.
     */
    private org.apache.cayenne.query.Query queryWithParameters() {
        if (parameters.size() == 0) {
            return cayenneQuery;
        }

        return ((ParameterizedQuery) cayenneQuery).createQuery(parameters);
    }

    /**
     * Execute a SELECT query and return the query results as a List.
     * 
     * @return a list of the results
     * @throws IllegalStateException if called for an EJB QL UPDATE or DELETE statement
     */
    @SuppressWarnings("unchecked")
    public List getResultList() {
        return context.performQuery(queryWithParameters());
    }

    /**
     * Execute an update or delete statement.
     * 
     * @return the number of entities updated or deleted
     * @throws IllegalStateException if called for an EJB QL SELECT statement
     * @throws TransactionRequiredException if there is no transaction
     */
    public int executeUpdate() {
        // TODO: check transaction

        QueryResponse response = context.performGenericQuery(queryWithParameters());
        int[] res = response.firstUpdateCount();

        if (res == null) {
            return -1;
        }

        int num = 0;
        for (int i = 0; i < res.length; i++) {
            num = num + res[i];
        }
        return num;
    }

    /**
     * Execute a SELECT query that returns a single result.
     * 
     * @return the result
     * @throws NoResultException if there is no result
     * @throws NonUniqueResultException if more than one result
     * @throws IllegalStateException if called for an EJB QL UPDATE or DELETE statement
     */
    public Object getSingleResult() {
        List<?> rows = getResultList();
        if (rows.size() == 0) {
            throw new NoResultException();
        }
        if (rows.size() > 1) {
            throw new NonUniqueResultException();
        }

        return rows.get(0);
    }

    /**
     * Set the maximum number of results to retrieve.
     * 
     * @param maxResult
     * @return the same query instance
     * @throws IllegalArgumentException if argument is negative
     */
    public Query setMaxResults(int maxResult) {
        if (maxResult < 0) {
            throw new IllegalArgumentException("Invalid max results value: " + maxResult);
        }

        Object query = getQuery();

        // the first two types are probably the only queries anyone would run via JPA
        if (query instanceof EJBQLQuery) {
            ((EJBQLQuery) query).setFetchLimit(maxResult);
        }
        else if (query instanceof SQLTemplate) {
            ((SQLTemplate) query).setFetchLimit(maxResult);
        }
        else if (query instanceof SelectQuery) {
            ((SelectQuery) query).setFetchLimit(maxResult);
        }
        else if (query instanceof ProcedureQuery) {
            ((ProcedureQuery) query).setFetchLimit(maxResult);
        }
        else {
            throw new IllegalArgumentException("query does not support maxResult: "
                    + query);
        }
        
        return this;
    }

    public Query setFlushMode(FlushModeType flushModeType) {
        return this;
    }

    /**
     * Set an implementation-specific hint. If the hint name is not recognized, it is
     * silently ignored.
     * 
     * @param hintName
     * @param value
     * @return the same query instance
     * @throws IllegalArgumentException if the second argument is not valid for the
     *             implementation
     */
    public Query setHint(String hintName, Object value) {
        return this;
    }

    /**
     * Set the position of the first result to retrieve.
     * 
     * @param startPosition position of the first result, numbered from 0
     * @return the same query instance
     * @throws IllegalArgumentException if argument is negative
     */
    public Query setFirstResult(int startPosition) {
        if (startPosition < 0) {
            throw new IllegalArgumentException("Invalid first result value: "
                    + startPosition);
        }
        // TODO: support in core like fetchLimit?
        // TODO: hack a temp solution here based on sub-list?
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Bind an argument to a named parameter.
     * 
     * @param name the parameter name
     * @param value
     * @return the same query instance
     * @throws IllegalArgumentException if parameter name does not correspond to parameter
     *             in query string or argument is of incorrect type
     */
    public Query setParameter(String name, Object value) {
        if (!(cayenneQuery instanceof ParameterizedQuery)) {
            throw new IllegalArgumentException("query does not accept parameters");
        }

        // TODO: check for valid parameter. should probably be built in to
        // all ParameterizedQuerys

        parameters.put(name, value);
        return this;
    }

    /**
     * Bind an instance of java.util.Date to a named parameter.
     * 
     * @param name
     * @param value
     * @param temporalType
     * @return the same query instance
     * @throws IllegalArgumentException if parameter name does not correspond to parameter
     *             in query string
     */
    public Query setParameter(String name, Date value, TemporalType temporalType) {
        // handled by cayenne.
        return setParameter(name, value);
    }

    /**
     * Bind an instance of java.util.Calendar to a named parameter.
     * 
     * @param name
     * @param value
     * @param temporalType
     * @return the same query instance
     * @throws IllegalArgumentException if parameter name does not correspond to parameter
     *             in query string
     */
    public Query setParameter(String name, Calendar value, TemporalType temporalType) {
        // handled by cayenne.
        return setParameter(name, value);
    }

    /**
     * Bind an argument to a positional parameter.
     * 
     * @param position
     * @param value
     * @return the same query instance
     * @throws IllegalArgumentException if position does not correspond to positional
     *             parameter of query or argument is of incorrect type
     */
    public Query setParameter(int position, Object value) {
        // TODO: implement
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Bind an instance of java.util.Date to a positional parameter.
     * 
     * @param position
     * @param value
     * @param temporalType
     * @return the same query instance
     * @throws IllegalArgumentException if position does not correspond to positional
     *             parameter of query
     */
    public Query setParameter(int position, Date value, TemporalType temporalType) {
        // handled by cayenne.
        return setParameter(position, value);
    }

    /**
     * Bind an instance of java.util.Calendar to a positional parameter.
     * 
     * @param position
     * @param value
     * @param temporalType
     * @return the same query instance
     * @throws IllegalArgumentException if position does not correspond to positional
     *             parameter of query
     */
    public Query setParameter(int position, Calendar value, TemporalType temporalType) {
        // handled by cayenne.
        return setParameter(position, value);
    }

}
