/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.testdo.entity_callbacks;

import org.apache.cayenne.lifecycle.PostAddAware;
import org.apache.cayenne.lifecycle.PostLoadAware;
import org.apache.cayenne.lifecycle.PostPersistAware;
import org.apache.cayenne.lifecycle.PostRemoveAware;
import org.apache.cayenne.lifecycle.PostUpdateAware;
import org.apache.cayenne.lifecycle.PrePersistAware;
import org.apache.cayenne.lifecycle.PreRemoveAware;
import org.apache.cayenne.lifecycle.PreUpdateAware;
import org.apache.cayenne.testdo.entity_callbacks.auto._CallbackArtist;

public class CallbackArtist extends _CallbackArtist implements
        PostAddAware,
        PrePersistAware,
        PostPersistAware,
        PreUpdateAware,
        PostUpdateAware,
        PreRemoveAware,
        PostRemoveAware,
        PostLoadAware {

    @Override
    public void onPostAdd() {
        postCallback = "onPostAdd";
    }

    @Override
    public void onPrePersist() {
        preCallback = "onPrePersist";
    }

    @Override
    public void onPostPersist() {
        postCallback = "onPostPersist";
    }

    @Override
    public void onPreUpdate() {
        preCallback = "onPreUpdate";
    }

    @Override
    public void onPostUpdate() {
        postCallback = "onPostUpdate";
    }

    @Override
    public void onPreRemove() {
        preCallback = "onPreRemove";
    }

    @Override
    public void onPostRemove() {
        postCallback = "onPostRemove";
    }

    @Override
    public void onPostLoad() {
        postCallback = "onPostLoad";
    }
}
