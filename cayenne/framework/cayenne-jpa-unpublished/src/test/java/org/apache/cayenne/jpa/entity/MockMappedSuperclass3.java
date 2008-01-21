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

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@MappedSuperclass
public class MockMappedSuperclass3 {

    @Basic
    protected String attribute1;

    @Version
    protected int attribute2;

    @OneToOne(targetEntity = MockTargetEntity1.class, fetch = FetchType.LAZY, optional = true, mappedBy = "mb1", cascade = {
            CascadeType.REMOVE, CascadeType.REFRESH
    })
    protected int attribute3;

    @OneToMany(targetEntity = MockTargetEntity2.class, fetch = FetchType.LAZY, mappedBy = "mb2", cascade = {
            CascadeType.PERSIST, CascadeType.MERGE
    })
    protected int attribute4;

    @ManyToOne(targetEntity = MockTargetEntity1.class, fetch = FetchType.LAZY, optional = true, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE
    })
    protected int attribute5;

    @ManyToMany(targetEntity = MockTargetEntity1.class, fetch = FetchType.LAZY, mappedBy = "mb4", cascade = {
            CascadeType.PERSIST, CascadeType.MERGE
    })
    protected int attribute6;

    @Embedded
    protected int attribute7;

    @Transient
    protected int attribute8;

    @Column(name = "column9")
    protected int attribute9;

    @OneToMany
    @JoinColumn(name = "join-column-10", referencedColumnName = "x-ref", unique = true, nullable = true, insertable = true, updatable = true, columnDefinition = "x-def", table = "jt1")
    protected int attribute10;

    @JoinTable(name = "jtable1", catalog = "catalog1", schema = "schema1", joinColumns = {
            @JoinColumn(name = "join-column1"), @JoinColumn(name = "join-column2")
    }, inverseJoinColumns = {
            @JoinColumn(name = "ijoin-column1"), @JoinColumn(name = "ijoin-column2")
    }, uniqueConstraints = {
        @UniqueConstraint(columnNames = {
            "pk1"
        })
    })
    @OneToMany
    protected int attribute11;

    @Lob
    protected int attribute12;

    @Temporal(TemporalType.DATE)
    protected int attribute13;

    @Enumerated(value = EnumType.ORDINAL)
    protected int attribute14;

    @ManyToMany
    @MapKey(name = "mk")
    protected int attribute15;

    @OrderBy(value = "x ASC")
    @ManyToMany
    protected int attribute16;
}
