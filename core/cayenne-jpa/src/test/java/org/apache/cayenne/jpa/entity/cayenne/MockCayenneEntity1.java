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


package org.apache.cayenne.jpa.entity.cayenne;

import java.util.Collection;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.cayenne.jpa.entity.MockTargetEntity2;
import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.ObjectId;

@Entity
@Table(name = "mock_persistent_1", catalog = "catalog1", schema = "schema1")
@NamedQueries( {
        @NamedQuery(name = "entityQuery1", query = "select x", hints = {
                @QueryHint(name = "hint1", value = "value1"),
                @QueryHint(name = "hint2", value = "value2")
        }), @NamedQuery(name = "entityQuery2", query = "select y")
})
@IdClass(ObjectId.class)
public class MockCayenneEntity1 extends CayenneDataObject {

    @Id
    protected int id;

    protected String attribute1;

    @Version
    protected int attribute2;

    @OneToOne(targetEntity = MockCayenneTargetEntity1.class, fetch = FetchType.LAZY, optional = true, mappedBy = "mb1", cascade = {
            CascadeType.MERGE, CascadeType.PERSIST
    })
    protected int attribute3;

    @OneToMany(targetEntity = MockCayenneTargetEntity2.class, fetch = FetchType.LAZY, mappedBy = "entity1", cascade = {
            CascadeType.MERGE, CascadeType.PERSIST
    })
    protected Collection<MockTargetEntity2> attribute4;

    @ManyToOne(targetEntity = MockCayenneTargetEntity2.class, fetch = FetchType.LAZY, optional = true, cascade = {
            CascadeType.MERGE, CascadeType.PERSIST
    })
    protected int attribute5;

    @ManyToMany(targetEntity = MockCayenneTargetEntity1.class, fetch = FetchType.LAZY, mappedBy = "mb4", cascade = {
            CascadeType.MERGE, CascadeType.PERSIST
    })
    protected int attribute6;

    @Transient
    protected int attribute8;

    @Column(name = "column9")
    @Temporal(TemporalType.DATE)
    protected Date attribute9;

    public String getAttribute1() {
        return attribute1;
    }

    public void setAttribute1(String attribute1) {
        this.attribute1 = attribute1;
    }

    public int getAttribute2() {
        return attribute2;
    }

    public void setAttribute2(int attribute2) {
        this.attribute2 = attribute2;
    }

    public int getAttribute3() {
        return attribute3;
    }

    public void setAttribute3(int attribute3) {
        this.attribute3 = attribute3;
    }
}
