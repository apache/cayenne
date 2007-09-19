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
package org.apache.cayenne.jpa.itest.ch2.entity;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class CollectionFieldPersistenceEntity {

    @Id
    protected int id;

    @OneToMany(mappedBy="entity")
    protected Collection<HelperEntity1> collection;

    @OneToMany(mappedBy="entity")
    protected Set<HelperEntity2> set;

    @OneToMany(mappedBy="entity")
    protected List<HelperEntity3> list;

    public Collection<HelperEntity1> getCollection() {
        return collection;
    }

    public void setCollection(Collection<HelperEntity1> collection) {
        this.collection = collection;
    }

    public List<HelperEntity3> getList() {
        return list;
    }

    public void setList(List<HelperEntity3> list) {
        this.list = list;
    }

    public Set<HelperEntity2> getSet() {
        return set;
    }

    public void setSet(Set<HelperEntity2> set) {
        this.set = set;
    }
}
