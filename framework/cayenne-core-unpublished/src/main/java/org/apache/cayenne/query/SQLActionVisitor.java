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

/**
 * A factory interface to create standard SQLActions for a set of standard queries.
 * Instances of SQLActionVisitor are passed by Cayenne to a Query in
 * {@link org.apache.cayenne.query.Query#createSQLAction(SQLActionVisitor)}, allowing
 * query to choose the action type and convert itself to a "standard" query if needed.
 * Individual DbAdapters would provide special visitors, thus allowing for DB-dependent
 * execution algorithms.
 * 
 * @see org.apache.cayenne.query.Query#createSQLAction(SQLActionVisitor)
 * @since 1.2
 */
public interface SQLActionVisitor {

    /**
     * Creates an action to execute a batch update query.
     */
    SQLAction batchAction(BatchQuery query);

    /**
     * Creates an action to execute a SelectQuery.
     */
    <T> SQLAction objectSelectAction(SelectQuery<T> query);

    /**
     * Creates an action to execute a SQLTemplate.
     */
    SQLAction sqlAction(SQLTemplate query);

    /**
     * Creates an action to execute a ProcedureQuery.
     */
    SQLAction procedureAction(ProcedureQuery query);

    /**
     * Creates an action to execute EJBQL query.
     */
    SQLAction ejbqlAction(EJBQLQuery query);
}
