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

package org.apache.cayenne.map;

import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;

/**
 * A QueryBuilder for stored procedure-based queries.
 * 
 * @since 1.1
 */
class ProcedureQueryBuilder extends QueryLoader {

    /**
     * Returns a ProcedureQuery.
     */
    @Override
    public Query getQuery() {
        ProcedureQuery query = new ProcedureQuery();
        Object root = getRoot();

        if (root != null) {
            query.setRoot(root);
        }

        query.setName(name);
        query.setDataMap(dataMap);
        query.setResultEntityName(resultEntity);
        query.initWithProperties(properties);

        return query;
    }

}
