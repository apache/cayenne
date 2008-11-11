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

import javax.persistence.Query;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.EJBQLQuery;

/**
 * A JPA wrapper for {@link EJBQLQuery}.
 * 
 * @since 3.0
 */
public class JpaEJBQLQuery extends JpaQuery {

    protected EJBQLQuery query;

    public JpaEJBQLQuery(ObjectContext ctxt, String ejbqlString) {
        super(ctxt);
        query = new EJBQLQuery(ejbqlString);
    }

    @Override
    protected org.apache.cayenne.query.Query getQuery() {
        return query;
    }

    @Override
    public Query setParameter(int position, Object value) {
        query.setParameter(position, value);
        return this;
    }

    @Override
    public Query setParameter(String name, Object value) {
        query.setParameter(name, value);
        return this;
    }
}
