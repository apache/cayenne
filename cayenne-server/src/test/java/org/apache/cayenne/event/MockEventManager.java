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
package org.apache.cayenne.event;

import java.util.EventObject;
import java.util.List;

import org.apache.cayenne.event.DefaultEventManager.Dispatch;


public class MockEventManager implements EventManager {

    public void addListener(
            Object listener,
            String methodName,
            Class<?> eventParameterClass,
            EventSubject subject) {
    }

    public void addListener(
            Object listener,
            String methodName,
            Class<?> eventParameterClass,
            EventSubject subject,
            Object sender) {
    }

    public void addNonBlockingListener(
            Object listener,
            String methodName,
            Class<?> eventParameterClass,
            EventSubject subject) {
    }

    public void addNonBlockingListener(
            Object listener,
            String methodName,
            Class<?> eventParameterClass,
            EventSubject subject,
            Object sender) {
    }

    public List<Dispatch> getEventQueue() {
        return null;
    }

    public boolean isSingleThreaded() {
        return false;
    }

    public void postEvent(EventObject event, EventSubject subject) {
    }

    public void postNonBlockingEvent(EventObject event, EventSubject subject) {
    }

    public boolean removeAllListeners(EventSubject subject) {
        return false;
    }

    public boolean removeListener(Object listener) {
        return false;
    }

    public boolean removeListener(Object listener, EventSubject subject) {
        return false;
    }

    public boolean removeListener(Object listener, EventSubject subject, Object sender) {
        return false;
    }

    public void shutdown() {
    }

}
