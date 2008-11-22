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
package org.apache.cayenne.jpa.itest.ch3.entity;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

public class EntityListener1 {

    @PrePersist
    public void prePersistMethod(Object entity) {
        EntityListenerState.addPrePersistListener(this);
    }

    @PostPersist
    public void postPersistMethod(Object entity) {
        EntityListenerState.addPostPersistListener(this);
    }

    @PreRemove
    public void preRemoveMethod(Object entity) {
        EntityListenerState.addPreRemoveListener(this);
    }

    @PostRemove
    public void postRemoveMethod(Object entity) {
        EntityListenerState.addPostRemoveListener(this);
    }

    @PreUpdate
    public void preUpdateMethod(Object entity) {
        EntityListenerState.addPreUpdateListener(this);
    }

    @PostUpdate
    public void postUpdateMethod(Object entity) {
        EntityListenerState.addPostUpdateListener(this);
    }

    @PostLoad
    public void postLoadMethod(Object entity) {
        EntityListenerState.addPostLoadListener(this);
    }
}
