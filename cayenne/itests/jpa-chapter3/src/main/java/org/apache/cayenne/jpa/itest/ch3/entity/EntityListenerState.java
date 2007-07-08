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

public class EntityListenerState {

    public static String prePersistCalled;
    public static String postPersistCalled;
    public static String preRemoveCalled;
    public static String postRemoveCalled;
    public static String preUpdateCalled;
    public static String postUpdateCalled;
    public static String postLoadCalled;

    public static void reset() {
        prePersistCalled = "";
        postPersistCalled = "";
        preRemoveCalled = "";
        postRemoveCalled = "";
        preUpdateCalled = "";
        postUpdateCalled = "";
        postLoadCalled = "";
    }

    public static String getPostLoadCalled() {
        return postLoadCalled;
    }

    public static String getPostPersistCalled() {
        return postPersistCalled;
    }

    public static String getPostRemoveCalled() {
        return postRemoveCalled;
    }

    public static String getPostUpdateCalled() {
        return postUpdateCalled;
    }

    public static String getPrePersistCalled() {
        return prePersistCalled;
    }

    public static String getPreRemoveCalled() {
        return preRemoveCalled;
    }

    public static String getPreUpdateCalled() {
        return preUpdateCalled;
    }

    public static void addPostLoadListener(Object listener) {
        EntityListenerState.postLoadCalled += ":" + listener.getClass().getName();
    }

    public static void addPrePersistListener(Object listener) {
        EntityListenerState.prePersistCalled += ":" + listener.getClass().getName();
    }

    public static void addPostPersistListener(Object listener) {
        EntityListenerState.postPersistCalled += ":" + listener.getClass().getName();
    }

    public static void addPreRemoveListener(Object listener) {
        EntityListenerState.preRemoveCalled += ":" + listener.getClass().getName();
    }

    public static void addPostRemoveListener(Object listener) {
        EntityListenerState.postRemoveCalled += ":" + listener.getClass().getName();
    }

    public static void addPreUpdateListener(Object listener) {
        EntityListenerState.preUpdateCalled += ":" + listener.getClass().getName();
    }

    public static void addPostUpdateListener(Object listener) {
        EntityListenerState.postUpdateCalled += ":" + listener.getClass().getName();
    }
}
