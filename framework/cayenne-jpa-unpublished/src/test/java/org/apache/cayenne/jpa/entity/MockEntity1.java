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

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EntityResult;
import javax.persistence.ExcludeDefaultListeners;
import javax.persistence.ExcludeSuperclassListeners;
import javax.persistence.FieldResult;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.QueryHint;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

@IdClass(MockIdClass.class)
@TableGenerator(name = "table-generator", table = "auto_pk_table", catalog = "catalog1", schema = "schema1", pkColumnName = "next_id", valueColumnName = "x", pkColumnValue = "y", initialValue = 4, allocationSize = 20, uniqueConstraints = {
    @UniqueConstraint(columnNames = {
        "pk1"
    })
})
@Entity(name = "MockEntity1")
@Table(name = "mock_persistent_1", catalog = "catalog1", schema = "schema1", uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "column1", "column2"
        }), @UniqueConstraint(columnNames = {
            "column3"
        })
})
@SecondaryTables(value = {
        @SecondaryTable(name = "secondary1", catalog = "catalog1", schema = "schema1", pkJoinColumns = {
                @PrimaryKeyJoinColumn(name = "secondary_column1", referencedColumnName = "column1", columnDefinition = "count(1)"),
                @PrimaryKeyJoinColumn(name = "secondary_column2", referencedColumnName = "column2")
        }, uniqueConstraints = {
            @UniqueConstraint(columnNames = {
                    "column1", "column2"
            })
        }), @SecondaryTable(name = "secondary2")
})
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
@ExcludeDefaultListeners
@ExcludeSuperclassListeners
@EntityListeners(value = {
        MockEntityListener1.class, MockEntityListener2.class
})
public class MockEntity1 {

    @Id
    @Temporal(TemporalType.TIME)
    @Column(name = "id_column", unique = true, nullable = true, insertable = true, updatable = true, table = "id_table", length = 3, precision = 4, scale = 5)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id-generator")
    protected int id1;

    @PrePersist
    protected void eprePersist() {

    }

    @PostPersist
    protected void epostPersist() {

    }

    @PreRemove
    protected void epreRemove() {

    }

    @PostRemove
    protected void epostRemove() {

    }

    @PreUpdate
    protected void epreUpdate() {

    }

    @PostUpdate
    protected void epostUpdate() {

    }

    @PostLoad
    protected void epostLoad() {

    }
}
