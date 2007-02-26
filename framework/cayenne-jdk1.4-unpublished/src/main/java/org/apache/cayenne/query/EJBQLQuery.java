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

import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.map.EntityResolver;

/**
 * An EJBQL query representation in Cayenne.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class EJBQLQuery implements Query {

    protected String ejbqlStatement;
    protected String name;

    private transient EJBQLExpression parsedRoot;

    public EJBQLQuery(String ejbqlStatement) {
        this.ejbqlStatement = ejbqlStatement;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        // TODO Auto-generated method stub
        return null;
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {
        // TODO Auto-generated method stub
        return null;
    }

    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        // TODO Auto-generated method stub
    }

    /**
     * Returns an unparsed EJB QL statement used to initialize this query.
     */
    public String getEjbqlStatement() {
        return ejbqlStatement;
    }

    /**
     * Returns lazily initialialized parsed root of this query.
     */
    Object getParsedRoot() throws EJBQLException {
        if (parsedRoot == null) {
            this.parsedRoot = EJBQLParserFactory.getParser().parse(ejbqlStatement);
        }

        return parsedRoot;
    }
}
