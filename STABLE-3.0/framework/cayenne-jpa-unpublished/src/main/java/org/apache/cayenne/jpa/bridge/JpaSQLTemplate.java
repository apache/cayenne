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


package org.apache.cayenne.jpa.bridge;

import java.util.Collections;

import org.apache.cayenne.jpa.map.JpaQueryHint;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;

/**
 * An indirect query that resolves to Cayenne SQLTemplate using information from JPA query
 * hints.
 * 
 */
public class JpaSQLTemplate extends JpaIndirectQuery {

    /**
     * Creates a SQLTemplate using query hints.
     */
    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {
        SQLTemplate query = new SQLTemplate();
        query.setDefaultTemplate(jpaQuery.getQuery());

        if (parentEntity != null) {
            query.setRoot(parentEntity);
        }
        else if (parentMap != null) {
            query.setRoot(parentMap);
        }
        else {
            throw new CayenneRuntimeException("Unknown query root. Name: " + getName());
        }

        // metadata hints
        JpaQueryHint dataRowsHint = jpaQuery.getHint(QueryHints.DATA_ROWS_HINT);
        if (dataRowsHint != null) {
            query.setFetchingDataRows("true".equalsIgnoreCase(dataRowsHint.getValue()));
        }

        return query.queryWithParameters(parameters != null
                ? parameters
                : Collections.EMPTY_MAP);
    }

}
