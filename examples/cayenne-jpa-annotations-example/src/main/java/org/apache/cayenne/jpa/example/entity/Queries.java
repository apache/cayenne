/*
 *  Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.cayenne.jpa.example.entity;

import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;

import org.apache.cayenne.jpa.bridge.QueryHints;

@NamedQueries(value = {
        @NamedQuery(name = "SchemaCheck", query = "select count(1) from department", hints = {
                @QueryHint(name = QueryHints.QUERY_TYPE_HINT, value = QueryHints.SQL_TEMPLATE_QUERY),
                @QueryHint(name = QueryHints.DATA_ROWS_HINT, value = "true")
        }),
        @NamedQuery(name = "CreateData", query = "insert into department (department_id, name, description) "
                + "values (1, 'IT', 'Information Technology Department')", hints = {
            @QueryHint(name = QueryHints.QUERY_TYPE_HINT, value = QueryHints.SQL_TEMPLATE_QUERY)
        }),
        @NamedQuery(name = "DeletePerson", query = "delete from person", hints = {
            @QueryHint(name = QueryHints.QUERY_TYPE_HINT, value = QueryHints.SQL_TEMPLATE_QUERY)
        }),
        @NamedQuery(name = "DeleteDepartment", query = "delete from department", hints = {
            @QueryHint(name = QueryHints.QUERY_TYPE_HINT, value = QueryHints.SQL_TEMPLATE_QUERY)
        })
})
public class Queries {

}
