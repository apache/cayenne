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
package org.apache.cayenne.access;

import org.apache.cayenne.access.jdbc.BatchAction;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;

/**
 * Class for creating DataDomain-specific actions
 */
class DataDomainActionBuilder implements SQLActionVisitor {
    DataDomain domain;
    
    SQLActionVisitor delegate;
    
    public DataDomainActionBuilder(DataDomain domain, SQLActionVisitor delegate) {
        this.domain = domain;
        this.delegate = delegate;
    }
    
    public DataDomain getDomain() {
        return domain;
    }

    public SQLAction batchAction(BatchQuery query) {
        SQLAction action = delegate.batchAction(query);
        if (action instanceof BatchAction) {
            ((BatchAction) action).setQueryBuilderFactory(domain.getQueryBuilderFactory());
        }
        return action;
    }

    public SQLAction ejbqlAction(EJBQLQuery query) {
        return delegate.ejbqlAction(query);
    }

    public SQLAction objectSelectAction(SelectQuery query) {
        return delegate.objectSelectAction(query);
    }

    public SQLAction procedureAction(ProcedureQuery query) {
        return delegate.procedureAction(query);
    }

    public SQLAction sqlAction(SQLTemplate query) {
        return delegate.sqlAction(query);
    }

    public SQLAction updateAction(Query query) {
        return delegate.updateAction(query);
    }
}
