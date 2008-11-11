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


package org.apache.cayenne.jpa.entity;

import javax.persistence.ColumnResult;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

/**
 * Annotations shared by the entore map.
 * 
 */
@SequenceGenerator(name = "sg-name", sequenceName = "seq-name", initialValue = 5, allocationSize = 10)
@NamedQueries( {
        @NamedQuery(name = "query1", query = "select x", hints = {
                @QueryHint(name = "hint1", value = "value1"),
                @QueryHint(name = "hint2", value = "value2")
        }), @NamedQuery(name = "query2", query = "select y")
})
@NamedNativeQueries( {
        @NamedNativeQuery(name = "query3", query = "select z", resultClass = MockResultClass.class, resultSetMapping = "rs-mapping1", hints = {
                @QueryHint(name = "hint3", value = "value3"),
                @QueryHint(name = "hint4", value = "value4")
        }), @NamedNativeQuery(name = "query4", query = "select a")
})
@SqlResultSetMapping(name = "result-map1", entities = {
        @EntityResult(entityClass = MockEntityX.class, discriminatorColumn = "column1", fields = {
                @FieldResult(name = "field1", column = "column1"),
                @FieldResult(name = "field2", column = "column2")
        }),
        @EntityResult(entityClass = MockEntityY.class, discriminatorColumn = "column2", fields = {
                @FieldResult(name = "field3", column = "column3"),
                @FieldResult(name = "field4", column = "column4")
        })
}, columns = {
        @ColumnResult(name = "column-result1"), @ColumnResult(name = "column-result2")
})
@TableGenerator(name = "table-generator", table = "auto_pk_table", catalog = "catalog1", schema = "schema1", pkColumnName = "next_id", valueColumnName = "x", pkColumnValue = "y", initialValue = 4, allocationSize = 20, uniqueConstraints = {
    @UniqueConstraint(columnNames = {
        "pk1"
    })
})
public class MockEntityMap1 {

}
