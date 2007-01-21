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

import javax.persistence.EntityListeners;
import javax.persistence.ExcludeDefaultListeners;
import javax.persistence.ExcludeSuperclassListeners;
import javax.persistence.IdClass;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

@MappedSuperclass
@IdClass(MockIdClass.class)
@ExcludeDefaultListeners
@ExcludeSuperclassListeners
@EntityListeners(value = {
    MockEntityListener1.class
})
public class MockMappedSuperclass1 {

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
