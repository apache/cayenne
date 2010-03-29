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

/**
 * Defines QueryHints recognized by Cayenne provider.
 * 
 */
public interface QueryHints {

    public static final String QUERY_TYPE_HINT = "cayenne.query.type";

    // must use strings instead of Class.getName() as otherwise Eclipse complains when the
    // constant is used in annotations. Is this an Eclipse bug?

    public static final String SELECT_QUERY = "org.apache.cayenne.jpa.bridge.JpaSelectQuery";
    public static final String PROCEDURE_QUERY = "org.apache.cayenne.jpa.bridge.JpaProcedureQuery";
    public static final String SQL_TEMPLATE_QUERY = "org.apache.cayenne.jpa.bridge.JpaSQLTemplate";

    public static final String QUALIFIER_HINT = "cayenne.query.qualifier";
    public static final String DATA_ROWS_HINT = "cayenne.query.fetchesDataRows";
}
