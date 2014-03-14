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

import org.apache.cayenne.access.trans.BatchQueryBuilder;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;

/**
 * Factory which creates BatchQueryBuilders for different types of queries,
 * which, in their turn, create SQL strings for batch queries.
 * 
 * @since 3.0
 */
public interface BatchQueryBuilderFactory {

    /**
     * Creates query builder for INSERT queries
     * 
     * @since 3.2
     */
    BatchQueryBuilder createInsertQueryBuilder(InsertBatchQuery query, DbAdapter adapter);

    /**
     * Creates query builder for UPDATE queries
     * 
     * @since 3.2
     */
    BatchQueryBuilder createUpdateQueryBuilder(UpdateBatchQuery query, DbAdapter adapter);

    /**
     * Creates query builder for DELETE queries
     * 
     * @since 3.2
     */
    BatchQueryBuilder createDeleteQueryBuilder(DeleteBatchQuery query, DbAdapter adapter);
}
