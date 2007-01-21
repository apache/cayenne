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

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;

@Entity(name = "MockEntity2")
@PrimaryKeyJoinColumns(value = {
        @PrimaryKeyJoinColumn(name = "pk_column1", referencedColumnName = "super_column1", columnDefinition = "count(1)"),
        @PrimaryKeyJoinColumn(name = "pk_column2")
})
@Inheritance(strategy = InheritanceType.JOINED)
@AttributeOverrides(value = {
        @AttributeOverride(name = "attribute1", column = @Column(name = "ao_column1", unique = true, nullable = true, insertable = true, updatable = true, columnDefinition = "count(1)", table = "ao_table1", length = 3, precision = 4, scale = 5)),
        @AttributeOverride(name = "attribute2", column = @Column(name = "ao_column2", unique = true, nullable = true, insertable = true, updatable = true, columnDefinition = "count(1)", table = "ao_table1", length = 3, precision = 4, scale = 5))
})
public class MockEntity2 {

}
